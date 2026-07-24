package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.*;

@Slf4j
public class JdcSqlApi {

    private final Map<String, JdbcTemplate> dataSources;
    private final boolean readOnly;

    public JdcSqlApi(Map<String, DataSource> scriptDataSources, boolean readOnly) {
        this.readOnly = readOnly;
        this.dataSources = new HashMap<>();
        if (scriptDataSources != null) {
            scriptDataSources.forEach((name, ds) ->
                    dataSources.put(name, new JdbcTemplate(ds)));
        }
    }

    @HostAccess.Export
    public List<Map<String, Object>> query(String dataSourceName, String sql, Object[] params) {
        try {
            JdbcTemplate jdbc = getJdbc(dataSourceName);
            if (jdbc == null) return List.of();
            if (!isSafeQuery(sql)) {
                log.warn("Blocked unsafe SQL from script: {}", sql.substring(0, Math.min(sql.length(), 100)));
                return List.of();
            }
            Object[] safeParams = params != null ? params : new Object[0];
            return jdbc.queryForList(sql, safeParams);
        } catch (Exception e) {
            log.warn("Script SQL query failed: {}", e.getMessage());
            return List.of();
        }
    }

    @HostAccess.Export
    public int update(String dataSourceName, String sql, Object[] params) {
        try {
            if (readOnly) {
                log.warn("Script SQL update blocked: read-only mode");
                return -1;
            }
            JdbcTemplate jdbc = getJdbc(dataSourceName);
            if (jdbc == null) return -1;
            if (!isSafeUpdate(sql)) {
                log.warn("Blocked unsafe SQL update from script: {}", sql.substring(0, Math.min(sql.length(), 100)));
                return -1;
            }
            Object[] safeParams = params != null ? params : new Object[0];
            return jdbc.update(sql, safeParams);
        } catch (Exception e) {
            log.warn("Script SQL update failed: {}", e.getMessage());
            return -1;
        }
    }

    @HostAccess.Export
    public List<String> getDataSources() {
        return new ArrayList<>(dataSources.keySet());
    }

    private JdbcTemplate getJdbc(String name) {
        if (name == null || !dataSources.containsKey(name)) {
            log.warn("Script requested unknown datasource: {}", name);
            return null;
        }
        return dataSources.get(name);
    }

    private boolean isSafeQuery(String sql) {
        if (sql == null) return false;
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("SELECT") || upper.startsWith("WITH");
    }

    private boolean isSafeUpdate(String sql) {
        if (sql == null) return false;
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("INSERT") || upper.startsWith("UPDATE") || upper.startsWith("DELETE");
    }
}
