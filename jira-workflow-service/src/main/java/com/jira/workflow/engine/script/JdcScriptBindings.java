package com.jira.workflow.engine.script;

import com.jira.workflow.config.ScriptEngineProperties;
import com.jira.workflow.engine.WorkflowIntegrationClient;
import com.jira.workflow.repository.ScriptPersistentVarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class JdcScriptBindings {

    private final WorkflowIntegrationClient integrationClient;
    private final ScriptEngineProperties properties;
    private final Map<String, DataSource> scriptDataSources;
    private final ScriptPersistentVarRepository persistentVarRepository;

    @Value("${jira.services.notification-url:http://jira-notification-service:8087}")
    private String notificationServiceUrl;

    @Value("${jira.services.user-url:http://jira-user-service:8082}")
    private String userServiceUrl;

    @Value("${jira.services.confluence-url:}")
    private String confluenceUrl;

    @Value("${jira.services.plan-url:http://jira-plan-service:8092}")
    private String planServiceUrl;

    public Map<String, Object> buildBindings(Map<String, Object> workflowContext) {
        Map<String, Object> bindings = new HashMap<>();

        JdcApi jdc = new JdcApi(integrationClient, workflowContext);
        bindings.put("jdc", jdc);

        JdcConsole console = new JdcConsole();
        bindings.put("console", console);

        bindings.put("http", new JdcHttpApi(properties));
        bindings.put("env", new JdcEnvApi(properties));
        bindings.put("sql", new JdcSqlApi(scriptDataSources, !properties.isSqlWriteEnabled()));
        bindings.put("xml", new JdcXmlApi());
        bindings.put("vars", new JdcPersistentVarApi(persistentVarRepository, workflowContext));
        bindings.put("email", new JdcEmailApi(integrationClient.restTemplate(), notificationServiceUrl));
        bindings.put("ldap", new JdcLdapApi(integrationClient.restTemplate(), userServiceUrl));
        bindings.put("confluence", new JdcConfluenceApi(integrationClient.restTemplate(), confluenceUrl));
        bindings.put("sprint", new JdcSprintApi(integrationClient.restTemplate(), planServiceUrl, integrationClient.getIssueServiceUrl()));
        JdcWebhookApi webhookApi = new JdcWebhookApi();
        if (workflowContext.containsKey("_requestHeaders")) {
            @SuppressWarnings("unchecked")
            Map<String, String> reqHeaders = (Map<String, String>) workflowContext.get("_requestHeaders");
            webhookApi.setRequestHeaders(reqHeaders);
        }
        bindings.put("webhook", webhookApi);
        bindings.put("test", new JdcTestApi());
        bindings.put("file", new JdcFileApi());

        Set<String> resolvedIncludes = workflowContext.containsKey("_resolvedIncludes")
                ? new java.util.HashSet<>((java.util.Collection<String>) workflowContext.get("_resolvedIncludes"))
                : new java.util.HashSet<>();
        JdcIncludeApi includeApi = new JdcIncludeApi(resolvedIncludes);
        bindings.put("include", includeApi);

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
