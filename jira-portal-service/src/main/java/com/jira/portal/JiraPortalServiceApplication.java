package com.jira.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.jira.portal.entity")
@EnableJpaRepositories(basePackages = "com.jira.portal.repository")
public class JiraPortalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JiraPortalServiceApplication.class, args);
    }
}
