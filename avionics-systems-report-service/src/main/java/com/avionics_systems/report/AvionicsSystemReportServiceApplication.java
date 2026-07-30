package com.avionics_systems.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.avionics_systems.report.entity")
@EnableJpaRepositories(basePackages = "com.avionics_systems.report.repository")
public class AvionicsSystemReportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvionicsSystemReportServiceApplication.class, args);
    }
}