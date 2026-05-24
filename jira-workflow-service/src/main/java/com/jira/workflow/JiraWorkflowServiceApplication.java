package com.jira.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JiraWorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiraWorkflowServiceApplication.class, args);
    }
}