package com.example.sportsapp.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SecurityConfig CORS origin validation.
 * Verifies that CORS origins are properly validated and that invalid origins are rejected.
 */
class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        // Create instance with mocked dependencies (not needed for origin validation tests)
        securityConfig = new SecurityConfig(null, null, null, null);
    }

    // ───── Valid CORS Origins ─────

    @Test
    void parseAndValidateOrigins_acceptsValidHttpOrigin() {
        List<String> origins = securityConfig.parseAndValidateOrigins("http://localhost:3000");

        assertNotNull(origins);
        assertEquals(1, origins.size());
        assertTrue(origins.contains("http://localhost:3000"));
    }

    @Test
    void parseAndValidateOrigins_acceptsValidHttpsOrigin() {
        List<String> origins = securityConfig.parseAndValidateOrigins("https://app.example.com");

        assertNotNull(origins);
        assertEquals(1, origins.size());
        assertTrue(origins.contains("https://app.example.com"));
    }

    @Test
    void parseAndValidateOrigins_acceptsOriginWithPort() {
        List<String> origins = securityConfig.parseAndValidateOrigins("https://app.example.com:8443");

        assertNotNull(origins);
        assertEquals(1, origins.size());
        assertTrue(origins.contains("https://app.example.com:8443"));
    }

    @Test
    void parseAndValidateOrigins_acceptsMultipleValidOrigins() {
        List<String> origins = securityConfig.parseAndValidateOrigins(
            "http://localhost:3000, https://app.example.com, https://staging.example.com:8443");

        assertNotNull(origins);
        assertEquals(3, origins.size());
        assertTrue(origins.contains("http://localhost:3000"));
        assertTrue(origins.contains("https://app.example.com"));
        assertTrue(origins.contains("https://staging.example.com:8443"));
    }

    @Test
    void parseAndValidateOrigins_trimsWhitespace() {
        List<String> origins = securityConfig.parseAndValidateOrigins(
            "  http://localhost:3000  , https://app.example.com  ");

        assertNotNull(origins);
        assertEquals(2, origins.size());
        assertTrue(origins.contains("http://localhost:3000"));
        assertTrue(origins.contains("https://app.example.com"));
    }

    // ───── Invalid Schemes ─────

    @Test
    void parseAndValidateOrigins_rejectsFileScheme() {
        List<String> origins = securityConfig.parseAndValidateOrigins("file:///etc/passwd");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "file:// scheme should be rejected");
    }

    @Test
    void parseAndValidateOrigins_rejectsJavascriptScheme() {
        List<String> origins = securityConfig.parseAndValidateOrigins("javascript:alert(1)");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "javascript: scheme should be rejected");
    }

    @Test
    void parseAndValidateOrigins_rejectsDataScheme() {
        List<String> origins = securityConfig.parseAndValidateOrigins("data:text/html,<script>alert(1)</script>");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "data: scheme should be rejected");
    }

    @Test
    void parseAndValidateOrigins_rejectsNoScheme() {
        List<String> origins = securityConfig.parseAndValidateOrigins("localhost:3000");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "origin without scheme should be rejected");
    }

    // ───── Invalid Hosts ─────

    @Test
    void parseAndValidateOrigins_rejectsWildcardHost() {
        List<String> origins = securityConfig.parseAndValidateOrigins("http://*");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "wildcard host should be rejected");
    }

    @Test
    void parseAndValidateOrigins_rejectsEmptyHost() {
        List<String> origins = securityConfig.parseAndValidateOrigins("http://");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "empty host should be rejected");
    }

    @Test
    void parseAndValidateOrigins_rejectsEmbeddedCredentials() {
        List<String> origins = securityConfig.parseAndValidateOrigins("http://user:pass@localhost:3000");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "embedded credentials should be rejected");
    }

    // ───── Paths, Queries, and Fragments ─────

    @Test
    void parseAndValidateOrigins_rejectsOriginWithPath() {
        List<String> origins = securityConfig.parseAndValidateOrigins("http://localhost:3000/dashboard");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "origin with path should be rejected");
    }

    @Test
    void parseAndValidateOrigins_rejectsOriginWithQueryString() {
        List<String> origins = securityConfig.parseAndValidateOrigins("http://localhost:3000?token=abc");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "origin with query string should be rejected");
    }

    @Test
    void parseAndValidateOrigins_rejectsOriginWithFragment() {
        List<String> origins = securityConfig.parseAndValidateOrigins("http://localhost:3000#section");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "origin with fragment should be rejected");
    }

    // ───── Empty and Null Cases ─────

    @Test
    void parseAndValidateOrigins_returnsEmptyListForNullProperty() {
        List<String> origins = securityConfig.parseAndValidateOrigins(null);

        assertNotNull(origins);
        assertTrue(origins.isEmpty());
    }

    @Test
    void parseAndValidateOrigins_returnsEmptyListForEmptyProperty() {
        List<String> origins = securityConfig.parseAndValidateOrigins("");

        assertNotNull(origins);
        assertTrue(origins.isEmpty());
    }

    @Test
    void parseAndValidateOrigins_returnsEmptyListForBlankProperty() {
        List<String> origins = securityConfig.parseAndValidateOrigins("   ");

        assertNotNull(origins);
        assertTrue(origins.isEmpty());
    }

    @Test
    void parseAndValidateOrigins_skipsEmptyOriginsInList() {
        List<String> origins = securityConfig.parseAndValidateOrigins(
            "http://localhost:3000, , https://app.example.com");

        assertNotNull(origins);
        assertEquals(2, origins.size());
        assertTrue(origins.contains("http://localhost:3000"));
        assertTrue(origins.contains("https://app.example.com"));
    }

    // ───── Malformed URLs ─────

    @Test
    void parseAndValidateOrigins_rejectsMalformedUrl() {
        List<String> origins = securityConfig.parseAndValidateOrigins("ht!tp://[invalid");

        assertNotNull(origins);
        assertTrue(origins.isEmpty(), "malformed URL should be rejected");
    }

    @Test
    void parseAndValidateOrigins_mixesValidAndInvalidOrigins() {
        List<String> origins = securityConfig.parseAndValidateOrigins(
            "http://localhost:3000, file:///path, https://app.example.com, javascript:alert(1)");

        assertNotNull(origins);
        assertEquals(2, origins.size());
        assertTrue(origins.contains("http://localhost:3000"));
        assertTrue(origins.contains("https://app.example.com"));
    }

    // ───── Configuration Validation ─────

    @Test
    void validateConfiguration_failsIfNoValidOrigins() {
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsProperty", "invalid://url");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> securityConfig.validateConfiguration());
        assertTrue(ex.getMessage().contains("CORS configuration error"));
        assertTrue(ex.getMessage().contains("invalid values") || ex.getMessage().contains("empty"));
    }

    @Test
    void validateConfiguration_passesIfValidOriginExists() {
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsProperty", "http://localhost:3000");

        assertDoesNotThrow(() -> securityConfig.validateConfiguration());
    }

    @Test
    void validateConfiguration_passesIfAtLeastOneValidOriginExists() {
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsProperty",
            "invalid://url, http://localhost:3000, file:///path");

        assertDoesNotThrow(() -> securityConfig.validateConfiguration());
    }
}

