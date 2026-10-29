package com.example.sportsapp.auth.security;

import com.example.sportsapp.auth.oauth.OAuthSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final OAuthSuccessHandler oAuthSuccessHandler;
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsProperty;

    public SecurityConfig(OAuthSuccessHandler oAuthSuccessHandler,
                          AuthenticationProvider authenticationProvider,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ClientRegistrationRepository clientRegistrationRepository) {
        this.oAuthSuccessHandler = oAuthSuccessHandler;
        this.authenticationProvider = authenticationProvider;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @PostConstruct
    public void validateConfiguration() {
        List<String> origins = parseAndValidateOrigins(allowedOriginsProperty);
        if (origins.isEmpty()) {
            throw new IllegalArgumentException(
                "CORS configuration error: app.cors.allowed-origins is empty or contains only invalid values. " +
                "Ensure at least one valid HTTP/HTTPS origin is configured. " +
                "Example: http://localhost:3000 or https://app.example.com");
        }
        logger.info("CORS configuration validated: {} allowed origins", origins.size());
    }

    /**
     * Parse and validate comma-separated CORS origins from property.
     * Validation rules:
     * - Scheme must be http or https (no file://, javascript://, data://, etc.)
     * - Host must be non-null, non-empty, and not a wildcard
     * - No embedded credentials (user:pass@ patterns)
     * - No paths (except empty or /)
     * - No query strings
     * - No fragments
     * - Origins are trimmed of whitespace
     *
     * Invalid origins are logged and skipped; empty origins return empty list.
     *
     * @param originsProperty comma-separated origins (may be empty, null, or contain whitespace)
     * @return List of validated origins; empty list if none valid
     */
    List<String> parseAndValidateOrigins(String originsProperty) {
        List<String> validOrigins = new ArrayList<>();

        if (originsProperty == null || originsProperty.isBlank()) {
            logger.warn("CORS allowed-origins property is null or empty");
            return validOrigins;
        }

        String[] origins = originsProperty.split(",");
        for (String origin : origins) {
            String trimmed = origin.trim();

            // Skip empty values
            if (trimmed.isEmpty()) {
                logger.warn("Skipping empty CORS origin");
                continue;
            }

            try {
                // Parse and validate using URI class
                java.net.URI uri = new java.net.URI(trimmed);
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

                // Validate host is present and not a wildcard
                if (host == null || host.isEmpty() || "*".equals(host)) {
                    logger.warn("Rejecting CORS origin with invalid host: {}", trimmed);
                    continue;
                }

                // Check for embedded credentials
                if (uri.getUserInfo() != null) {
                    logger.warn("Rejecting CORS origin with embedded credentials: {}", trimmed);
                    continue;
                }

                // Reject paths (except empty or /)
                if (path != null && !path.isEmpty() && !path.equals("/")) {
                    logger.warn("Rejecting CORS origin with path '{}': {}", path, trimmed);
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

                // All validations passed
                validOrigins.add(trimmed);
                logger.debug("Accepted CORS origin: {}", trimmed);

            } catch (java.net.URISyntaxException e) {
                logger.warn("Rejecting malformed CORS origin '{}': {}", trimmed, e.getMessage());
            }
        }

        return validOrigins;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> validOrigins = parseAndValidateOrigins(allowedOriginsProperty);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(validOrigins)))
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/auth/login",
                                "/auth/signup",
                                "/auth/forgotPassword",
                                "/auth/verifyOtp",
                                "/auth/resetPassword",
                                "/auth/diagnostics/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/actuator/health",
                                "/actuator/prometheus"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .successHandler(oAuthSuccessHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}