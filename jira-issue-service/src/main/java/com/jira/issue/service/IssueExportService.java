package com.jira.issue.service;

import com.jira.issue.entity.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueExportService {

    private static final int EXPORT_CAP = 10_000;

    private final JqlSpecificationBuilder jqlSpecificationBuilder;
    private final com.jira.issue.repository.IssueRepository issueRepository;

    @Transactional(readOnly = true)
    public byte[] exportJqlCsv(String jql, UUID userId) {
        JqlSpecificationBuilder.JqlParseResult parsed = jqlSpecificationBuilder.parse(jql, userId);
        Pageable pageable = PageRequest.of(0, EXPORT_CAP, parsed.sort());
        Specification<Issue> spec = parsed.spec();
        Page<Issue> page = issueRepository.findAll(spec, pageable);
        List<Issue> issues = page.getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("Key,Summary,Status,Priority,Type,Assignee,Reporter,Updated\n");
        for (Issue issue : issues) {
            sb.append(csv(issue.getIssueKey())).append(',');
            sb.append(csv(issue.getTitle())).append(',');
            sb.append(csv(issue.getStatus() != null ? issue.getStatus().getName() : "")).append(',');
            sb.append(csv(issue.getPriority() != null ? issue.getPriority().getName() : "")).append(',');
            sb.append(csv(issue.getIssueType() != null ? issue.getIssueType().getName() : "")).append(',');
            sb.append(csv(issue.getAssigneeId() != null ? issue.getAssigneeId().toString() : "")).append(',');
            sb.append(csv(issue.getReporterId() != null ? issue.getReporterId().toString() : "")).append(',');
            sb.append(csv(issue.getUpdatedAt() != null ? issue.getUpdatedAt().toString() : "")).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
