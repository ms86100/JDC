package com.avionics_systems.issue;

import com.avionics_systems.issue.service.CiCdIntegrationService;
import com.avionics_systems.issue.service.ImportService;
import com.avionics_systems.issue.service.ReportingService;
import com.avionics_systems.issue.service.TestManagementService;
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
        basePackages = "com.avionics_systems.issue",
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
                        pattern = "com\\.avionics_systems\\.issue\\.controller\\.(Test|Import|CiCd|Report|Traceability).*")
        })
public class AvionicsSystemIssueServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvionicsSystemIssueServiceApplication.class, args);
    }
}