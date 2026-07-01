package com.jira.migration.dc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.entity.DcStagingEntry;
import com.jira.migration.parser.JiraDcXmlParser;
import com.jira.migration.repository.DcStagingEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JiraDcStagingService {

    private final DcStagingEntryRepository stagingRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID stageParsedEntities(UUID jobId, List<JiraDcXmlParser.ParsedEntity> entities, String rawXmlSnippet) {
        UUID batchId = UUID.randomUUID();
        stagingRepository.deleteByJobId(jobId);
        String fileChecksum = sha256(rawXmlSnippet != null ? rawXmlSnippet : entities.toString());

        int order = 0;
        for (JiraDcXmlParser.ParsedEntity entity : entities) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("entityType", entity.getEntityType());
            payload.put("entityKey", entity.getEntityKey());
            payload.put("fields", entity.getFields());
            if (entity.getDependencies() != null) {
                payload.put("dependencies", entity.getDependencies());
            }

            String sourceId = extractSourceId(entity);
            stagingRepository.save(DcStagingEntry.builder()
                    .jobId(jobId)
                    .importBatchId(batchId)
                    .entityType(entity.getEntityType())
                    .sourceId(sourceId)
                    .sourceKey(entity.getEntityKey())
                    .validationState("PENDING")
                    .checksum(fileChecksum)
                    .parsedPayload(payload)
                    .sequenceOrder(order++)
                    .build());
        }
        return batchId;
    }

    public List<DcStagingEntry> loadStaged(UUID jobId) {
        return stagingRepository.findByJobIdOrderBySequenceOrderAsc(jobId);
    }

    public List<JiraDcXmlParser.ParsedEntity> toParsedEntities(List<DcStagingEntry> staged) {
        List<JiraDcXmlParser.ParsedEntity> out = new ArrayList<>();
        for (DcStagingEntry entry : staged) {
            if (!"VALID".equals(entry.getValidationState()) && !"WARN".equals(entry.getValidationState())) {
                if (!"PENDING".equals(entry.getValidationState())) {
                    continue;
                }
            }
            Map<String, Object> payload = entry.getParsedPayload();
            if (payload == null) {
                continue;
            }
            JiraDcXmlParser.ParsedEntity entity = new JiraDcXmlParser.ParsedEntity();
            entity.setEntityType((String) payload.get("entityType"));
            entity.setEntityKey((String) payload.get("entityKey"));
            @SuppressWarnings("unchecked")
            Map<String, String> fields = (Map<String, String>) payload.get("fields");
            entity.setFields(fields != null ? new HashMap<>(fields) : new HashMap<>());
            out.add(entity);
        }
        return out;
    }

    @Transactional
    public void updateValidationState(UUID entryId, String state) {
        stagingRepository.findById(entryId).ifPresent(e -> {
            e.setValidationState(state);
            stagingRepository.save(e);
        });
    }

    @Transactional
    public void markValidationStates(UUID jobId, Map<String, String> entityKeyToState) {
        for (DcStagingEntry entry : stagingRepository.findByJobIdOrderBySequenceOrderAsc(jobId)) {
            String state = entityKeyToState.getOrDefault(entry.getSourceKey(), "VALID");
            entry.setValidationState(state);
            stagingRepository.save(entry);
        }
    }

    public boolean isDuplicateBatch(UUID jobId, UUID batchId, String checksum) {
        return stagingRepository.countByJobIdAndImportBatchIdAndChecksum(jobId, batchId, checksum) > 0;
    }

    public Map<String, Object> summarizeByJob(UUID jobId) {
        List<DcStagingEntry> entries = stagingRepository.findByJobIdOrderBySequenceOrderAsc(jobId);
        Map<String, Long> byType = new LinkedHashMap<>();
        Map<String, Long> byState = new LinkedHashMap<>();
        for (DcStagingEntry e : entries) {
            byType.merge(e.getEntityType(), 1L, Long::sum);
            byState.merge(e.getValidationState(), 1L, Long::sum);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEntries", entries.size());
        summary.put("byEntityType", byType);
        summary.put("byValidationState", byState);
        summary.put("jobId", jobId.toString());
        return summary;
    }

    private static String extractSourceId(JiraDcXmlParser.ParsedEntity entity) {
        Map<String, String> f = entity.getFields();
        if (f == null) {
            return null;
        }
        return firstNonBlank(f, "id", "sourceAttachmentId", "sourceCommentId", "sourceHistoryId");
    }

    private static String firstNonBlank(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            String v = fields.get(key);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
