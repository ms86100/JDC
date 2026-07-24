package com.jira.workflow.service;

import com.jira.workflow.engine.WorkflowIntegrationClient;
import com.jira.workflow.engine.script.ScriptExecutionService;
import com.jira.workflow.engine.script.ScriptResult;
import com.jira.workflow.entity.ScriptCalculatedField;
import com.jira.workflow.entity.ScriptDefinition;
import com.jira.workflow.repository.ScriptCalculatedFieldRepository;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptCalculatedFieldService {

    private final ScriptCalculatedFieldRepository calculatedFieldRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptExecutionService scriptExecutionService;
    private final WorkflowIntegrationClient integrationClient;

    public Map<String, Object> evaluateField(UUID issueId, UUID customFieldId) {
        Optional<ScriptCalculatedField> binding = calculatedFieldRepository
                .findByCustomFieldIdAndIsEnabledTrue(customFieldId);

        if (binding.isEmpty()) {
            return Map.of("hasScript", false);
        }

        ScriptDefinition script = scriptDefinitionRepository.findById(binding.get().getScriptId()).orElse(null);
        if (script == null || !Boolean.TRUE.equals(script.getIsEnabled())) {
            return Map.of("hasScript", true, "error", "Script not found or disabled");
        }

        Map<String, Object> issueData = integrationClient.fetchIssue(issueId);

        Map<String, Object> context = new HashMap<>();
        context.put("issueId", issueId.toString());
        context.put("customFieldId", customFieldId.toString());
        context.put("issueData", issueData);
        context.put("projectId", issueData.get("projectId") != null ? issueData.get("projectId").toString() : null);

        ScriptResult result = scriptExecutionService.executeByKey(
                script.getScriptKey(), context, "CALCULATED_FIELD");

        Map<String, Object> response = new HashMap<>();
        response.put("hasScript", true);
        response.put("success", result.success());
        response.put("value", result.value());
        if (!result.success()) {
            response.put("error", result.errorMessage());
        }
        return response;
    }

    @Transactional
    public ScriptCalculatedField createBinding(UUID scriptId, UUID customFieldId, Long cacheTtlMs, UUID createdBy) {
        return calculatedFieldRepository.save(ScriptCalculatedField.builder()
                .scriptId(scriptId)
                .customFieldId(customFieldId)
                .cacheTtlMs(cacheTtlMs != null ? cacheTtlMs : 0L)
                .isEnabled(true)
                .createdBy(createdBy)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ScriptCalculatedField> getBindingsForScript(UUID scriptId) {
        return calculatedFieldRepository.findByScriptId(scriptId);
    }

    @Transactional
    public void deleteBinding(UUID bindingId) {
        calculatedFieldRepository.deleteById(bindingId);
    }
}
