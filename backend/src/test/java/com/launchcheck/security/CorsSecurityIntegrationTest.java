package com.launchcheck.security;

import com.launchcheck.auth.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

public class CorsSecurityIntegrationTest {

    @Test
    public void testProdProfileRequiresExplicitAllowedOrigins() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        // No allowed-origins property set in prod

        SecurityConfig config = new SecurityConfig(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> config.corsConfigurationSource(env));
        assertTrue(ex.getMessage().contains("Production CORS allowed-origins"));
    }

    @Test
    public void testProdProfileAllowsConfiguredOrigins() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("launchcheck.security.cors.allowed-origins", "https://app.launchcheck.com, https://admin.launchcheck.com");

        SecurityConfig config = new SecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource(env);
        CorsConfiguration corsConfig = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertNotNull(corsConfig);
        assertEquals(2, corsConfig.getAllowedOrigins().size());
        assertTrue(corsConfig.getAllowedOrigins().contains("https://app.launchcheck.com"));
        assertTrue(corsConfig.getAllowedOrigins().contains("https://admin.launchcheck.com"));
        assertTrue(corsConfig.getAllowCredentials());
    }

    @Test
    public void testDevProfileDefaultsToLocalhostOrigins() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        SecurityConfig config = new SecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource(env);
        CorsConfiguration corsConfig = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest());

        assertNotNull(corsConfig);
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(corsConfig.getAllowedOrigins().contains("http://127.0.0.1:5174"));
        assertFalse(corsConfig.getAllowedOrigins().contains("http://evil.example"));
        assertTrue(corsConfig.getAllowCredentials());
    }
}
