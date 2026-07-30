package com.avionics_systems.issue.service;

import com.avionics_systems.issue.dto.IssueResponse;
import com.avionics_systems.issue.dto.RankIssueRequest;
import com.avionics_systems.issue.entity.Issue;
import com.avionics_systems.issue.exception.ResourceNotFoundException;
import com.avionics_systems.issue.event.IssueEventOutboxPublisher;
import com.avionics_systems.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueRankService {

    private final IssueRepository issueRepository;
    private final IssueService issueService;
    private final AuditIntegrationClient auditIntegrationClient;
    private final IssueEventOutboxPublisher issueEventOutboxPublisher;

    @Transactional
    public IssueResponse rankRelative(UUID issueId, RankIssueRequest request, UUID userId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        if (!issue.getProjectId().equals(request.getProjectId())) {
            throw new IllegalArgumentException("Issue does not belong to project " + request.getProjectId());
        }

        List<Issue> ordered = new ArrayList<>(issueRepository.findByProjectIdForRanking(request.getProjectId()));
        ordered.sort(Comparator
                .comparing((Issue i) -> i.getRank() == null)
                .thenComparing(Issue::getRank, Comparator.nullsLast(String::compareTo))
                .thenComparing(Issue::getUpdatedAt, Comparator.nullsLast(java.time.LocalDateTime::compareTo).reversed()));

        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getRank() == null) {
                ordered.get(i).setRank(padRank(i));
            }
        }

        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(issueId)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            throw new ResourceNotFoundException("Issue", "id", issueId);
        }

        String dir = request.getDirection().trim().toUpperCase();
        int swapWith = "UP".equals(dir) ? idx - 1 : "DOWN".equals(dir) ? idx + 1 : -1;
        if (swapWith < 0 || swapWith >= ordered.size()) {
            return issueService.getIssue(issueId);
        }

        Issue a = ordered.get(idx);
        Issue b = ordered.get(swapWith);
        String tmp = a.getRank();
        a.setRank(b.getRank());
        b.setRank(tmp);
        issueRepository.save(a);
        issueRepository.save(b);

        auditIntegrationClient.logIssueEvent(userId, issueId, "ISSUE_RANKED",
                java.util.Map.of("direction", dir, "issueKey", a.getIssueKey(), "newRank", a.getRank()));
        issueEventOutboxPublisher.publish("issue.updated", issueId, request.getProjectId());

        return issueService.getIssue(issueId);
    }

    private static String padRank(int index) {
        return String.format("rank|%09d", index);
    }
}
