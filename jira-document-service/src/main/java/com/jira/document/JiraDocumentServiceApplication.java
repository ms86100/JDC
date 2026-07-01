package com.jira.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.jira.document.entity")
@EnableJpaRepositories(basePackages = "com.jira.document.repository")
public class JiraDocumentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JiraDocumentServiceApplication.class, args);
    }
}