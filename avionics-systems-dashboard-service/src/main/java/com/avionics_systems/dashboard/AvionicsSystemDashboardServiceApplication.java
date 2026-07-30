package com.avionics_systems.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableCaching
@EntityScan(basePackages = "com.avionics_systems.dashboard.entity")
@EnableJpaRepositories(basePackages = "com.avionics_systems.dashboard.repository")
public class AvionicsSystemDashboardServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvionicsSystemDashboardServiceApplication.class, args);
    }
}
