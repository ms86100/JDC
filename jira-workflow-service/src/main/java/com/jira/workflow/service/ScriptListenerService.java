package com.jira.workflow.service;

import com.jira.workflow.engine.script.ScriptExecutionService;
import com.jira.workflow.engine.script.ScriptResult;
import com.jira.workflow.entity.ScriptDefinition;
import com.jira.workflow.entity.ScriptListener;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import com.jira.workflow.repository.ScriptListenerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptListenerService {

    private final ScriptListenerRepository listenerRepository;
    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptExecutionService scriptExecutionService;

    @Async
    public void fireEvent(String eventType, UUID issueId, UUID projectId, UUID userId,
                          UUID issueTypeId, Map<String, Object> eventData) {
        List<ScriptListener> listeners = listenerRepository
                .findByEventTypeAndIsEnabledTrueOrderByExecutionOrderAsc(eventType);

        for (ScriptListener listener : listeners) {
            if (listener.getProjectFilter() != null && !listener.getProjectFilter().equals(projectId)) {
                continue;
            }
            if (listener.getIssueTypeFilter() != null && !listener.getIssueTypeFilter().equals(issueTypeId)) {
                continue;
            }

            ScriptDefinition script = scriptDefinitionRepository.findById(listener.getScriptId()).orElse(null);
            if (script == null || !Boolean.TRUE.equals(script.getIsEnabled())) {
                continue;
            }

            try {
                Map<String, Object> context = new HashMap<>();
                context.put("issueId", issueId != null ? issueId.toString() : null);
                context.put("projectId", projectId != null ? projectId.toString() : null);
                context.put("userId", userId != null ? userId.toString() : null);
                context.put("issueTypeId", issueTypeId != null ? issueTypeId.toString() : null);
                context.put("eventType", eventType);
                if (eventData != null) {
                    context.putAll(eventData);
                }

                ScriptResult result = scriptExecutionService.executeByKey(
                        script.getScriptKey(), context, "LISTENER");
                if (!result.success()) {
                    log.warn("Listener script '{}' for event {} failed: {}",
                            script.getScriptKey(), eventType, result.errorMessage());
                }
            } catch (Exception e) {
                log.error("Error executing listener script '{}' for event {}: {}",
                        script.getScriptKey(), eventType, e.getMessage());
            }
        }
    }

    @Transactional
    public ScriptListener createListener(UUID scriptId, String eventType, UUID projectFilter,
                                          UUID issueTypeFilter, UUID createdBy) {
        ScriptListener listener = ScriptListener.builder()
                .scriptId(scriptId)
                .eventType(eventType)
                .projectFilter(projectFilter)
                .issueTypeFilter(issueTypeFilter)
                .isEnabled(true)
                .createdBy(createdBy)
                .build();
        return listenerRepository.save(listener);
    }

    @Transactional(readOnly = true)
    public List<ScriptListener> getListenersForScript(UUID scriptId) {
        return listenerRepository.findByScriptIdOrderByEventTypeAsc(scriptId);
    }

    @Transactional(readOnly = true)
    public List<ScriptListener> getAllListeners() {
        return listenerRepository.findByIsEnabledTrueOrderByExecutionOrderAsc();
    }

    @Transactional
    public void deleteListener(UUID listenerId) {
        listenerRepository.deleteById(listenerId);
    }

    @Transactional
    public ScriptListener toggleListener(UUID listenerId) {
        ScriptListener listener = listenerRepository.findById(listenerId).orElseThrow();
        listener.setIsEnabled(!listener.getIsEnabled());
        return listenerRepository.save(listener);
    }
}
