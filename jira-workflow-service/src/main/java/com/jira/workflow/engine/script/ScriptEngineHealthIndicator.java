package com.jira.workflow.engine.script;

import com.jira.workflow.config.ScriptEngineProperties;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import com.jira.workflow.repository.ScriptExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScriptEngineHealthIndicator implements HealthIndicator {

    private final ScriptEngineProperties properties;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final GraalScriptEngine graalScriptEngine;

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up().withDetail("scriptEngine", "disabled").build();
        }

        try {
            graalScriptEngine.parseOnly("1+1");

            long totalScripts = scriptDefinitionRepository.count();
            long enabledScripts = scriptDefinitionRepository.findByIsEnabledTrueOrderByNameAsc().size();

            return Health.up()
                    .withDetail("scriptEngine", "operational")
                    .withDetail("totalScripts", totalScripts)
                    .withDetail("enabledScripts", enabledScripts)
                    .withDetail("timeoutMs", properties.getTimeoutMs())
                    .withDetail("maxStatements", properties.getMaxStatements())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("scriptEngine", "failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
