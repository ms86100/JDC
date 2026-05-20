package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.EvidenceMetadata;
import com.jira.test.entity.EvidenceRecord;
import com.jira.test.entity.EvidenceRetentionPolicy;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.repository.EvidenceMetadataRepository;
import com.jira.test.repository.EvidenceRecordRepository;
import com.jira.test.repository.EvidenceRetentionPolicyRepository;
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
public class EvidenceManagementService {

    private final EvidenceRecordRepository evidenceRepository;
    private final EvidenceMetadataRepository metadataRepository;
    private final EvidenceRetentionPolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    // ==================== Core Evidence Operations ====================

    @Transactional
    public EvidenceResponse uploadEvidence(EvidenceUploadRequest request) {
        log.info("Uploading evidence for execution: {}", request.getExecutionId());

        EvidenceRecord record = EvidenceRecord.builder()
                .executionId(request.getExecutionId())
                .stepResultId(request.getStepResultId())
                .evidenceType(request.getEvidenceType())
                .classificationLevel(request.getClassificationLevel() != null ?
                        request.getClassificationLevel() : "RUN_LEVEL")
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .url(request.getUrl())
                .content(request.getContent())
                .metadata(request.getMetadata() != null ? serializeMap(request.getMetadata()) : null)
                .retentionPolicyId(request.getRetentionPolicyId())
                .createdBy(request.getCreatedBy())
                .build();

        record = evidenceRepository.save(record);

        // Save metadata entries if provided
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            request.getMetadata().forEach((key, value) -> {
                EvidenceMetadata metadata = EvidenceMetadata.builder()
                        .evidenceId(record.getId())
                        .metadataKey(key)
                        .metadataValue(value)
                        .metadataType(determineMetadataType(value))
                        .build();
                metadataRepository.save(metadata);
            });
        }

