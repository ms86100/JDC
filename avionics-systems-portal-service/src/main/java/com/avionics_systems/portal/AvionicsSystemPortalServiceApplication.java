package com.avionics_systems.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.avionics_systems.portal.entity")
@EnableJpaRepositories(basePackages = "com.avionics_systems.portal.repository")
public class AvionicsSystemPortalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvionicsSystemPortalServiceApplication.class, args);
    }
}
