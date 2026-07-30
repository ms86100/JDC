package com.avionics_systems.workflow.service;

import com.avionics_systems.workflow.engine.script.ScriptExecutionService;
import com.avionics_systems.workflow.engine.script.ScriptResult;
import com.avionics_systems.workflow.entity.ScriptDefinition;
import com.avionics_systems.workflow.entity.ScriptFieldBehavior;
import com.avionics_systems.workflow.repository.ScriptDefinitionRepository;
import com.avionics_systems.workflow.repository.ScriptFieldBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptFieldBehaviorService {

    private final ScriptFieldBehaviorRepository behaviorRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptExecutionService scriptExecutionService;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> evaluateFieldBehaviors(String screenContext, UUID projectId,
                                                             UUID issueTypeId, Map<String, Object> issueData,
                                                             UUID userId) {
        List<ScriptFieldBehavior> behaviors = behaviorRepository
                .findByScreenContextAndIsEnabledTrueOrderByExecutionOrderAsc(screenContext);

        List<Map<String, Object>> allDirectives = new ArrayList<>();

        for (ScriptFieldBehavior behavior : behaviors) {
            if (behavior.getProjectId() != null && !behavior.getProjectId().equals(projectId)) continue;
            if (behavior.getIssueTypeId() != null && !behavior.getIssueTypeId().equals(issueTypeId)) continue;

            ScriptDefinition script = scriptDefinitionRepository.findById(behavior.getScriptId()).orElse(null);
            if (script == null || !Boolean.TRUE.equals(script.getIsEnabled())) continue;

            try {
                Map<String, Object> context = new HashMap<>();
                context.put("screenContext", screenContext);
                context.put("projectId", projectId != null ? projectId.toString() : null);
                context.put("issueTypeId", issueTypeId != null ? issueTypeId.toString() : null);
                context.put("userId", userId != null ? userId.toString() : null);
                context.put("issueData", issueData != null ? issueData : Map.of());

                ScriptResult result = scriptExecutionService.executeByKey(
                        script.getScriptKey(), context, "FIELD_BEHAVIOR");

                if (result.success() && result.value() instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> m) {
                            Map<String, Object> directive = new HashMap<>();
                            m.forEach((k, v) -> directive.put(String.valueOf(k), v));
                            allDirectives.add(directive);
                        }
                    }
                } else if (result.success() && result.value() instanceof Map<?, ?> m) {
                    Map<String, Object> directive = new HashMap<>();
                    m.forEach((k, v) -> directive.put(String.valueOf(k), v));
                    allDirectives.add(directive);
                }
            } catch (Exception e) {
                log.error("Error evaluating field behavior script '{}': {}", script.getScriptKey(), e.getMessage());
            }
        }

        return allDirectives;
    }

    @Transactional
    public ScriptFieldBehavior createBehavior(UUID scriptId, String screenContext, UUID projectId,
                                               UUID issueTypeId, UUID createdBy) {
        return behaviorRepository.save(ScriptFieldBehavior.builder()
                .scriptId(scriptId)
                .screenContext(screenContext)
                .projectId(projectId)
                .issueTypeId(issueTypeId)
                .isEnabled(true)
                .createdBy(createdBy)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ScriptFieldBehavior> getBehaviorsForScript(UUID scriptId) {
        return behaviorRepository.findByScriptId(scriptId);
    }

    @Transactional(readOnly = true)
    public List<ScriptFieldBehavior> getAllBehaviors() {
        return behaviorRepository.findAll();
    }

    @Transactional
    public void deleteBehavior(UUID behaviorId) {
        behaviorRepository.deleteById(behaviorId);
    }
}
