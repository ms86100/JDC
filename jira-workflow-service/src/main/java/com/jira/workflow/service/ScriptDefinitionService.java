package com.jira.workflow.service;

import com.jira.workflow.dto.*;
import com.jira.workflow.engine.script.ScriptPluginRegistrar;
import com.jira.workflow.entity.ScriptDefinition;
import com.jira.workflow.entity.ScriptExecutionLog;
import com.jira.workflow.entity.ScriptVersion;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import com.jira.workflow.repository.ScriptExecutionLogRepository;
import com.jira.workflow.repository.ScriptVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptDefinitionService {

    private final ScriptDefinitionRepository scriptDefinitionRepository;
    private final ScriptVersionRepository scriptVersionRepository;
    private final ScriptExecutionLogRepository executionLogRepository;
    private final ScriptPluginRegistrar scriptPluginRegistrar;

    @Transactional
    public ScriptResponse createScript(CreateScriptRequest request, UUID createdBy) {
        if (scriptDefinitionRepository.existsByScriptKey(request.getScriptKey())) {
            throw new IllegalArgumentException("Script key already exists: " + request.getScriptKey());
        }

        ScriptDefinition script = ScriptDefinition.builder()
                .name(request.getName())
                .description(request.getDescription())
                .scriptType(request.getScriptType())
                .scriptKey(request.getScriptKey())
                .scriptBody(request.getScriptBody())
                .version(1)
                .isEnabled(true)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();

        ScriptDefinition saved = scriptDefinitionRepository.save(script);

        ScriptVersion initialVersion = ScriptVersion.builder()
                .scriptId(saved.getId())
                .version(1)
                .scriptBody(request.getScriptBody())
                .changeSummary("Initial version")
                .createdBy(createdBy)
                .build();
        scriptVersionRepository.save(initialVersion);

        scriptPluginRegistrar.refreshScript(saved);
        log.info("Created script '{}' (key: {}, type: {})", saved.getName(), saved.getScriptKey(), saved.getScriptType());
        return mapToResponse(saved);
    }

    @Transactional
    public ScriptResponse updateScript(UUID id, UpdateScriptRequest request, UUID updatedBy) {
        ScriptDefinition script = scriptDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Script", "id", id));

        boolean bodyChanged = false;

        if (request.getName() != null) {
            script.setName(request.getName());
        }
        if (request.getDescription() != null) {
            script.setDescription(request.getDescription());
        }
        if (request.getIsEnabled() != null) {
            script.setIsEnabled(request.getIsEnabled());
        }
        if (request.getScriptBody() != null && !request.getScriptBody().equals(script.getScriptBody())) {
            script.setScriptBody(request.getScriptBody());
            script.setVersion(script.getVersion() + 1);
            bodyChanged = true;
        }
        script.setUpdatedBy(updatedBy);

        ScriptDefinition saved = scriptDefinitionRepository.save(script);

        if (bodyChanged) {
            ScriptVersion version = ScriptVersion.builder()
                    .scriptId(saved.getId())
                    .version(saved.getVersion())
                    .scriptBody(saved.getScriptBody())
                    .changeSummary(request.getChangeSummary())
                    .createdBy(updatedBy)
                    .build();
            scriptVersionRepository.save(version);
        }

        scriptPluginRegistrar.refreshScript(saved);
        log.info("Updated script '{}' to version {}", saved.getScriptKey(), saved.getVersion());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteScript(UUID id) {
        ScriptDefinition script = scriptDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Script", "id", id));
        log.info("Deleting script '{}' (key: {})", script.getName(), script.getScriptKey());
        scriptPluginRegistrar.unregisterScript(script);
        scriptDefinitionRepository.delete(script);
    }

    @Transactional(readOnly = true)
    public ScriptResponse getScript(UUID id) {
        return scriptDefinitionRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Script", "id", id));
    }

    @Transactional(readOnly = true)
    public ScriptResponse getScriptByKey(String key) {
        return scriptDefinitionRepository.findByScriptKey(key)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Script", "scriptKey", key));
    }

    @Transactional(readOnly = true)
    public List<ScriptResponse> listScripts(String type) {
        List<ScriptDefinition> scripts;
        if (type != null && !type.isBlank()) {
            scripts = scriptDefinitionRepository.findByScriptTypeOrderByNameAsc(type);
        } else {
            scripts = scriptDefinitionRepository.findAll();
        }
        return scripts.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ScriptResponse> listEnabledScriptsByType(String type) {
        return scriptDefinitionRepository.findByScriptTypeAndIsEnabledTrueOrderByNameAsc(type)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ScriptResponse toggleScript(UUID id, boolean enabled, UUID updatedBy) {
        ScriptDefinition script = scriptDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Script", "id", id));
        script.setIsEnabled(enabled);
        script.setUpdatedBy(updatedBy);
        ScriptDefinition saved = scriptDefinitionRepository.save(script);
        scriptPluginRegistrar.refreshScript(saved);
        log.info("Script '{}' {}", saved.getScriptKey(), enabled ? "enabled" : "disabled");
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ScriptVersionResponse> getVersionHistory(UUID scriptId) {
        if (!scriptDefinitionRepository.existsById(scriptId)) {
            throw new ResourceNotFoundException("Script", "id", scriptId);
        }
        return scriptVersionRepository.findByScriptIdOrderByVersionDesc(scriptId)
                .stream().map(this::mapVersionToResponse).toList();
    }

    @Transactional
    public ScriptResponse revertToVersion(UUID scriptId, Integer version, UUID updatedBy) {
        ScriptDefinition script = scriptDefinitionRepository.findById(scriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Script", "id", scriptId));

        ScriptVersion targetVersion = scriptVersionRepository.findByScriptIdAndVersion(scriptId, version)
                .orElseThrow(() -> new ResourceNotFoundException("ScriptVersion", "version", version));

        script.setScriptBody(targetVersion.getScriptBody());
        script.setVersion(script.getVersion() + 1);
        script.setUpdatedBy(updatedBy);
        ScriptDefinition saved = scriptDefinitionRepository.save(script);

        ScriptVersion revertVersion = ScriptVersion.builder()
                .scriptId(saved.getId())
                .version(saved.getVersion())
                .scriptBody(saved.getScriptBody())
                .changeSummary("Reverted to version " + version)
                .createdBy(updatedBy)
                .build();
        scriptVersionRepository.save(revertVersion);

        scriptPluginRegistrar.refreshScript(saved);
        log.info("Reverted script '{}' to version {} (now version {})", saved.getScriptKey(), version, saved.getVersion());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ScriptExecutionLogResponse> getExecutionLogs(UUID scriptId, Pageable pageable) {
        return executionLogRepository.findByScriptIdOrderByCreatedAtDesc(scriptId, pageable)
                .map(this::mapLogToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ScriptExecutionLogResponse> getAllExecutionLogs(Pageable pageable) {
        return executionLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapLogToResponse);
    }

    private ScriptResponse mapToResponse(ScriptDefinition entity) {
        return ScriptResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .scriptType(entity.getScriptType())
                .scriptKey(entity.getScriptKey())
                .scriptBody(entity.getScriptBody())
                .version(entity.getVersion())
                .isEnabled(entity.getIsEnabled())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ScriptVersionResponse mapVersionToResponse(ScriptVersion entity) {
        return ScriptVersionResponse.builder()
                .id(entity.getId())
                .scriptId(entity.getScriptId())
                .version(entity.getVersion())
                .scriptBody(entity.getScriptBody())
                .changeSummary(entity.getChangeSummary())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ScriptExecutionLogResponse mapLogToResponse(ScriptExecutionLog entity) {
        return ScriptExecutionLogResponse.builder()
                .id(entity.getId())
                .scriptId(entity.getScriptId())
                .scriptKey(entity.getScriptKey())
                .scriptType(entity.getScriptType())
                .executionMode(entity.getExecutionMode())
                .issueId(entity.getIssueId())
                .projectId(entity.getProjectId())
                .userId(entity.getUserId())
                .success(entity.getSuccess())
                .resultValue(entity.getResultValue())
                .errorMessage(entity.getErrorMessage())
                .executionMs(entity.getExecutionMs())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
