package com.jira.issue;

import com.jira.issue.service.CiCdIntegrationService;
import com.jira.issue.service.ImportService;
import com.jira.issue.service.ReportingService;
import com.jira.issue.service.TestManagementService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration.class
})
@ComponentScan(
        basePackages = "com.jira.issue",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                ImportService.class,
                                CiCdIntegrationService.class,
                                TestManagementService.class,
                                ReportingService.class
                        }),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.jira\\.issue\\.controller\\.(Test|Import|CiCd|Report|Traceability).*")
        })
public class JiraIssueServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiraIssueServiceApplication.class, args);
    }
}