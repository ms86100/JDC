package com.jira.issue.service;

import com.jira.issue.entity.*;
import com.jira.issue.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevInfoService {

    private final DevInfoCommitRepository commitRepository;
    private final DevInfoBranchRepository branchRepository;
    private final DevInfoPullRequestRepository pullRequestRepository;
    private final DevInfoBuildRepository buildRepository;
    private final IssueRepository issueRepository;

    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("([A-Z][A-Z0-9]+-\\d+)");

    @Transactional(readOnly = true)
    public Map<String, Object> getDevInfoForIssue(UUID issueId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("issueId", issueId);
        result.put("commitCount", commitRepository.countByIssueId(issueId));
        result.put("branchCount", branchRepository.countByIssueId(issueId));
        result.put("pullRequestCount", pullRequestRepository.countByIssueId(issueId));
        result.put("buildCount", buildRepository.countByIssueId(issueId));
        result.put("commits", commitRepository.findByIssueIdOrderByCommittedAtDesc(issueId));
        result.put("branches", branchRepository.findByIssueIdOrderByCreatedAtDesc(issueId));
        result.put("pullRequests", pullRequestRepository.findByIssueIdOrderByCreatedAtDesc(issueId));
        result.put("builds", buildRepository.findByIssueIdOrderByCreatedAtDesc(issueId));
        result.put("openPullRequests", pullRequestRepository.findByIssueIdAndStatus(issueId, "OPEN"));
        return result;
    }

    @Transactional
    public List<DevInfoCommit> processCommits(List<Map<String, Object>> commitPayloads) {
        List<DevInfoCommit> saved = new ArrayList<>();
        for (Map<String, Object> payload : commitPayloads) {
            final String message = (String) payload.get("message");
            String rawHash = (String) payload.get("hash");
            if (rawHash == null) rawHash = (String) payload.get("commitHash");
            final String commitHash = rawHash;
            if (commitHash == null) continue;

            Set<String> issueKeys = extractIssueKeys(message);
            String branchName = (String) payload.get("branch");
            if (branchName != null) {
                issueKeys.addAll(extractIssueKeys(branchName));
            }

            for (String issueKey : issueKeys) {
                issueRepository.findByIssueKey(issueKey).ifPresent(issue -> {
                    if (!commitRepository.existsByIssueIdAndCommitHash(issue.getId(), commitHash)) {
                        DevInfoCommit commit = DevInfoCommit.builder()
                                .issueId(issue.getId())
                                .commitHash(commitHash)
                                .message(message)
                                .authorName((String) payload.get("authorName"))
                                .authorEmail((String) payload.getOrDefault("authorEmail",
                                        payload.get("author")))
                                .repository((String) payload.get("repository"))
                                .repositoryUrl((String) payload.get("repositoryUrl"))
                                .url((String) payload.get("url"))
                                .committedAt(parseTimestamp(payload.get("timestamp")))
                                .build();
                        saved.add(commitRepository.save(commit));
                        log.info("Linked commit {} to issue {}", commitHash, issueKey);
                    }
                });
            }
        }
        return saved;
    }

    @Transactional
    public List<DevInfoPullRequest> processPullRequests(List<Map<String, Object>> prPayloads) {
        List<DevInfoPullRequest> saved = new ArrayList<>();
        for (Map<String, Object> payload : prPayloads) {
            String title = (String) payload.get("title");
            String sourceBranch = (String) payload.get("sourceBranch");

            Set<String> issueKeys = extractIssueKeys(title);
            if (sourceBranch != null) {
                issueKeys.addAll(extractIssueKeys(sourceBranch));
            }

            for (String issueKey : issueKeys) {
                issueRepository.findByIssueKey(issueKey).ifPresent(issue -> {
                    DevInfoPullRequest pr = DevInfoPullRequest.builder()
                            .issueId(issue.getId())
                            .prNumber(payload.get("prNumber") instanceof Number ?
                                    ((Number) payload.get("prNumber")).intValue() : null)
                            .title(title)
                            .status((String) payload.getOrDefault("status", "OPEN"))
                            .sourceBranch(sourceBranch)
                            .targetBranch((String) payload.get("targetBranch"))
                            .repository((String) payload.get("repository"))
                            .url((String) payload.get("url"))
                            .authorName((String) payload.get("authorName"))
                            .build();
                    saved.add(pullRequestRepository.save(pr));
                    log.info("Linked PR to issue {}", issueKey);
                });
            }
        }
        return saved;
    }

    @Transactional
    public DevInfoBranch createBranchRecord(UUID issueId, String branchName,
                                             String repository, String url) {
        DevInfoBranch branch = DevInfoBranch.builder()
                .issueId(issueId)
                .branchName(branchName)
                .repository(repository)
                .url(url)
                .createdFromIssue(true)
                .status("ACTIVE")
                .build();
        return branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public String generateBranchName(UUID issueId) {
        return issueRepository.findById(issueId)
                .map(issue -> {
                    String key = issue.getIssueKey().toLowerCase();
                    String title = issue.getTitle()
                            .toLowerCase()
                            .replaceAll("[^a-z0-9]+", "-")
                            .replaceAll("^-|-$", "");
                    if (title.length() > 50) title = title.substring(0, 50);
                    return "feature/" + key + "-" + title;
                })
                .orElse(null);
    }

    private Set<String> extractIssueKeys(String text) {
        Set<String> keys = new LinkedHashSet<>();
        if (text == null) return keys;
        Matcher matcher = ISSUE_KEY_PATTERN.matcher(text);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    private java.time.LocalDateTime parseTimestamp(Object ts) {
        if (ts == null) return null;
        try {
            return java.time.LocalDateTime.parse(ts.toString().replace("Z", ""));
        } catch (Exception e) {
            try {
                return java.time.Instant.parse(ts.toString())
                        .atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
