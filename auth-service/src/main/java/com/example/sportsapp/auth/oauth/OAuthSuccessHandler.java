package com.example.sportsapp.auth.oauth;

import com.example.sportsapp.auth.entitiy.User;
import com.example.sportsapp.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuthSuccessHandler.class);

    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Value("${app.oauth.frontend-redirect-base-url:http://localhost:3000}")
    private String frontendRedirectBaseUrl;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsProperty;

    private List<String> trustedOrigins = new ArrayList<>();

    public OAuthSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void validateConfiguration() {
        // Parse and validate the configured redirect base URL
        validateRedirectUrl(frontendRedirectBaseUrl);

        // Build allowlist of trusted origins from CORS configuration
        trustedOrigins = parseAndValidateOrigins(allowedOriginsProperty);
        if (trustedOrigins.isEmpty()) {
            throw new IllegalArgumentException(
                "OAuth redirect configuration error: no valid trusted origins configured. " +
                "Set app.cors.allowed-origins to a comma-separated list of trusted frontend origins. " +
                "Example: http://localhost:3000 or https://app.example.com");
        }

        // Verify that the redirect base URL matches a trusted origin
        String redirectOrigin = extractOrigin(frontendRedirectBaseUrl);
        if (!trustedOrigins.contains(redirectOrigin)) {
            throw new IllegalArgumentException(
                String.format(
                    "OAuth redirect configuration error: configured frontend redirect URL (%s) does not match any trusted CORS origin. " +
                    "Trusted origins: %s. " +
                    "Configure app.oauth.frontend-redirect-base-url to use one of the app.cors.allowed-origins.",
                    frontendRedirectBaseUrl, trustedOrigins));
        }

        logger.info("OAuth2 redirect configuration validated against {} trusted origins", trustedOrigins.size());
    }

    /**
     * Validate a redirect URL: must be absolute HTTP(S), with valid host, no path/query/fragment.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if URL is invalid
     */
    private void validateRedirectUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException(
                "OAuth redirect configuration error: frontend redirect base URL is empty. " +
                "Configure app.oauth.frontend-redirect-base-url to an absolute HTTP(S) URL.");
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();
            String query = uri.getQuery();
            String fragment = uri.getFragment();

            // Validate scheme
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException(
                    String.format(
                        "OAuth redirect configuration error: scheme '%s' is not permitted. " +
                        "Only 'http' and 'https' are allowed. Configured URL: %s",
                        scheme, url));
            }

            // Validate host is present and not a wildcard
            if (host == null || host.isEmpty() || "*".equals(host)) {
                throw new IllegalArgumentException(
                    String.format(
                        "OAuth redirect configuration error: host is missing, empty, or is a wildcard. " +
                        "Configured URL: %s", url));
            }

            // Reject embedded credentials
            if (uri.getUserInfo() != null) {
                throw new IllegalArgumentException(
                    String.format(
                        "OAuth redirect configuration error: credentials embedded in URL. " +
                        "Remove username:password@ from configured URL: %s", url));
            }

            // Reject non-empty paths (except root /)
            if (path != null && !path.isEmpty() && !path.equals("/")) {
                throw new IllegalArgumentException(
                    String.format(
                        "OAuth redirect configuration error: URL contains a path (%s). " +
                        "Only scheme, host, and optional port are allowed. Configured URL: %s",
                        path, url));
            }

            // Reject query strings
            if (query != null && !query.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format(
                        "OAuth redirect configuration error: URL contains a query string. " +
                        "Configured URL: %s", url));
            }

            // Reject fragments
            if (fragment != null && !fragment.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format(
                        "OAuth redirect configuration error: URL contains a fragment. " +
                        "Configured URL: %s", url));
            }

        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException(
                String.format(
                    "OAuth redirect configuration error: malformed URL. %s. Configured URL: %s",
                    e.getMessage(), url), e);
        }
    }

    /**
     * Extract origin (scheme + host + optional port) from a URL string.
     *
     * @param url the URL
     * @return origin string (e.g., "http://localhost:3000" or "https://app.example.com")
     */
    private String extractOrigin(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();

            if (port == -1) {
                return String.format("%s://%s", scheme, host);
            } else {
                return String.format("%s://%s:%d", scheme, host, port);
            }
        } catch (java.net.URISyntaxException e) {
            // Should not happen if validateRedirectUrl passed
            logger.warn("Failed to extract origin from URL: {}", url);
            return url;
        }
    }

    /**
     * Parse and validate comma-separated CORS origins from property.
     * Validation rules:
     * - Scheme must be http or https
     * - Host must be non-null and non-empty
     * - No embedded credentials, paths (except /), query strings, or fragments
     * - Origins are trimmed of whitespace
     *
     * @param originsProperty comma-separated origins
     * @return List of validated origins; empty list if none valid
     */
    private List<String> parseAndValidateOrigins(String originsProperty) {
        List<String> validOrigins = new ArrayList<>();

        if (originsProperty == null || originsProperty.isBlank()) {
            logger.warn("CORS allowed-origins property is null or empty");
            return validOrigins;
        }

        String[] origins = originsProperty.split(",");
        for (String origin : origins) {
            String trimmed = origin.trim();

            if (trimmed.isEmpty()) {
                logger.warn("Skipping empty CORS origin");
                continue;
            }

            try {
                URI uri = new URI(trimmed);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                String path = uri.getPath();
                String query = uri.getQuery();
                String fragment = uri.getFragment();

                // Validate scheme
                if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                    logger.warn("Rejecting CORS origin with invalid scheme '{}': {}", scheme, trimmed);
                    continue;
                }

                // Validate host
                if (host == null || host.isEmpty() || "*".equals(host)) {
                    logger.warn("Rejecting CORS origin with invalid host: {}", trimmed);
                    continue;
                }

                // Reject embedded credentials
                if (uri.getUserInfo() != null) {
                    logger.warn("Rejecting CORS origin with embedded credentials: {}", trimmed);
                    continue;
                }

                // Reject paths (except empty or /)
                if (path != null && !path.isEmpty() && !path.equals("/")) {
                    logger.warn("Rejecting CORS origin with path: {}", trimmed);
                    continue;
                }

                // Reject query strings
                if (query != null && !query.isEmpty()) {
                    logger.warn("Rejecting CORS origin with query string: {}", trimmed);
                    continue;
                }

                // Reject fragments
                if (fragment != null && !fragment.isEmpty()) {
                    logger.warn("Rejecting CORS origin with fragment: {}", trimmed);
                    continue;
                }

                // Valid origin
                validOrigins.add(trimmed);
                logger.debug("Accepted CORS origin: {}", trimmed);

            } catch (java.net.URISyntaxException e) {
                logger.warn("Rejecting malformed CORS origin '{}': {}", trimmed, e.getMessage());
            }
        }

        return validOrigins;
    }

    /**
     * Build the secure redirect URL for post-login frontend destination.
     *
     * Uses UriComponentsBuilder for proper RFC 3986 URL encoding.
     * Redirects to /dashboard with token and email as query parameters.
     * The base URL must be validated as a trusted origin at startup.
     *
     * Security notes:
     * - JWT passed in query parameter (known limitation; future refactor to HTTP-only cookie)
     * - Frontend extracts from URL and stores in localStorage
     * - URL cleaned from browser history via window.history.replaceState()
     * - Base URL validated against allowlist at startup (@PostConstruct validateConfiguration)
     * - Email is URL-encoded for safety
     * - No partial or user-controlled input influences the redirect destination
     *
     * @param token JWT token (will be included in URL; not logged)
     * @param email user email (URL-encoded for safety)
     * @return full redirect URL with encoded parameters
     */
    String buildSecureRedirectUrl(String token, String email) {
        return UriComponentsBuilder.fromUriString(frontendRedirectBaseUrl)
                .path("/dashboard")
                .queryParam("token", token)
                .queryParam("email", email)
                .toUriString();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String email = oAuth2User.getAttribute("email");

            if (email == null || email.isEmpty()) {
                logger.error("OAuth2 authentication: no email found in OAuth2User attributes");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to extract email from OAuth provider");
                return;
            }

            logger.info("OAuth2 user authenticated");

            // Check if user exists in the database, if not create a new user
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                logger.info("Creating new user from OAuth2 provider");
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setProvider("GOOGLE");
                return userRepository.save(newUser);
            });

            String token = generateToken(user);

            // Build redirect URL with proper encoding and validation
            String redirectUrl = buildSecureRedirectUrl(token, email);

            // Log only the destination host for debugging, never log the full URL or JWT
            try {
                URI uri = new URI(redirectUrl);
                logger.info("OAuth2 post-login redirect to host: {}", uri.getHost());
            } catch (java.net.URISyntaxException e) {
                logger.debug("Unable to parse redirect URI for logging");
            }

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            logger.error("OAuth2 authentication handler error: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth authentication failed");
        }
    }

    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }
}

