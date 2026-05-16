package com.jira.notification.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Health configuration for notification service.
 * Makes mail health check optional so service shows UP even without SMTP configured.
 */
@Configuration
public class HealthConfig {

    /**
     * Custom mail health indicator that shows UP when email is disabled or not configured.
     */
    @Bean
    public HealthIndicator optionalMailHealthIndicator() {
        return () -> {
            String emailEnabled = System.getenv("EMAIL_ENABLED");
            if ("true".equalsIgnoreCase(emailEnabled)) {
                return Health.up()
                    .withDetail("mail", "SMTP enabled")
                    .build();
            } else {
                return Health.up()
                    .withDetail("mail", "Email disabled - SMTP not configured")
                    .build();
            }
        };
    }
}