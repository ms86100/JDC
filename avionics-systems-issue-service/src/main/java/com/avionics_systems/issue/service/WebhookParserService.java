package com.avionics_systems.issue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookParserService {

    private final DevInfoService devInfoService;

    @SuppressWarnings("unchecked")
    public int parseGitHubPushEvent(Map<String, Object> payload) {
        List<Map<String, Object>> commits = new ArrayList<>();
        List<Map<String, Object>> rawCommits = (List<Map<String, Object>>) payload.get("commits");
        String repository = payload.get("repository") != null ?
                ((Map<String, Object>) payload.get("repository")).getOrDefault("full_name", "").toString() : "";
        String repoUrl = payload.get("repository") != null ?
                ((Map<String, Object>) payload.get("repository")).getOrDefault("html_url", "").toString() : "";

        if (rawCommits != null) {
            for (Map<String, Object> rc : rawCommits) {
                Map<String, Object> commit = new LinkedHashMap<>();
                commit.put("hash", rc.get("id"));
                commit.put("message", rc.get("message"));
                Map<String, Object> author = (Map<String, Object>) rc.getOrDefault("author", Map.of());
                commit.put("authorName", author.get("name"));
                commit.put("authorEmail", author.get("email"));
                commit.put("repository", repository);
                commit.put("repositoryUrl", repoUrl);
                commit.put("url", rc.get("url"));
                commit.put("timestamp", rc.get("timestamp"));
                commits.add(commit);
            }
        }
        return devInfoService.processCommits(commits).size();
    }

    @SuppressWarnings("unchecked")
    public int parseGitHubPullRequestEvent(Map<String, Object> payload) {
        Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
        if (pr == null) return 0;

        Map<String, Object> head = (Map<String, Object>) pr.getOrDefault("head", Map.of());
        Map<String, Object> base = (Map<String, Object>) pr.getOrDefault("base", Map.of());
        Map<String, Object> user = (Map<String, Object>) pr.getOrDefault("user", Map.of());
        Map<String, Object> repo = (Map<String, Object>) payload.getOrDefault("repository", Map.of());

        String action = payload.getOrDefault("action", "").toString();
        String status = switch (action) {
            case "opened", "reopened" -> "OPEN";
            case "closed" -> pr.getOrDefault("merged", false).equals(true) ? "MERGED" : "DECLINED";
            default -> "OPEN";
        };

        Map<String, Object> prData = new LinkedHashMap<>();
        prData.put("prNumber", pr.get("number"));
        prData.put("title", pr.get("title"));
        prData.put("status", status);
        prData.put("sourceBranch", head.getOrDefault("ref", ""));
        prData.put("targetBranch", base.getOrDefault("ref", ""));
        prData.put("repository", repo.getOrDefault("full_name", ""));
        prData.put("url", pr.get("html_url"));
        prData.put("authorName", user.getOrDefault("login", ""));

        return devInfoService.processPullRequests(List.of(prData)).size();
    }

    @SuppressWarnings("unchecked")
    public int parseGitLabPushEvent(Map<String, Object> payload) {
        List<Map<String, Object>> commits = new ArrayList<>();
        List<Map<String, Object>> rawCommits = (List<Map<String, Object>>) payload.get("commits");
        Map<String, Object> project = (Map<String, Object>) payload.getOrDefault("project", Map.of());
        String repository = project.getOrDefault("path_with_namespace", "").toString();
        String repoUrl = project.getOrDefault("web_url", "").toString();

        if (rawCommits != null) {
            for (Map<String, Object> rc : rawCommits) {
                Map<String, Object> commit = new LinkedHashMap<>();
                commit.put("hash", rc.get("id"));
                commit.put("message", rc.get("message"));
                Map<String, Object> author = (Map<String, Object>) rc.getOrDefault("author", Map.of());
                commit.put("authorName", author.get("name"));
                commit.put("authorEmail", author.get("email"));
                commit.put("repository", repository);
                commit.put("repositoryUrl", repoUrl);
                commit.put("url", rc.get("url"));
                commit.put("timestamp", rc.get("timestamp"));
                commits.add(commit);
            }
        }
        return devInfoService.processCommits(commits).size();
    }

    @SuppressWarnings("unchecked")
    public int parseGitLabMergeRequestEvent(Map<String, Object> payload) {
        Map<String, Object> attrs = (Map<String, Object>) payload.get("object_attributes");
        if (attrs == null) return 0;

        Map<String, Object> project = (Map<String, Object>) payload.getOrDefault("project", Map.of());
        Map<String, Object> user = (Map<String, Object>) payload.getOrDefault("user", Map.of());

        String state = attrs.getOrDefault("state", "opened").toString();
        String status = switch (state) {
            case "merged" -> "MERGED";
            case "closed" -> "DECLINED";
            default -> "OPEN";
        };

        Map<String, Object> prData = new LinkedHashMap<>();
        prData.put("prNumber", attrs.get("iid"));
        prData.put("title", attrs.get("title"));
        prData.put("status", status);
        prData.put("sourceBranch", attrs.get("source_branch"));
        prData.put("targetBranch", attrs.get("target_branch"));
        prData.put("repository", project.getOrDefault("path_with_namespace", ""));
        prData.put("url", attrs.get("url"));
        prData.put("authorName", user.getOrDefault("name", ""));

        return devInfoService.processPullRequests(List.of(prData)).size();
    }

    @SuppressWarnings("unchecked")
    public int parseBitbucketPushEvent(Map<String, Object> payload) {
        List<Map<String, Object>> commits = new ArrayList<>();
        Map<String, Object> repo = (Map<String, Object>) payload.getOrDefault("repository", Map.of());
        String repository = repo.getOrDefault("full_name", "").toString();

        List<Map<String, Object>> changes = List.of();
        Object pushObj = payload.get("push");
        if (pushObj instanceof Map) {
            Object changesObj = ((Map<String, Object>) pushObj).get("changes");
            if (changesObj instanceof List) {
                changes = (List<Map<String, Object>>) changesObj;
            }
        }

        if (changes != null) {
            for (Map<String, Object> change : changes) {
                List<Map<String, Object>> rawCommits = (List<Map<String, Object>>) change.get("commits");
                if (rawCommits != null) {
                    for (Map<String, Object> rc : rawCommits) {
                        Map<String, Object> commit = new LinkedHashMap<>();
                        commit.put("hash", rc.get("hash"));
                        commit.put("message", rc.get("message"));
                        Map<String, Object> author = (Map<String, Object>) rc.getOrDefault("author", Map.of());
                        commit.put("authorName", author.getOrDefault("raw", ""));
                        commit.put("repository", repository);
                        commit.put("url", rc.getOrDefault("links", Map.of()));
                        commit.put("timestamp", rc.get("date"));
                        commits.add(commit);
                    }
                }
            }
        }
        return devInfoService.processCommits(commits).size();
    }

    @SuppressWarnings("unchecked")
    public int parseBitbucketPullRequestEvent(Map<String, Object> payload) {
        Map<String, Object> pr = (Map<String, Object>) payload.get("pullrequest");
        if (pr == null) return 0;

        Map<String, Object> source = (Map<String, Object>) pr.getOrDefault("source", Map.of());
        Map<String, Object> dest = (Map<String, Object>) pr.getOrDefault("destination", Map.of());
        Map<String, Object> author = (Map<String, Object>) pr.getOrDefault("author", Map.of());
        Map<String, Object> repo = (Map<String, Object>) payload.getOrDefault("repository", Map.of());

        String state = pr.getOrDefault("state", "OPEN").toString();
        String status = switch (state.toUpperCase()) {
            case "MERGED" -> "MERGED";
            case "DECLINED" -> "DECLINED";
            default -> "OPEN";
        };

        Map<String, Object> prData = new LinkedHashMap<>();
        prData.put("prNumber", pr.get("id"));
        prData.put("title", pr.get("title"));
        prData.put("status", status);
        Map<String, Object> sourceBranch = (Map<String, Object>) source.getOrDefault("branch", Map.of());
        Map<String, Object> destBranch = (Map<String, Object>) dest.getOrDefault("branch", Map.of());
        prData.put("sourceBranch", sourceBranch.getOrDefault("name", ""));
        prData.put("targetBranch", destBranch.getOrDefault("name", ""));
        prData.put("repository", repo.getOrDefault("full_name", ""));
        prData.put("url", ((Map<String, Object>) pr.getOrDefault("links", Map.of())).getOrDefault("html", Map.of()));
        prData.put("authorName", author.getOrDefault("display_name", ""));

        return devInfoService.processPullRequests(List.of(prData)).size();
    }
}
