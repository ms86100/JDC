package com.jira.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.jira.report.entity")
@EnableJpaRepositories(basePackages = "com.jira.report.repository")
public class JiraReportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JiraReportServiceApplication.class, args);
    }
}