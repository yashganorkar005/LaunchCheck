package com.launchcheck.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

@Configuration
public class ProductionSecurityValidator {

    private final Environment env;

    public ProductionSecurityValidator(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void validateSecurityConfig() {
        // Only validate if NOT in a development profile (assuming 'dev' is default)
        String[] activeProfiles = env.getActiveProfiles();
        boolean isDev = activeProfiles.length == 0 || java.util.Arrays.asList(activeProfiles).contains("dev");

        if (!isDev) {
            String dbPassword = env.getProperty("SPRING_DATASOURCE_PASSWORD");
            if (dbPassword == null || dbPassword.equals("launchcheck")) {
                throw new IllegalStateException("Security Error: SPRING_DATASOURCE_PASSWORD must be configured in production.");
            }

            String jwtSecret = env.getProperty("JWT_SECRET");
            if (jwtSecret == null || jwtSecret.equals("0123456789012345678901234567890123456789012345678901234567890123")) {
                throw new IllegalStateException("Security Error: JWT_SECRET must be configured and secure in production.");
            }
            if (jwtSecret.length() < 32) {
                throw new IllegalStateException("Security Error: JWT_SECRET must be at least 32 characters long.");
            }
        }
    }
}
