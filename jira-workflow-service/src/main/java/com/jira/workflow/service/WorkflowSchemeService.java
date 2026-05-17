package com.jira.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.dto.*;
import com.jira.workflow.entity.*;
import com.jira.workflow.exception.DuplicateResourceException;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowSchemeService {

    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final WorkflowSchemeMappingRepository workflowSchemeMappingRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowSharingRepository workflowSharingRepository;
    private final WorkflowAuditLogRepository workflowAuditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowSchemeResponse createScheme(CreateWorkflowSchemeRequest request, UUID userId) {
        log.info("Creating workflow scheme: {}", request.getName());

        if (workflowSchemeRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateResourceException("Workflow scheme with name '" + request.getName() + "' already exists");
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            workflowSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        workflowSchemeRepository.save(existing);
                    });
        }

        WorkflowScheme scheme = WorkflowScheme.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(request.getIsDefault())
                .defaultWorkflowId(request.getDefaultWorkflowId())
                .isDraft(false)
                .isActive(true)
                .createdBy(userId)
                .build();

        scheme = workflowSchemeRepository.save(scheme);
        log.info("Workflow scheme created: {}", scheme.getId());
        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public List<WorkflowSchemeResponse> listAllSchemes() {
        return workflowSchemeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowSchemeResponse getScheme(UUID schemeId) {
        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", schemeId));
        return mapToResponse(scheme);
    }

    @Transactional
    public WorkflowSchemeResponse updateScheme(UUID schemeId, CreateWorkflowSchemeRequest request, UUID userId) {
        log.info("Updating workflow scheme: {}", schemeId);

        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", schemeId));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(scheme.getIsDefault())) {
            workflowSchemeRepository.findByIsDefaultTrue()
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        workflowSchemeRepository.save(existing);
                    });
        }

        if (request.getName() != null) {
            scheme.setName(request.getName());
        }
        if (request.getDescription() != null) {
            scheme.setDescription(request.getDescription());
        }
        scheme.setIsDefault(request.getIsDefault());
        if (request.getDefaultWorkflowId() != null) {
            scheme.setDefaultWorkflowId(request.getDefaultWorkflowId());
        }
        scheme.setUpdatedBy(userId);

        scheme = workflowSchemeRepository.save(scheme);
        return mapToResponse(scheme);
    }

    @Transactional
    public void deleteScheme(UUID schemeId) {
        log.info("Deleting workflow scheme: {}", schemeId);

        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", schemeId));

        if (Boolean.TRUE.equals(scheme.getIsDefault())) {
            throw new IllegalStateException("Cannot delete default workflow scheme");
        }

        workflowSchemeRepository.delete(scheme);
        log.info("Workflow scheme deleted: {}", schemeId);
    }

    @Transactional
    public WorkflowSchemeResponse addMapping(UUID schemeId, WorkflowSchemeMappingRequest request) {
        log.info("Adding workflow mapping to scheme {}: issueType={}, workflow={}",
                schemeId, request.getIssueTypeId(), request.getWorkflowId());

        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", schemeId));

        workflowSchemeMappingRepository.findBySchemeIdAndIssueTypeId(schemeId, request.getIssueTypeId())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Mapping for issue type '" + request.getIssueTypeId() + "' already exists in scheme");
                });

        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", request.getWorkflowId()));

        WorkflowSchemeMapping mapping = WorkflowSchemeMapping.builder()
                .scheme(scheme)
                .issueTypeId(request.getIssueTypeId())
                .workflow(workflow)
                .build();

        mapping = workflowSchemeMappingRepository.save(mapping);
        return mapToResponse(scheme);
    }

    @Transactional
    public WorkflowSchemeResponse updateMapping(UUID schemeId, UUID mappingId, WorkflowSchemeMappingRequest request) {
        log.info("Updating workflow mapping {} in scheme {}", mappingId, schemeId);

        WorkflowSchemeMapping mapping = workflowSchemeMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowSchemeMapping", "id", mappingId));

        if (!mapping.getScheme().getId().equals(schemeId)) {
            throw new ResourceNotFoundException("Mapping does not belong to scheme");
        }

        if (request.getWorkflowId() != null) {
            Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", request.getWorkflowId()));
            mapping.setWorkflow(workflow);
        }

        mapping = workflowSchemeMappingRepository.save(mapping);
        return mapToResponse(mapping.getScheme());
    }

    @Transactional
    public WorkflowSchemeResponse removeMapping(UUID schemeId, UUID mappingId) {
        log.info("Removing workflow mapping {} from scheme {}", mappingId, schemeId);

        WorkflowSchemeMapping mapping = workflowSchemeMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowSchemeMapping", "id", mappingId));

        WorkflowScheme scheme = mapping.getScheme();
        workflowSchemeMappingRepository.delete(mapping);

        return mapToResponse(scheme);
    }

    @Transactional
    public WorkflowSchemeResponse createDraft(UUID schemeId, UUID userId) {
        log.info("Creating draft of scheme: {}", schemeId);

        WorkflowScheme original = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", schemeId));

        if (Boolean.TRUE.equals(original.getIsDraft())) {
            throw new IllegalStateException("Scheme is already a draft");
        }

        WorkflowScheme draft = WorkflowScheme.builder()
                .name(original.getName() + " (Draft)")
                .description(original.getDescription())
                .isDefault(false)
                .defaultWorkflowId(original.getDefaultWorkflowId())
                .isDraft(true)
                .draftOfSchemeId(original.getId())
                .isActive(true)
                .createdBy(userId)
                .build();

        draft = workflowSchemeRepository.save(draft);

        for (WorkflowSchemeMapping originalMapping : original.getMappings()) {
            WorkflowSchemeMapping draftMapping = WorkflowSchemeMapping.builder()
                    .scheme(draft)
                    .issueTypeId(originalMapping.getIssueTypeId())
                    .workflow(originalMapping.getWorkflow())
                    .build();
            workflowSchemeMappingRepository.save(draftMapping);
        }

        log.info("Draft scheme created: {}", draft.getId());
        return mapToResponse(draft);
    }

    @Transactional
    public WorkflowSchemeResponse publishDraft(UUID draftId, UUID userId) {
        log.info("Publishing draft scheme: {}", draftId);

        WorkflowScheme draft = workflowSchemeRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", draftId));

        if (!Boolean.TRUE.equals(draft.getIsDraft())) {
            throw new IllegalStateException("Scheme is not a draft");
        }

        WorkflowScheme original = workflowSchemeRepository.findById(draft.getDraftOfSchemeId())
                .orElseThrow(() -> new ResourceNotFoundException("Original scheme not found"));

        original.setName(draft.getName());
        original.setDescription(draft.getDescription());
        original.setDefaultWorkflowId(draft.getDefaultWorkflowId());
        original.setIsDraft(false);
        original.setUpdatedBy(userId);

        original = workflowSchemeRepository.save(original);

        workflowSchemeMappingRepository.deleteAll(original.getMappings());
        for (WorkflowSchemeMapping draftMapping : draft.getMappings()) {
            WorkflowSchemeMapping mapping = WorkflowSchemeMapping.builder()
                    .scheme(original)
                    .issueTypeId(draftMapping.getIssueTypeId())
                    .workflow(draftMapping.getWorkflow())
                    .build();
            workflowSchemeMappingRepository.save(mapping);
        }

        draft.setIsActive(false);
        workflowSchemeRepository.save(draft);

        return mapToResponse(original);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> getWorkflowForIssueType(UUID projectId, UUID issueTypeId) {
        log.debug("Getting workflow for project {} and issue type {}", projectId, issueTypeId);

        List<WorkflowSharing> sharings = workflowSharingRepository.findByProjectId(projectId);
        if (sharings.isEmpty()) {
            WorkflowScheme defaultScheme = workflowSchemeRepository.findByIsDefaultTrue().orElse(null);
            if (defaultScheme != null) {
                return workflowSchemeMappingRepository
                        .findBySchemeIdAndIssueTypeId(defaultScheme.getId(), issueTypeId)
                        .map(m -> m.getWorkflow().getId());
            }
            return Optional.empty();
        }

        WorkflowScheme scheme = workflowSchemeRepository.findById(sharings.get(0).getSchemeId()).orElse(null);
        if (scheme == null) {
            return Optional.empty();
        }

        return workflowSchemeMappingRepository
                .findBySchemeIdAndIssueTypeId(scheme.getId(), issueTypeId)
                .map(m -> m.getWorkflow().getId());
    }

    private WorkflowSchemeResponse mapToResponse(WorkflowScheme scheme) {
        List<WorkflowSchemeMappingResponse> mappings = scheme.getMappings().stream()
                .map(m -> WorkflowSchemeMappingResponse.builder()
                        .id(m.getId())
                        .issueTypeId(m.getIssueTypeId())
                        .workflowId(m.getWorkflow().getId())
                        .workflowName(m.getWorkflow().getName())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        int projectCount = 0;
        if (scheme.getMappings() != null && !scheme.getMappings().isEmpty()) {
            Set<UUID> projectIds = new HashSet<>();
            for (WorkflowSchemeMapping mapping : scheme.getMappings()) {
                Workflow w = mapping.getWorkflow();
                if (w != null && w.getProjectId() != null) {
                    projectIds.add(w.getProjectId());
                }
            }
            projectCount = projectIds.size();
        }

        return WorkflowSchemeResponse.builder()
                .id(scheme.getId())
                .name(scheme.getName())
                .description(scheme.getDescription())
                .isDefault(scheme.getIsDefault())
                .defaultWorkflowId(scheme.getDefaultWorkflowId())
                .isDraft(scheme.getIsDraft())
                .draftOfSchemeId(scheme.getDraftOfSchemeId())
                .isActive(scheme.getIsActive())
                .mappings(mappings)
                .issueTypeCount(mappings.size())
                .projectCount(projectCount)
                .createdAt(scheme.getCreatedAt())
                .updatedAt(scheme.getUpdatedAt())
                .createdBy(scheme.getCreatedBy())
                .updatedBy(scheme.getUpdatedBy())
                .build();
    }
}