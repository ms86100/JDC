package com.jira.cluster.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "cluster.datasource.read-replica.enabled", havingValue = "true")
public class DataSourceRoutingAutoConfiguration {

    @Value("${cluster.datasource.read-replica.url:}")
    private String replicaUrl;

    @Value("${cluster.datasource.read-replica.username:${spring.datasource.username:}}")
    private String replicaUsername;

    @Value("${cluster.datasource.read-replica.password:${spring.datasource.password:}}")
    private String replicaPassword;

    @Bean
    @Primary
    public DataSource routingDataSource(DataSourceProperties properties) {
        HikariDataSource primary = new HikariDataSource();
        primary.setJdbcUrl(properties.getUrl());
        primary.setUsername(properties.getUsername());
        primary.setPassword(properties.getPassword());
        primary.setPoolName("primary");

        HikariDataSource replica = new HikariDataSource();
        replica.setJdbcUrl(replicaUrl.isBlank() ? properties.getUrl() : replicaUrl);
        replica.setUsername(replicaUsername.isBlank() ? properties.getUsername() : replicaUsername);
        replica.setPassword(replicaPassword.isBlank() ? properties.getPassword() : replicaPassword);
        replica.setPoolName("replica");
        replica.setReadOnly(true);

        ReadOnlyRoutingDataSource routing = new ReadOnlyRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put("primary", primary);
        targets.put("replica", replica);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(primary);
        routing.afterPropertiesSet();

        return routing;
    }
}
