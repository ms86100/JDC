package com.jira.issue.service;

import com.jira.issue.dto.VoteResponse;
import com.jira.issue.entity.Vote;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing issue votes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final VoteRepository voteRepository;

    @Transactional
    public VoteResponse addVote(UUID issueId, UUID userId) {
        log.info("Adding vote for issue {} by user {}", issueId, userId);

        // Check if already voted
        if (voteRepository.existsByIssueIdAndUserId(issueId, userId)) {
            log.info("User {} already voted for issue {}", userId, issueId);
            Vote existingVote = voteRepository.findByIssueIdAndUserId(issueId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vote", "issueId", issueId));
            return mapToResponse(existingVote);
        }

        Vote vote = Vote.builder()
                .issueId(issueId)
                .userId(userId)
                .build();

        vote = voteRepository.save(vote);
        log.info("Vote added: {}", vote.getId());

        return mapToResponse(vote);
    }

    @Transactional
    public void removeVote(UUID issueId, UUID userId) {
        log.info("Removing vote for issue {} by user {}", issueId, userId);
        voteRepository.deleteByIssueIdAndUserId(issueId, userId);
        log.info("Vote removed");
    }

    public List<VoteResponse> getVotesByIssue(UUID issueId) {
        log.info("Getting votes for issue: {}", issueId);
        return voteRepository.findByIssueId(issueId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long getVoteCount(UUID issueId) {
        return voteRepository.countByIssueId(issueId);
    }

    public boolean hasVoted(UUID issueId, UUID userId) {
        return voteRepository.existsByIssueIdAndUserId(issueId, userId);
    }

    public List<VoteResponse> getVotesByUser(UUID userId) {
        log.info("Getting votes by user: {}", userId);
        return voteRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private VoteResponse mapToResponse(Vote vote) {
        return VoteResponse.builder()
                .id(vote.getId())
                .issueId(vote.getIssueId())
                .userId(vote.getUserId())
                .createdAt(vote.getCreatedAt())
                .build();
    }
}