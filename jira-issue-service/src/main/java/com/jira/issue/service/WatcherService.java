package com.jira.issue.service;

import com.jira.issue.dto.WatcherResponse;
import com.jira.issue.entity.Watcher;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.WatcherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing issue watchers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WatcherService {

    private final WatcherRepository watcherRepository;

    @Transactional
    public WatcherResponse addWatcher(UUID issueId, UUID userId) {
        log.info("Adding watcher for issue {} by user {}", issueId, userId);

        // Check if already watching
        if (watcherRepository.existsByIssueIdAndUserId(issueId, userId)) {
            log.info("User {} is already watching issue {}", userId, issueId);
            Watcher existingWatcher = watcherRepository.findByIssueIdAndUserId(issueId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Watcher", "issueId", issueId));
            return mapToResponse(existingWatcher);
        }

        Watcher watcher = Watcher.builder()
                .issueId(issueId)
                .userId(userId)
                .build();

        watcher = watcherRepository.save(watcher);
        log.info("Watcher added: {}", watcher.getId());

        return mapToResponse(watcher);
    }

    @Transactional
    public void removeWatcher(UUID issueId, UUID userId) {
        log.info("Removing watcher for issue {} by user {}", issueId, userId);
        watcherRepository.deleteByIssueIdAndUserId(issueId, userId);
        log.info("Watcher removed");
    }

    public List<WatcherResponse> getWatchersByIssue(UUID issueId) {
        log.info("Getting watchers for issue: {}", issueId);
        return watcherRepository.findByIssueId(issueId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getWatcherCount(UUID issueId) {
        return watcherRepository.countByIssueId(issueId);
    }

    public boolean isWatching(UUID issueId, UUID userId) {
        return watcherRepository.existsByIssueIdAndUserId(issueId, userId);
    }

    public List<WatcherResponse> getWatchedIssuesByUser(UUID userId) {
        log.info("Getting watched issues by user: {}", userId);
        return watcherRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WatcherResponse mapToResponse(Watcher watcher) {
        return WatcherResponse.builder()
                .id(watcher.getId())
                .issueId(watcher.getIssueId())
                .userId(watcher.getUserId())
                .createdAt(watcher.getCreatedAt())
                .build();
    }
}