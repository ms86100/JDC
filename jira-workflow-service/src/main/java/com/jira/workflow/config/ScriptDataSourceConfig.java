package com.jira.workflow.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

@Configuration
public class ScriptDataSourceConfig {

    @Bean
    public Map<String, DataSource> scriptDataSources() {
        return Collections.emptyMap();
    }
}
