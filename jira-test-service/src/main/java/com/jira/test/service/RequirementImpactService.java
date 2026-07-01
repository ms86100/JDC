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

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequirementImpactService {

    private final RequirementVersionRepository versionRepository;
    private final RequirementChangeEventRepository changeEventRepository;
    private final CoverageDriftRecordRepository driftRepository;
    private final RequirementLinkRepository requirementLinkRepository;
    private final TestIssueRepository testIssueRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createVersionSnapshot(UUID requirementId, String title, String description,
                                      List<Map<String, String>> acceptanceCriteria, UUID changedBy) {
        int newVersion = versionRepository.findMaxVersionByRequirementId(requirementId).orElse(0) + 1;

        RequirementVersion version = RequirementVersion.builder()
                .requirementId(requirementId)
                .versionNumber(newVersion)
                .version(String.valueOf(newVersion))
                .titleSnapshot(title)
                .descriptionSnapshot(description)
                .acceptanceCriteriaSnapshot(serializeList(acceptanceCriteria))
                .changedBy(changedBy)
                .changeMagnitude(newVersion == 1
                        ? RequirementVersion.ChangeMagnitude.MAJOR
                        : RequirementVersion.ChangeMagnitude.MINOR)
                .build();

        versionRepository.save(version);
        log.info("Created version {} for requirement {}", newVersion, requirementId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVersionHistory(UUID requirementId) {
        return versionRepository.findByRequirementIdOrderByVersionNumberDesc(requirementId).stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("versionNumber", v.getVersionNumber());
                    map.put("titleSnapshot", v.getTitleSnapshot());
                    map.put("changeMagnitude", v.getChangeMagnitude());
                    map.put("changelog", v.getChangelog());
                    map.put("createdAt", v.getCreatedAt());
                    return map;
                }).toList();
    }

    @Transactional
    public Map<String, Object> analyzeChangeImpact(UUID requirementId, Integer fromVersion, Integer toVersion) {
        RequirementVersion from = versionRepository.findByRequirementIdAndVersionNumber(requirementId, fromVersion)
                .orElseThrow(() -> new ResourceNotFoundException("RequirementVersion", "id", requirementId.toString()));
        RequirementVersion to = versionRepository.findByRequirementIdAndVersionNumber(requirementId, toVersion)
                .orElseThrow(() -> new ResourceNotFoundException("RequirementVersion", "id", requirementId.toString()));

        List<Map<String, String>> fieldChanges = detectFieldChanges(from, to);

        List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(requirementId.toString());
        List<Map<String, Object>> affectedTests = links.stream().map(link -> {
            Map<String, Object> test = new HashMap<>();
            test.put("testId", link.getTestId());
            test.put("impactLevel", determineImpactLevel(fieldChanges));
            return test;
        }).toList();

        Map<String, Object> impact = new HashMap<>();
        impact.put("fieldChanges", fieldChanges);
        impact.put("affectedTests", affectedTests);
        impact.put("changeMagnitude", to.getChangeMagnitude());

        RequirementChangeEvent event = RequirementChangeEvent.builder()
                .requirementId(requirementId)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .changeType(RequirementChangeEvent.ChangeType.MODIFIED)
                .fieldChanges(serializeFieldChanges(fieldChanges))
                .affectedTests(serializeList(affectedTests))
                .build();
        changeEventRepository.save(event);

        return impact;
    }

    @Transactional
    public void analyzeCoverageDrift(UUID requirementId) {
        List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(requirementId.toString());

        BigDecimal currentScore = BigDecimal.valueOf(links.size() * 10L);
        UUID projectId = links.stream()
                .map(RequirementLink::getTestId)
                .findFirst()
                .flatMap(testIssueRepository::findById)
                .map(TestIssue::getProjectId)
                .orElse(requirementId);

        CoverageDriftRecord drift = CoverageDriftRecord.builder()
                .requirementId(requirementId)
                .projectId(projectId)
                .currentCoverage(currentScore)
                .driftType(CoverageDriftRecord.DriftType.STABLE)
                .actionRequired(false)
                .build();

        driftRepository.save(drift);
        log.info("Analyzed coverage drift for requirement {}", requirementId);
    }

    private List<Map<String, String>> detectFieldChanges(RequirementVersion from, RequirementVersion to) {
        List<Map<String, String>> changes = new ArrayList<>();

        if (!Objects.equals(from.getTitleSnapshot(), to.getTitleSnapshot())) {
            changes.add(Map.of("field", "title", "old", String.valueOf(from.getTitleSnapshot()), "new", String.valueOf(to.getTitleSnapshot())));
        }
        if (!Objects.equals(from.getDescriptionSnapshot(), to.getDescriptionSnapshot())) {
            changes.add(Map.of("field", "description", "old", "changed", "new", "changed"));
        }

        return changes;
    }

    private String determineImpactLevel(List<Map<String, String>> changes) {
        return changes.size() > 3 ? "HIGH" : changes.size() > 0 ? "MEDIUM" : "LOW";
    }

    private String serializeList(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String serializeFieldChanges(List<Map<String, String>> changes) {
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
