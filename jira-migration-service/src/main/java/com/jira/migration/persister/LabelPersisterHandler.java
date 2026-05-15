package com.jira.migration.persister;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Label Persister Handler
 * Handles label management and issue-label associations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LabelPersisterHandler {

    private final Map<String, UUID> labelCache = new HashMap<>();

    @Transactional(rollbackFor = Exception.class)
    public void persistLabelsForIssue(String issueKey, List<String> labels, UUID jobId) {
        if (labels == null || labels.isEmpty()) return;

        for (String label : labels) {
            if (label == null || label.isBlank()) continue;

            String normalizedLabel = normalizeLabel(label);
            persistLabel(normalizedLabel, jobId);
            associateLabelWithIssue(issueKey, normalizedLabel, jobId);
        }

        log.debug("Persisted {} labels for issue {}", labels.size(), issueKey);
    }

    private String normalizeLabel(String label) {
        // Labels: lowercase, no spaces, special chars allowed
        return label.toLowerCase().trim().replaceAll("\\s+", "-");
    }

    private void persistLabel(String label, UUID jobId) {
        if (!labelCache.containsKey(label)) {
            log.debug("Persisting label: {}", label);
            // In production: Persist to labels table
            labelCache.put(label, UUID.randomUUID());
        }
    }

    private void associateLabelWithIssue(String issueKey, String label, UUID jobId) {
        log.debug("Associating label '{}' with issue {}", label, issueKey);
        // In production: Persist to issue_labels table
    }

    /**
     * Validate label format
     */
    public boolean isValidLabel(String label) {
        if (label == null || label.isBlank()) return false;
        if (label.length() > 100) return false;
        // Labels can contain alphanumeric, hyphens, underscores
        return label.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Parse comma-separated labels from CSV
     */
    public List<String> parseLabels(String labelString) {
        if (labelString == null || labelString.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(labelString.split(","))
                .map(String::trim)
                .filter(this::isValidLabel)
                .toList();
    }

    /**
     * Clear label cache (for new job)
     */
    public void clearCache() {
        labelCache.clear();
    }
}