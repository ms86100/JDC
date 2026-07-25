package com.jira.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // TODO: Security hardening — replace .requestMatchers("/api/**").permitAll()
            //  with .anyRequest().authenticated() once a JWT authentication filter is
            //  added to this service. Currently permitAll is required because there is
            //  no JWT filter to validate tokens.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/workflow/scripts/field-behaviors/evaluate").permitAll()
                .requestMatchers("/api/workflow/scripts/calculated-fields/evaluate").permitAll()
                .requestMatchers("/api/**").permitAll()
            )
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
