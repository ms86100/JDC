package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.entity.*;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionDiffService {

    private final VersionDiffCacheRepository diffCacheRepository;
    private final TestVersionRepository testVersionRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final SharedStepVersionRepository sharedStepVersionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> diffTestVersions(UUID testId, Integer v1, Integer v2) {
        // Check cache first
        Optional<VersionDiffCache> cached = diffCacheRepository
                .findByEntityTypeAndEntityIdAndVersionAAndVersionB("test", testId, v1, v2);
        if (cached.isPresent()) {
            return parseDiffData(cached.get().getDiffData());
        }

        List<TestVersion> versions = testVersionRepository.findByTestIdOrderByVersionNumberDesc(testId);
        TestVersion versionA = versions.stream().filter(v -> v.getVersionNumber().equals(v1)).findFirst().orElse(null);
        TestVersion versionB = versions.stream().filter(v -> v.getVersionNumber().equals(v2)).findFirst().orElse(null);

        Map<String, Object> diff = computeDiff(versionA, versionB);

        // Cache result
        cacheDiff("test", testId, v1, v2, diff);

        return diff;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> diffDatasetVersions(UUID datasetId, Integer v1, Integer v2) {
        Optional<VersionDiffCache> cached = diffCacheRepository
                .findByEntityTypeAndEntityIdAndVersionAAndVersionB("dataset", datasetId, v1, v2);
        if (cached.isPresent()) {
            return parseDiffData(cached.get().getDiffData());
        }

        List<Map<String, Object>> diff = new ArrayList<>();
        // Simplified diff logic
        Map<String, Object> result = new HashMap<>();
        result.put("changes", diff);
        result.put("summary", Map.of("added", 0, "removed", 0, "modified", 0));

        cacheDiff("dataset", datasetId, v1, v2, result);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> diffSharedStepVersions(UUID sharedStepId, Integer v1, Integer v2) {
        Optional<VersionDiffCache> cached = diffCacheRepository
                .findByEntityTypeAndEntityIdAndVersionAAndVersionB("shared_step", sharedStepId, v1, v2);
        if (cached.isPresent()) {
            return parseDiffData(cached.get().getDiffData());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("changes", new ArrayList<>());
        result.put("summary", Map.of("added", 0, "removed", 0, "modified", 0));

        cacheDiff("shared_step", sharedStepId, v1, v2, result);
        return result;
    }

    private Map<String, Object> computeDiff(TestVersion v1, TestVersion v2) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> changes = new ArrayList<>();

        // Compare steps
        if (v1 != null && v2 != null) {
            result.put("versionA", Map.of("number", v1.getVersionNumber(), "date", v1.getCreatedAt()));
            result.put("versionB", Map.of("number", v2.getVersionNumber(), "date", v2.getCreatedAt()));
            result.put("changeSummary", v2.getChangeSummary());
        }

        result.put("changes", changes);
        result.put("summary", Map.of("added", 0, "removed", 0, "modified", 0));
        return result;
    }

    private void cacheDiff(String entityType, UUID entityId, Integer v1, Integer v2, Map<String, Object> diff) {
        VersionDiffCache cache = VersionDiffCache.builder()
                .entityType(entityType)
                .entityId(entityId)
                .versionA(v1)
                .versionB(v2)
                .diffData(serializeDiff(diff))
                .build();
        diffCacheRepository.save(cache);
    }

    private Map<String, Object> parseDiffData(String json) {
        try { return objectMapper.readValue(json, Map.class); }
        catch (JsonProcessingException e) { return new HashMap<>(); }
    }

    private String serializeDiff(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "{}"; }
    }
}