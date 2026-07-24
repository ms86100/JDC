package com.jira.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {LdapAutoConfiguration.class})
@EnableScheduling
public class JiraUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiraUserServiceApplication.class, args);
    }
}