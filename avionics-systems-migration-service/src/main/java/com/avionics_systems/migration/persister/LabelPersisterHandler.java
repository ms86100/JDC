package com.avionics_systems.migration.persister;

import com.avionics_systems.migration.service.clients.IssueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Label Persister Handler — associates labels via issue-service API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LabelPersisterHandler {

    @Value("${app.label.max-length:100}")
    private int maxLabelLength;

    private final IssueServiceClient issueServiceClient;

    @Transactional(rollbackFor = Exception.class)
    public void persistLabelsForIssue(String issueKey, List<String> labels, UUID jobId) {
        if (labels == null || labels.isEmpty() || issueKey == null) {
            return;
        }

        String issueId = issueServiceClient.getIssueByKey(issueKey)
                .map(i -> i.getId())
                .orElse(null);
        if (issueId == null) {
            log.debug("Cannot attach labels — issue not found for key {}", issueKey);
            return;
        }

        for (String label : labels) {
            if (label == null || label.isBlank()) {
                continue;
            }
            String normalized = normalizeLabel(label);
            try {
                issueServiceClient.addIssueLabel(issueId, normalized);
            } catch (Exception e) {
                log.warn("Failed to add label {} to {}: {}", normalized, issueKey, e.getMessage());
            }
        }
        log.debug("Persisted {} labels for issue {}", labels.size(), issueKey);
    }

    private String normalizeLabel(String label) {
        return label.trim();
    }

    public boolean isValidLabel(String label) {
        return label != null && !label.isBlank() && label.length() <= maxLabelLength;
    }
}
