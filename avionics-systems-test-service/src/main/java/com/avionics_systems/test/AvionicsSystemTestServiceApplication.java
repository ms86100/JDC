package com.avionics_systems.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AvionicsSystemTestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvionicsSystemTestServiceApplication.class, args);
    }
}