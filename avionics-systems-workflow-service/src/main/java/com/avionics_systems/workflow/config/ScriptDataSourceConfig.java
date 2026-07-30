package com.avionics_systems.workflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class ScriptDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "avionics-systems.scripting")
    public ScriptDataSourceProperties scriptDataSourceProperties() {
        return new ScriptDataSourceProperties();
    }

    @Bean
    public Map<String, DataSource> scriptDataSources(ScriptDataSourceProperties props) {
        if (props.getDatasources() == null || props.getDatasources().isEmpty()) {
            log.info("No script datasources configured — SQL API will be unavailable");
            return Collections.emptyMap();
        }
        Map<String, DataSource> result = new HashMap<>();
        props.getDatasources().forEach((name, config) -> {
            try {
                DataSource ds = DataSourceBuilder.create()
                        .url(config.getUrl())
                        .username(config.getUsername())
                        .password(config.getPassword())
                        .driverClassName(config.getDriverClassName() != null
                                ? config.getDriverClassName()
                                : "org.postgresql.Driver")
                        .build();
                result.put(name, ds);
                log.info("Script datasource '{}' configured: {}", name, config.getUrl());
            } catch (Exception e) {
                log.error("Failed to configure script datasource '{}': {}", name, e.getMessage());
            }
        });
        return result;
    }

    @lombok.Getter
    @lombok.Setter
    public static class ScriptDataSourceProperties {
        private Map<String, DsConfig> datasources = new HashMap<>();

        @lombok.Getter
        @lombok.Setter
        public static class DsConfig {
            private String url;
            private String username;
            private String password;
            private String driverClassName;
        }
    }
}
