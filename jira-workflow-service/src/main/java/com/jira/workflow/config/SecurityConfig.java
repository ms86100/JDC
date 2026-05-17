package com.jira.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        var user = User.withDefaultPasswordEncoder()
            .username("admin")
            .password("admin123")
            .roles("USER", "ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow health checks and swagger
                .requestMatchers("/actuator/**", "/api-docs/**", "/swagger-ui/**").permitAll()
                // Workflow endpoints - allow without auth for development
                .requestMatchers("/api/workflows/**").permitAll()
                // Workflow scheme endpoints - allow without auth for development
                .requestMatchers("/api/workflow-schemes/**").permitAll()
                // Admin endpoints - allow without auth for development
                .requestMatchers("/api/admin/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});

        return http.build();
    }
}