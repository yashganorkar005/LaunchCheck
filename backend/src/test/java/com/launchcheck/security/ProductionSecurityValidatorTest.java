package com.launchcheck.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

public class ProductionSecurityValidatorTest {

    @Test
    public void testProductionValidationFailsWithoutSecureConfig() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("SPRING_DATASOURCE_PASSWORD", "launchcheck");
        env.setProperty("JWT_SECRET", "super-secret-key-that-is-at-least-32-chars-long");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConfig);
        assertTrue(ex.getMessage().contains("SPRING_DATASOURCE_PASSWORD must be configured in production"));
    }

    @Test
    public void testProductionValidationFailsWithDefaultJwtSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("SPRING_DATASOURCE_PASSWORD", "strong_prod_password_123!");
        env.setProperty("JWT_SECRET", "0123456789012345678901234567890123456789012345678901234567890123");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConfig);
        assertTrue(ex.getMessage().contains("JWT_SECRET must be configured and secure in production"));
    }

    @Test
    public void testProductionValidationPassesWithValidConfig() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("SPRING_DATASOURCE_PASSWORD", "strong_prod_password_123!");
        env.setProperty("JWT_SECRET", "a_really_long_and_very_secure_production_jwt_secret_token_key_123");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        assertDoesNotThrow(validator::validateSecurityConfig);
    }

    @Test
    public void testDevProfileSkipsValidation() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        assertDoesNotThrow(validator::validateSecurityConfig);
    }
}