        log.info("Evidence uploaded: {} for execution {}", record.getId(), request.getExecutionId());
        return mapToEvidenceResponse(record);
    }

    @Transactional
    public EvidenceResponse classifyEvidence(EvidenceClassificationRequest request) {
        EvidenceRecord record = evidenceRepository.findById(request.getEvidenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", request.getEvidenceId()));

        record.setClassificationLevel(request.getClassificationLevel());
        record = evidenceRepository.save(record);

        log.info("Evidence {} classified as {}", request.getEvidenceId(), request.getClassificationLevel());
        return mapToEvidenceResponse(record);
    }

    @Transactional
    public void deleteEvidence(UUID evidenceId) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));

        // Delete metadata first
        metadataRepository.deleteByEvidenceId(evidenceId);

        // Delete the record
        evidenceRepository.delete(record);
        log.info("Evidence {} deleted", evidenceId);
    }

    @Transactional
    public void archiveEvidence(UUID evidenceId) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));

        record.setIsArchived(true);
        record.setArchivedAt(LocalDateTime.now());
        evidenceRepository.save(record);

        log.info("Evidence {} archived", evidenceId);
    }

    // ==================== Query Operations ====================

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidenceForExecution(UUID executionId) {
        return evidenceRepository.findByExecutionIdAndIsArchivedFalseOrderByCreatedAtDesc(executionId)
                .stream()
                .map(this::mapToEvidenceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidenceForStep(UUID stepResultId) {
        return evidenceRepository.findByStepResultIdOrderByCreatedAtDesc(stepResultId)
                .stream()
                .map(this::mapToEvidenceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EvidenceResponse getEvidence(UUID evidenceId) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));
        return mapToEvidenceResponse(record);
    }

    @Transactional(readOnly = true)
    public EvidenceViewerData getViewerData(UUID executionId) {
        List<EvidenceRecord> evidences = evidenceRepository.findByExecutionIdAndIsArchivedFalseOrderByCreatedAtDesc(executionId);

        if (evidences.isEmpty()) {
            return EvidenceViewerData.builder()
                    .executionId(executionId)
                    .evidenceGroups(List.of())
                    .totalCount(0)
                    .countByType(Map.of())
                    .countByLevel(Map.of())
                    .build();
        }

        // Group evidence by classification level
        Map<String, List<EvidenceRecord>> byLevel = evidences.stream()
                .collect(Collectors.groupingBy(e -> e.getClassificationLevel() != null ?
                        e.getClassificationLevel() : "RUN_LEVEL"));

        List<EvidenceViewerData.EvidenceGroup> groups = byLevel.entrySet().stream()
                .map(entry -> EvidenceViewerData.EvidenceGroup.builder()
                        .groupKey(entry.getKey())
                        .groupLabel(getLevelLabel(entry.getKey()))
                        .evidenceLevel(entry.getKey())
                        .evidences(entry.getValue().stream()
                                .map(this::mapToEvidenceResponse)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        // Count by type
        Map<String, Long> countByType = evidences.stream()
                .collect(Collectors.groupingBy(EvidenceRecord::getEvidenceType, Collectors.counting()));

        // Count by level
        Map<String, Long> countByLevel = byLevel.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        return EvidenceViewerData.builder()
                .executionId(executionId)
                .evidenceGroups(groups)
                .totalCount(evidences.size())
                .countByType(countByType)
                .countByLevel(countByLevel)
                .build();
    }

    // ==================== Retention Policy Operations ====================

    @Transactional
    public RetentionPolicyResponse createRetentionPolicy(RetentionPolicyRequest request) {
        EvidenceRetentionPolicy policy = EvidenceRetentionPolicy.builder()
                .projectId(request.getProjectId())
                .policyName(request.getPolicyName())
                .description(request.getDescription())
                .evidenceType(request.getEvidenceType())
                .retentionDays(request.getRetentionDays() != null ? request.getRetentionDays() : 365)
                .compressionEnabled(request.getCompressionEnabled() != null ? request.getCompressionEnabled() : false)
                .autoArchive(request.getAutoArchive() != null ? request.getAutoArchive() : true)
                .moveToColdStorage(request.getMoveToColdStorage() != null ? request.getMoveToColdStorage() : false)
                .coldStorageAfterDays(request.getColdStorageAfterDays() != null ? request.getColdStorageAfterDays() : 90)
                .permanentDelete(request.getPermanentDelete() != null ? request.getPermanentDelete() : false)
                .deleteAfterDays(request.getDeleteAfterDays())
                .createdBy(request.getCreatedBy())
                .build();

        policy = policyRepository.save(policy);
        log.info("Created retention policy: {}", policy.getId());
        return mapToPolicyResponse(policy);
    }

    @Transactional(readOnly = true)
    public List<RetentionPolicyResponse> getRetentionPolicies(UUID projectId) {
        if (projectId != null) {
            return policyRepository.findByProjectIdAndIsActiveTrue(projectId).stream()
                    .map(this::mapToPolicyResponse)
                    .collect(Collectors.toList());
        }
        return policyRepository.findByAutoArchiveTrue().stream()
                .map(this::mapToPolicyResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RetentionPolicyResponse getRetentionPolicy(UUID policyId) {
        EvidenceRetentionPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("RetentionPolicy", "id", policyId));
        return mapToPolicyResponse(policy);
    }

    @Transactional
    public void applyRetentionPolicy(UUID policyId) {
        EvidenceRetentionPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("RetentionPolicy", "id", policyId));

        List<EvidenceRecord> evidences;
        if (policy.getEvidenceType() != null) {
            evidences = evidenceRepository.findByEvidenceType(policy.getEvidenceType());
        } else {
            evidences = evidenceRepository.findAll();
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(policy.getRetentionDays());
        int archived = 0;

        for (EvidenceRecord evidence : evidences) {
            if (evidence.getCreatedAt().isBefore(cutoffDate) && !evidence.getIsArchived()) {
                if (policy.getAutoArchive()) {
                    evidence.setIsArchived(true);
                    evidence.setArchivedAt(LocalDateTime.now());
                    evidenceRepository.save(evidence);
                    archived++;
                }
            }
        }

        log.info("Retention policy {} applied, archived {} evidences", policyId, archived);
    }

    // ==================== Helper Methods ====================

    private EvidenceResponse mapToEvidenceResponse(EvidenceRecord record) {
        EvidenceRetentionPolicy policy = null;
        if (record.getRetentionPolicyId() != null) {
            policy = policyRepository.findById(record.getRetentionPolicyId()).orElse(null);
        }

        return EvidenceResponse.builder()
                .id(record.getId())
                .executionId(record.getExecutionId())
                .stepResultId(record.getStepResultId())
                .evidenceType(record.getEvidenceType())
                .classificationLevel(record.getClassificationLevel())
                .fileName(record.getFileName())
                .filePath(record.getFilePath())
                .fileSize(record.getFileSize())
                .mimeType(record.getMimeType())
                .url(record.getUrl())
                .thumbnailUrl(record.getThumbnailUrl())
                .content(record.getContent())
                .metadata(parseMap(record.getMetadata()))
                .retentionPolicyId(record.getRetentionPolicyId())
                .retentionPolicyName(policy != null ? policy.getPolicyName() : null)
                .isArchived(record.getIsArchived())
                .archivedAt(record.getArchivedAt())
                .createdBy(record.getCreatedBy())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private RetentionPolicyResponse mapToPolicyResponse(EvidenceRetentionPolicy policy) {
        return RetentionPolicyResponse.builder()
                .id(policy.getId())
                .projectId(policy.getProjectId())
                .policyName(policy.getPolicyName())
                .description(policy.getDescription())
                .evidenceType(policy.getEvidenceType())
                .retentionDays(policy.getRetentionDays())
                .compressionEnabled(policy.getCompressionEnabled())
                .autoArchive(policy.getAutoArchive())
                .moveToColdStorage(policy.getMoveToColdStorage())
                .coldStorageAfterDays(policy.getColdStorageAfterDays())
                .permanentDelete(policy.getPermanentDelete())
                .deleteAfterDays(policy.getDeleteAfterDays())
                .isActive(policy.getIsActive())
                .createdBy(policy.getCreatedBy())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    private String getLevelLabel(String level) {
        return switch (level) {
            case "STEP_LEVEL" -> "Step-Level Evidence";
            case "RUN_LEVEL" -> "Run-Level Evidence";
            case "ENVIRONMENT_LEVEL" -> "Environment-Level Evidence";
            default -> "Other";
        };
    }

    private String determineMetadataType(String value) {
        if (value == null) return "STRING";
        try {
            Integer.parseInt(value);
            return "NUMBER";
        } catch (NumberFormatException e) {
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return "BOOLEAN";
            }
        }
        return "STRING";
    }

    private String serializeMap(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, String> parseMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
}