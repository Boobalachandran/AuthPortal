package com.example.sportsapp.auth.oauth;

import com.example.sportsapp.auth.entitiy.User;
import com.example.sportsapp.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Date;

@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    public OAuthSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String email = oAuth2User.getAttribute("email");

            if (email == null || email.isEmpty()) {
                System.err.println("ERROR: No email found in OAuth2User attributes");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to extract email from OAuth provider");
                return;
            }

            System.out.println("OAuth2 user authenticated: " + email);

            // Check if user exists in the database, if not create a new user
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                System.out.println("Creating new user from OAuth2: " + email);
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setProvider("GOOGLE");
                return userRepository.save(newUser);
            });

            String token = generateToken(user);

            // Redirect to frontend dashboard with token and email
            String redirectUrl = "http://localhost:3000/dashboard?token=" + token + "&email=" + email;
            System.out.println("Redirecting to: " + redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            System.err.println("ERROR in OAuthSuccessHandler: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth authentication failed: " + e.getMessage());
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

