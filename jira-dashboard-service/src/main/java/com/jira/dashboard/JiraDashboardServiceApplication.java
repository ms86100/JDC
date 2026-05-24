package com.jira.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableCaching
@EntityScan(basePackages = "com.jira.dashboard.entity")
@EnableJpaRepositories(basePackages = "com.jira.dashboard.repository")
public class JiraDashboardServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JiraDashboardServiceApplication.class, args);
    }
}