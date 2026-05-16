package com.jira.notification.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;

/**
 * Health configuration for notification service.
 * Makes mail health check optional so service shows UP even without SMTP configured.
 */
@Configuration
public class HealthConfig {

    /**
     * Custom mail health indicator that shows UP when email is disabled or not configured.
     * Only shows DOWN when email is enabled but SMTP connection fails.
     */
    @Bean
    public HealthIndicator optionalMailHealthIndicator(org.springframework.mail.javamail.JavaMailSender mailSender) {
        return () -> {
            try {
                // Only test connection if email is enabled
                String emailEnabled = System.getenv("EMAIL_ENABLED");
                if ("true".equalsIgnoreCase(emailEnabled)) {
                    // Test the connection
                    mailSender.testConnection();
                    return Health.up()
                        .withDetail("mail", "SMTP connection successful")
                        .build();
                } else {
                    return Health.up()
                        .withDetail("mail", "Email disabled - SMTP not configured")
                        .build();
                }
            } catch (MailException e) {
                return Health.down()
                    .withDetail("mail", "SMTP connection failed: " + e.getMessage())
                    .build();
            } catch (Exception e) {
                return Health.up()
                    .withDetail("mail", "Email disabled or not configured")
                    .build();
            }
        };
    }
}