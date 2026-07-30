package com.avionics_systems.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.EvidenceMetadata;
import com.avionics_systems.test.entity.EvidenceRecord;
import com.avionics_systems.test.entity.EvidenceRetentionPolicy;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.repository.EvidenceMetadataRepository;
import com.avionics_systems.test.repository.EvidenceRecordRepository;
import com.avionics_systems.test.repository.EvidenceRetentionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceManagementService {

    private final EvidenceRecordRepository evidenceRepository;
    private final EvidenceMetadataRepository metadataRepository;
    private final EvidenceRetentionPolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    // Supported evidence types
    public static final Set<String> SUPPORTED_TYPES = Set.of(
            "SCREENSHOT", "VIDEO", "LOG", "HAR", "PDF", "FILE", "COMMENT"
    );

    // MIME type mappings for evidence types
    private static final Map<String, List<String>> MIME_TYPE_MAP = Map.of(
            "SCREENSHOT", List.of("image/png", "image/jpeg", "image/gif", "image/webp"),
            "VIDEO", List.of("video/mp4", "video/webm", "video/quicktime"),
            "LOG", List.of("text/plain", "text/log", "application/json"),
            "HAR", List.of("application/json", "application/har+json"),
            "PDF", List.of("application/pdf"),
            "FILE", List.of("application/octet-stream")
    );

    // Metadata extraction patterns
    private static final Map<String, Pattern> EXTRACTION_PATTERNS = Map.of(
            "timestamp", Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2})"),
            "error_code", Pattern.compile("(?:error|code|status)[:\\s]+([A-Z0-9_]+)", Pattern.CASE_INSENSITIVE),
            "ip_address", Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"),
            "url", Pattern.compile("https?://[^\\s]+"),
            "duration", Pattern.compile("(?:duration|took|elapsed)[:\\s]+(\\d+(?:\\.\\d+)?\\s*(?:ms|s|m))", Pattern.CASE_INSENSITIVE)
    );

    // ==================== Enhanced Evidence Upload ====================

    @Transactional
    public EvidenceResponse uploadEvidence(EvidenceUploadRequest request) {
        log.info("Uploading evidence for execution: {}", request.getExecutionId());

        // Determine evidence type if not provided
        String evidenceType = request.getEvidenceType();
        if (evidenceType == null || evidenceType.isEmpty()) {
            evidenceType = inferEvidenceType(request.getFileName(), request.getMimeType());
        }

        EvidenceRecord record = EvidenceRecord.builder()
                .executionId(request.getExecutionId())
                .stepResultId(request.getStepResultId())
                .evidenceType(evidenceType)
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

        // Auto-extract metadata
        Map<String, String> extractedMetadata = extractMetadata(request.getContent(), request.getFileName(), evidenceType);
        request.getMetadata().forEach(extractedMetadata::put);

        // Save metadata entries
        if (!extractedMetadata.isEmpty()) {
            final UUID savedRecordId = record.getId();
            extractedMetadata.forEach((key, value) -> {
                EvidenceMetadata metadata = EvidenceMetadata.builder()
                        .evidenceId(savedRecordId)
                        .metadataKey(key)
                        .metadataValue(value)
                        .metadataType(determineMetadataType(value))
                        .build();
                metadataRepository.save(metadata);
            });
        }

        // Initialize chain of custody
        initializeChainOfCustody(record);

        log.info("Evidence uploaded: {} for execution {}", record.getId(), request.getExecutionId());
        return mapToEvidenceResponse(record);
    }

    private String inferEvidenceType(String fileName, String mimeType) {
        if (fileName == null) return "FILE";

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".gif") || lowerName.endsWith(".webp")) {
            return "SCREENSHOT";
        }
        if (lowerName.endsWith(".mp4") || lowerName.endsWith(".webm") || lowerName.endsWith(".mov")) {
            return "VIDEO";
        }
        if (lowerName.endsWith(".log") || lowerName.endsWith(".txt")) {
            return "LOG";
        }
        if (lowerName.endsWith(".har")) {
            return "HAR";
        }
        if (lowerName.endsWith(".pdf")) {
            return "PDF";
        }

        return "FILE";
    }

    private Map<String, String> extractMetadata(String content, String fileName, String evidenceType) {
        Map<String, String> extracted = new HashMap<>();

        if (content == null) return extracted;

        // Extract based on evidence type
        switch (evidenceType) {
            case "SCREENSHOT":
                extracted.put("capturedAt", LocalDateTime.now().toString());
                extracted.put("format", fileName != null && fileName.contains(".") ?
                        fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase() : "PNG");
                break;

            case "VIDEO":
                extracted.put("recordedAt", LocalDateTime.now().toString());
                extracted.put("format", "MP4");
                break;

            case "LOG":
                // Extract timestamps
                Matcher timestampMatcher = EXTRACTION_PATTERNS.get("timestamp").matcher(content);
                List<String> timestamps = new ArrayList<>();
                while (timestampMatcher.find() && timestamps.size() < 10) {
                    timestamps.add(timestampMatcher.group(1));
                }
                if (!timestamps.isEmpty()) {
                    extracted.put("firstTimestamp", timestamps.get(0));
                    extracted.put("lastTimestamp", timestamps.get(timestamps.size() - 1));
                }

                // Extract error codes
                Matcher errorMatcher = EXTRACTION_PATTERNS.get("error_code").matcher(content);
                Set<String> errors = new HashSet<>();
                while (errorMatcher.find() && errors.size() < 5) {
                    errors.add(errorMatcher.group(1));
                }
                if (!errors.isEmpty()) {
                    extracted.put("errorCodes", String.join(",", errors));
                }

                // Extract duration
                Matcher durationMatcher = EXTRACTION_PATTERNS.get("duration").matcher(content);
                if (durationMatcher.find()) {
                    extracted.put("executionDuration", durationMatcher.group(1));
                }
                break;

            case "HAR":
                try {
                    Map<String, Object> harData = objectMapper.readValue(content, Map.class);
                    extracted.put("harVersion", String.valueOf(harData.getOrDefault("log.version", "unknown")));
                    extracted.put("entryCount", String.valueOf(
                            ((List<?>) harData.getOrDefault("log.entries", List.of())).size()));
                } catch (Exception e) {
                    log.debug("Failed to parse HAR file: {}", e.getMessage());
                }
                break;
        }

        // Universal extractions
        Matcher urlMatcher = EXTRACTION_PATTERNS.get("url").matcher(content);
        if (urlMatcher.find()) {
            extracted.put("firstUrl", urlMatcher.group());
        }

        return extracted;
    }

    private void initializeChainOfCustody(EvidenceRecord record) {
        // Initialize custody tracking in metadata
        EvidenceMetadata custody = EvidenceMetadata.builder()
                .evidenceId(record.getId())
                .metadataKey("chain_of_custody")
                .metadataValue(serializeMap(Map.of(
                        "created_at", LocalDateTime.now().toString(),
                        "created_by", record.getCreatedBy() != null ? record.getCreatedBy() : "system",
                        "created_action", "upload",
                        "integrity_hash", generateIntegrityHash(record)
                )))
                .metadataType("OBJECT")
                .build();
        metadataRepository.save(custody);

        // Add integrity hash to record
        record.setFilePath(generateIntegrityHash(record));
        evidenceRepository.save(record);
    }

    private String generateIntegrityHash(EvidenceRecord record) {
        // Simplified integrity hash - in production use SHA-256
        String data = String.join("|",
                record.getId().toString(),
                record.getFileName() != null ? record.getFileName() : "",
                record.getFileSize() != null ? record.getFileSize().toString() : "0",
                record.getCreatedAt() != null ? record.getCreatedAt().toString() : ""
        );
        return Integer.toHexString(data.hashCode());
    }

    // ==================== Chain of Custody Tracking ====================

    @Transactional
    public ChainOfCustodyRecord addCustodyEvent(UUID evidenceId, String action, String performedBy, String notes) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));

        ChainOfCustodyEvent event = ChainOfCustodyEvent.builder()
                .action(action)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .notes(notes)
                .integrityHash(generateIntegrityHash(record))
                .build();

        // Get existing custody chain
        EvidenceMetadata custody = metadataRepository.findByEvidenceIdAndMetadataKey(evidenceId, "chain_of_custody")
                .stream().findFirst().orElse(null);

        List<ChainOfCustodyEvent> events = new ArrayList<>();
        if (custody != null && custody.getMetadataValue() != null) {
            try {
                Map<String, Object> existing = objectMapper.readValue(custody.getMetadataValue(), Map.class);
                if (existing.containsKey("events")) {
                    events = objectMapper.convertValue(existing.get("events"),
                            new TypeReference<List<ChainOfCustodyEvent>>() {});
                }
            } catch (Exception e) {
                log.warn("Failed to parse existing custody chain: {}", e.getMessage());
            }
        }

        events.add(event);

        // Update custody record
        Map<String, Object> custodyData = new HashMap<>();
        custodyData.put("created_at", record.getCreatedAt().toString());
        custodyData.put("created_by", record.getCreatedBy());
        custodyData.put("created_action", "upload");
        custodyData.put("events", events);

        EvidenceMetadata newCustody = EvidenceMetadata.builder()
                .evidenceId(evidenceId)
                .metadataKey("chain_of_custody")
                .metadataValue(serializeMap(custodyData))
                .metadataType("OBJECT")
                .build();
        metadataRepository.save(newCustody);

        log.info("Chain of custody event added for evidence {}: {} by {}", evidenceId, action, performedBy);

        return ChainOfCustodyRecord.builder()
                .evidenceId(evidenceId)
                .events(events)
                .totalEvents(events.size())
                .isIntact(verifyChainOfCustody(evidenceId))
                .build();
    }

    @Transactional(readOnly = true)
    public ChainOfCustodyRecord getChainOfCustody(UUID evidenceId) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));

        EvidenceMetadata custody = metadataRepository.findByEvidenceIdAndMetadataKey(evidenceId, "chain_of_custody")
                .stream().findFirst().orElse(null);

        List<ChainOfCustodyEvent> events = new ArrayList<>();
        if (custody != null && custody.getMetadataValue() != null) {
            try {
                Map<String, Object> existing = objectMapper.readValue(custody.getMetadataValue(), Map.class);
                if (existing.containsKey("events")) {
                    events = objectMapper.convertValue(existing.get("events"),
                            new TypeReference<List<ChainOfCustodyEvent>>() {});
                }
            } catch (Exception e) {
                log.warn("Failed to parse custody chain: {}", e.getMessage());
            }
        }

        return ChainOfCustodyRecord.builder()
                .evidenceId(evidenceId)
                .events(events)
                .totalEvents(events.size())
                .isIntact(verifyChainOfCustody(evidenceId))
                .build();
    }

    private boolean verifyChainOfCustody(UUID evidenceId) {
        // Verify integrity hash matches
        EvidenceRecord record = evidenceRepository.findById(evidenceId).orElse(null);
        if (record == null) return false;

        String currentHash = generateIntegrityHash(record);
        return currentHash.equals(record.getFilePath());
    }

    // ==================== Evidence Linking ====================

    @Transactional
    public void linkEvidenceToStep(UUID evidenceId, UUID stepResultId) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));

        record.setStepResultId(stepResultId);
        record.setClassificationLevel("STEP_LEVEL");
        evidenceRepository.save(record);

        // Add custody event
        addCustodyEvent(evidenceId, "linked_to_step", "system", "Evidence linked to test step");

        log.info("Evidence {} linked to step {}", evidenceId, stepResultId);
    }

    @Transactional
    public void linkEvidenceToTestCase(UUID evidenceId, UUID testCaseId) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));

        // Store in metadata
        EvidenceMetadata link = EvidenceMetadata.builder()
                .evidenceId(evidenceId)
                .metadataKey("test_case_link")
                .metadataValue(testCaseId.toString())
                .metadataType("UUID")
                .build();
        metadataRepository.save(link);

        addCustodyEvent(evidenceId, "linked_to_testcase", "system", "Evidence linked to test case: " + testCaseId);

        log.info("Evidence {} linked to test case {}", evidenceId, testCaseId);
    }

    // ==================== Evidence Search & Categorization ====================

    @Transactional(readOnly = true)
    public EvidenceSearchResult searchEvidence(EvidenceSearchRequest request) {
        List<EvidenceRecord> results;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            // Search in filename and metadata
            results = evidenceRepository.findAll().stream()
                    .filter(e -> {
                        if (e.getFileName() != null && e.getFileName().toLowerCase().contains(request.getQuery().toLowerCase())) {
                            return true;
                        }
                        if (e.getMetadata() != null && e.getMetadata().toLowerCase().contains(request.getQuery().toLowerCase())) {
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        } else {
            results = evidenceRepository.findAll();
        }

        // Apply filters
        if (request.getEvidenceType() != null && !request.getEvidenceType().isEmpty()) {
            results = results.stream()
                    .filter(e -> request.getEvidenceType().equals(e.getEvidenceType()))
                    .collect(Collectors.toList());
        }

        if (request.getClassificationLevel() != null && !request.getClassificationLevel().isEmpty()) {
            results = results.stream()
                    .filter(e -> request.getClassificationLevel().equals(e.getClassificationLevel()))
                    .collect(Collectors.toList());
        }

        if (request.getStartDate() != null) {
            results = results.stream()
                    .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(request.getStartDate()))
                    .collect(Collectors.toList());
        }

        if (request.getEndDate() != null) {
            results = results.stream()
                    .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isBefore(request.getEndDate()))
                    .collect(Collectors.toList());
        }

        // Build response
        List<EvidenceResponse> responses = results.stream()
                .map(this::mapToEvidenceResponse)
                .collect(Collectors.toList());

        // Apply pagination
        int total = responses.size();
        int start = request.getPage() * request.getSize();
        int end = Math.min(start + request.getSize(), total);

        List<EvidenceResponse> paged = start < total ?
                responses.subList(start, end) : List.of();

        return EvidenceSearchResult.builder()
                .results(paged)
                .totalCount(total)
                .page(request.getPage())
                .pageSize(request.getSize())
                .totalPages((int) Math.ceil((double) total / request.getSize()))
                .facets(generateSearchFacets(results))
                .build();
    }

    private Map<String, Map<String, Long>> generateSearchFacets(List<EvidenceRecord> records) {
        Map<String, Map<String, Long>> facets = new HashMap<>();

        // Type facet
        Map<String, Long> typeFacet = records.stream()
                .collect(Collectors.groupingBy(e -> e.getEvidenceType() != null ? e.getEvidenceType() : "UNKNOWN", Collectors.counting()));
        facets.put("type", typeFacet);

        // Classification level facet
        Map<String, Long> levelFacet = records.stream()
                .collect(Collectors.groupingBy(e -> e.getClassificationLevel() != null ? e.getClassificationLevel() : "UNKNOWN", Collectors.counting()));
        facets.put("classificationLevel", levelFacet);

        // Created by facet
        Map<String, Long> creatorFacet = records.stream()
                .filter(e -> e.getCreatedBy() != null)
                .collect(Collectors.groupingBy(e -> String.valueOf(e.getCreatedBy()), Collectors.counting()));
        facets.put("createdBy", creatorFacet);

        return facets;
    }

    // ==================== Categorization ====================

    @Transactional
    public EvidenceResponse categorizeEvidence(UUID evidenceId, String category, Map<String, String> tags) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));

        // Add category metadata
        EvidenceMetadata categoryMeta = EvidenceMetadata.builder()
                .evidenceId(evidenceId)
                .metadataKey("category")
                .metadataValue(category)
                .metadataType("STRING")
                .build();
        metadataRepository.save(categoryMeta);

        // Add tags
        if (tags != null && !tags.isEmpty()) {
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                EvidenceMetadata tagMeta = EvidenceMetadata.builder()
                        .evidenceId(evidenceId)
                        .metadataKey("tag:" + tag.getKey())
                        .metadataValue(tag.getValue())
                        .metadataType("TAG")
                        .build();
                metadataRepository.save(tagMeta);
            }
        }

        addCustodyEvent(evidenceId, "categorized", "system", "Evidence categorized as: " + category);

        log.info("Evidence {} categorized as {}", evidenceId, category);
        return mapToEvidenceResponse(record);
    }

    // ==================== Classification Operations ====================

    @Transactional
    public EvidenceResponse classifyEvidence(EvidenceClassificationRequest request) {
        EvidenceRecord record = evidenceRepository.findById(request.getEvidenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", request.getEvidenceId()));

        String oldLevel = record.getClassificationLevel();
        record.setClassificationLevel(request.getClassificationLevel());
        record = evidenceRepository.save(record);

        addCustodyEvent(request.getEvidenceId(), "reclassified", "system",
                "Classification changed from " + oldLevel + " to " + request.getClassificationLevel());

        log.info("Evidence {} classified as {}", request.getEvidenceId(), request.getClassificationLevel());
        return mapToEvidenceResponse(record);
    }

    @Transactional
    public void deleteEvidence(UUID evidenceId) {
        EvidenceRecord record = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", "id", evidenceId));

        // Add custody event before deletion
        addCustodyEvent(evidenceId, "deleted", "system", "Evidence deleted");

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

        addCustodyEvent(evidenceId, "archived", "system", "Evidence archived");

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

        // Get extracted metadata
        List<EvidenceMetadata> metadataList = metadataRepository.findByEvidenceIdOrderByCreatedAtAsc(record.getId());
        Map<String, String> metadata = metadataList.stream()
                .filter(m -> m.getMetadataKey() != null && !m.getMetadataKey().startsWith("chain_of_custody"))
                .collect(Collectors.toMap(EvidenceMetadata::getMetadataKey, EvidenceMetadata::getMetadataValue, (a, b) -> a));

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
                .metadata(metadata)
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

    private String serializeMap(Map<String, ?> map) {
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

    // ==================== DTO Classes ====================

    @lombok.Data
    @lombok.Builder
    public static class ChainOfCustodyEvent {
        private String action;
        private String performedBy;
        private LocalDateTime performedAt;
        private String notes;
        private String integrityHash;
    }

    @lombok.Data
    @lombok.Builder
    public static class ChainOfCustodyRecord {
        private UUID evidenceId;
        private List<ChainOfCustodyEvent> events;
        private int totalEvents;
        private boolean isIntact;
    }
}
