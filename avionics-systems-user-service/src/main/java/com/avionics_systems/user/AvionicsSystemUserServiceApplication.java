package com.avionics_systems.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {LdapAutoConfiguration.class})
@EnableScheduling
public class AvionicsSystemUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvionicsSystemUserServiceApplication.class, args);
    }
}