package com.jira.workflow.engine.script;

import com.jira.workflow.config.ScriptEngineProperties;
import com.jira.workflow.engine.WorkflowIntegrationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JdcScriptBindings {

    private final WorkflowIntegrationClient integrationClient;
    private final ScriptEngineProperties properties;
    private final Map<String, DataSource> scriptDataSources;

    public Map<String, Object> buildBindings(Map<String, Object> workflowContext) {
        Map<String, Object> bindings = new HashMap<>();

        JdcApi jdc = new JdcApi(integrationClient, workflowContext);
        bindings.put("jdc", jdc);

        JdcConsole console = new JdcConsole();
        bindings.put("console", console);

        bindings.put("http", new JdcHttpApi(properties));
        bindings.put("env", new JdcEnvApi(properties));
        bindings.put("sql", new JdcSqlApi(scriptDataSources, true));

        bindings.put("issueId", workflowContext.get("issueId"));
        bindings.put("projectId", workflowContext.get("projectId"));
        bindings.put("userId", workflowContext.get("userId"));
        bindings.put("issueTypeId", workflowContext.get("issueTypeId"));
        bindings.put("currentStatusId", workflowContext.get("currentStatusId"));
        bindings.put("transitionId", workflowContext.get("transitionId"));
        bindings.put("transitionName", workflowContext.get("transitionName"));
        bindings.put("fromStatusId", workflowContext.get("fromStatusId"));
        bindings.put("toStatusId", workflowContext.get("toStatusId"));
        bindings.put("comment", workflowContext.get("comment"));
        bindings.put("resolutionId", workflowContext.get("resolutionId"));
        bindings.put("screenInput", workflowContext.getOrDefault("screenInput", Map.of()));
        bindings.put("issueData", workflowContext.getOrDefault("issueData", Map.of()));
        bindings.put("userData", workflowContext.getOrDefault("userData", Map.of()));

        return bindings;
    }
}
