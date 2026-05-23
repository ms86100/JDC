package com.jira.migration.service.field;

import com.jira.migration.service.clients.IssueServiceClient;
import com.jira.migration.service.clients.dto.IssueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class FieldIssueContextResolver {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final IssueServiceClient issueServiceClient;

    public record IssueContext(UUID issueId, String issueKey, UUID projectId, UUID issueTypeId) {}

    public Optional<IssueContext> resolve(String issueIdOrKey) {
        if (issueIdOrKey == null || issueIdOrKey.isBlank()) {
            return Optional.empty();
        }
        String trimmed = issueIdOrKey.trim();
        try {
            IssueResponse issue;
            if (UUID_PATTERN.matcher(trimmed).matches()) {
                issue = issueServiceClient.getIssue(trimmed);
            } else {
                issue = issueServiceClient.getIssueByKey(trimmed).orElse(null);
                if (issue == null) {
                    return Optional.empty();
                }
            }
            if (issue == null || issue.getId() == null) {
                return Optional.empty();
            }
            UUID issueId = UUID.fromString(issue.getId());
            UUID projectId = issue.getProjectId() != null ? UUID.fromString(issue.getProjectId()) : null;
            return Optional.of(new IssueContext(issueId, issue.getKey(), projectId, null));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
