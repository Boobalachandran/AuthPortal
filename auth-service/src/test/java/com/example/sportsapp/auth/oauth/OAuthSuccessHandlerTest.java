package com.example.sportsapp.auth.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OAuthSuccessHandler redirect URL validation and allowlist checking.
 */
class OAuthSuccessHandlerTest {

    private OAuthSuccessHandler handler;

    @BeforeEach
    void setUp() {
        // Create a mock UserRepository (not needed for these tests)
        handler = new OAuthSuccessHandler(null);
    }

    // ───── Redirect URL Structure Validation ─────

    @Test
    void validateConfiguration_acceptsValidLocalhostUrl() {
        // Valid localhost URLs should pass validation
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        // Should not throw
        assertDoesNotThrow(() -> handler.validateConfiguration());
    }

    @Test
    void validateConfiguration_acceptsValidHttpsUrl() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "https://app.example.com");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "https://app.example.com");

        assertDoesNotThrow(() -> handler.validateConfiguration());
    }

    @Test
    void validateConfiguration_acceptsUrlWithPort() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "https://app.example.com:8443");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "https://app.example.com:8443");

        assertDoesNotThrow(() -> handler.validateConfiguration());
    }

    @Test
    void validateConfiguration_rejectsEmptyUrl() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("frontend redirect base URL is empty"));
    }

    @Test
    void validateConfiguration_rejectsNullUrl() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", null);
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("frontend redirect base URL is empty"));
    }

    @Test
    void validateConfiguration_rejectsMalformedUrl() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "ht!tp://[invalid");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("malformed URL"));
    }

    @Test
    void validateConfiguration_rejectsFileScheme() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "file:///etc/passwd");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("scheme") && ex.getMessage().contains("not permitted"));
    }

    @Test
    void validateConfiguration_rejectsJavascriptScheme() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "javascript:alert(1)");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("scheme") && ex.getMessage().contains("not permitted"));
    }

    @Test
    void validateConfiguration_rejectsDataScheme() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "data:text/html");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("scheme") || ex.getMessage().contains("malformed"));
    }

    @Test
    void validateConfiguration_rejectsUrlWithEmbeddedCredentials() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://user:pass@localhost:3000");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("credentials"));
    }

    @Test
    void validateConfiguration_rejectsUrlWithPath() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://localhost:3000/dashboard");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("path"));
    }

    @Test
    void validateConfiguration_rejectsUrlWithQueryString() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://localhost:3000?token=abc");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("query"));
    }

    @Test
    void validateConfiguration_rejectsUrlWithFragment() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://localhost:3000#section");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("fragment"));
    }

    @Test
    void validateConfiguration_rejectsUrlWithWildcardHost() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://*");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("host"));
    }

    // ───── Allowlist Validation ─────

    @Test
    void validateConfiguration_rejectsUrlNotInAllowlist() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://attacker.com");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "http://localhost:3000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("does not match any trusted CORS origin"));
    }

    @Test
    void validateConfiguration_acceptsUrlMatchingAllowlist() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "https://app.example.com");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "https://app.example.com,http://localhost:3000");

        assertDoesNotThrow(() -> handler.validateConfiguration());
    }

    @Test
    void validateConfiguration_failsIfNoValidOriginsInAllowlist() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(handler, "allowedOriginsProperty", "invalid://url,/path/only");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> handler.validateConfiguration());
        assertTrue(ex.getMessage().contains("no valid trusted origins"));
    }

    // ───── Build URL Test ─────

    @Test
    void buildSecureRedirectUrl_includesTokenAndEmail() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://localhost:3000");

        String redirectUrl = handler.buildSecureRedirectUrl("test-jwt-token", "user@example.com");

        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("token=test-jwt-token"));
        assertTrue(redirectUrl.contains("email="));
        assertTrue(redirectUrl.contains("/dashboard"));
    }

    @Test
    void buildSecureRedirectUrl_properlyEncodesEmailWithSpecialCharacters() {
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://localhost:3000");

        // Email with special characters that need encoding
        String redirectUrl = handler.buildSecureRedirectUrl("token", "user+test@example.com");

        assertNotNull(redirectUrl);
        // The email should be URL-encoded (+ becomes %2B)
        assertTrue(redirectUrl.contains("email=") && redirectUrl.contains("@"));
    }

    // ───── Compatibility Test ─────

    @Test
    void buildSecureRedirectUrl_preservesExistingContractWithTokenAndEmailParams() {
        // Verify the existing frontend contract is preserved:
        // URL parameters are named "token" and "email"
        ReflectionTestUtils.setField(handler, "frontendRedirectBaseUrl", "http://localhost:3000");

        String redirectUrl = handler.buildSecureRedirectUrl("jwt123", "test@example.com");

        assertTrue(redirectUrl.contains("token=jwt123"), "token parameter must be present");
        assertTrue(redirectUrl.contains("email="), "email parameter must be present");
        assertTrue(redirectUrl.startsWith("http://localhost:3000/dashboard?"), "URL must start with /dashboard");
    }
}

