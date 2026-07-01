package com.jira.sprint.service;

import com.jira.sprint.dto.CreateSprintRequest;
import com.jira.sprint.dto.SprintResponse;
import com.jira.sprint.dto.UpdateSprintRequest;
import com.jira.sprint.entity.Sprint;
import com.jira.sprint.entity.SprintIssue;
import com.jira.sprint.exception.ResourceNotFoundException;
import com.jira.sprint.repository.SprintIssueRepository;
import com.jira.sprint.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintService {

    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;

    @Transactional
    public SprintResponse createSprint(CreateSprintRequest request, UUID createdBy) {
        Sprint sprint = Sprint.builder()
                .name(request.getName())
                .goal(request.getGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Sprint.SprintStatus.PLANNING)
                .projectId(request.getProjectId())
                .createdBy(createdBy)
                .build();

        sprint = sprintRepository.save(sprint);
        log.info("Created sprint {} for project {}", sprint.getId(), sprint.getProjectId());

        return enrichSprintResponse(SprintResponse.from(sprint));
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByProject(UUID projectId) {
        try {
            List<Sprint> sprints;
            if (projectId != null) {
                sprints = sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
            } else {
                sprints = sprintRepository.findAll();
            }
            return sprints.stream()
                    .map(sprint -> enrichSprintResponseSafe(SprintResponse.from(sprint)))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching sprints for project {}: {}", projectId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private SprintResponse enrichSprintResponseSafe(SprintResponse response) {
        try {
            return enrichSprintResponse(response);
        } catch (Exception e) {
            log.warn("Error enriching sprint {}: {}", response.getId(), e.getMessage());
            response.setIssueCount(0);
            response.setCompletedIssueCount(0);
            return response;
        }
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprint(UUID sprintId) {
        Sprint sprint = findSprintById(sprintId);
        return enrichSprintResponse(SprintResponse.from(sprint));
    }

    @Transactional(readOnly = true)
    public SprintResponse getActiveSprint(UUID projectId) {
        return sprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, Sprint.SprintStatus.ACTIVE)
                .stream()
                .findFirst()
                .map(sprint -> enrichSprintResponse(SprintResponse.from(sprint)))
                .orElse(null);
    }

    @Transactional
    public SprintResponse updateSprint(UUID sprintId, UpdateSprintRequest request) {
        Sprint sprint = findSprintById(sprintId);

        if (request.getName() != null) {
            sprint.setName(request.getName());
        }
        if (request.getGoal() != null) {
            sprint.setGoal(request.getGoal());
        }
        if (request.getStartDate() != null) {
            sprint.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            sprint.setEndDate(request.getEndDate());
        }

        sprint = sprintRepository.save(sprint);
        log.info("Updated sprint {}", sprintId);

        return enrichSprintResponse(SprintResponse.from(sprint));
    }

    @Transactional
    public SprintResponse startSprint(UUID sprintId) {
        Sprint sprint = findSprintById(sprintId);

        // Complete any currently active sprints
        List<Sprint> activeSprints = sprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(
                sprint.getProjectId(), Sprint.SprintStatus.ACTIVE);
        activeSprints.forEach(s -> {
            s.setStatus(Sprint.SprintStatus.COMPLETED);
            sprintRepository.save(s);
        });

        sprint.setStatus(Sprint.SprintStatus.ACTIVE);
        if (sprint.getStartDate() == null) {
            sprint.setStartDate(java.time.LocalDate.now());
        }

        sprint = sprintRepository.save(sprint);
        log.info("Started sprint {}", sprintId);

        return enrichSprintResponse(SprintResponse.from(sprint));
    }

    @Transactional
    public SprintResponse completeSprint(UUID sprintId) {
        Sprint sprint = findSprintById(sprintId);
        sprint.setStatus(Sprint.SprintStatus.COMPLETED);
        if (sprint.getEndDate() == null) {
            sprint.setEndDate(java.time.LocalDate.now());
        }

        sprint = sprintRepository.save(sprint);
        log.info("Completed sprint {}", sprintId);

        return enrichSprintResponse(SprintResponse.from(sprint));
    }

    @Transactional
    public void deleteSprint(UUID sprintId) {
        Sprint sprint = findSprintById(sprintId);
        sprintRepository.delete(sprint);
        log.info("Deleted sprint {}", sprintId);
    }

    @Transactional
    public void addIssueToSprint(UUID sprintId, UUID issueId) {
        Sprint sprint = findSprintById(sprintId);
        if (sprint.getStatus() == Sprint.SprintStatus.COMPLETED) {
            throw new IllegalStateException("Cannot add issues to a completed sprint");
        }

        // Check if already in sprint
        if (sprintIssueRepository.findBySprintIdAndIssueId(sprintId, issueId).isPresent()) {
            throw new IllegalArgumentException("Issue already in sprint");
        }

        // Get max order index
        int maxOrder = sprintIssueRepository.findBySprintIdOrderByOrderIndex(sprintId)
                .stream()
                .mapToInt(SprintIssue::getOrderIndex)
                .max()
                .orElse(-1);

        SprintIssue sprintIssue = SprintIssue.builder()
                .sprintId(sprintId)
                .issueId(issueId)
                .orderIndex(maxOrder + 1)
                .build();

        sprintIssueRepository.save(sprintIssue);
        log.info("Added issue {} to sprint {}", issueId, sprintId);
    }

    @Transactional
    public void removeIssueFromSprint(UUID sprintId, UUID issueId) {
        sprintIssueRepository.deleteBySprintIdAndIssueId(sprintId, issueId);
        log.info("Removed issue {} from sprint {}", issueId, sprintId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getSprintIssueIds(UUID sprintId) {
        return sprintIssueRepository.findBySprintIdOrderByOrderIndex(sprintId)
                .stream()
                .map(SprintIssue::getIssueId)
                .collect(Collectors.toList());
    }

    private Sprint findSprintById(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));
    }

    private SprintResponse enrichSprintResponse(SprintResponse response) {
        try {
            int issueCount = sprintIssueRepository.countBySprintId(response.getId());
            response.setIssueCount(issueCount);
            // For completed count, we'd need to check issue statuses
            // This would require calling issue-service, so we'll leave it as 0 for now
            response.setCompletedIssueCount(0);
            return response;
        } catch (Exception e) {
            log.warn("Error counting issues for sprint {}: {}", response.getId(), e.getMessage());
            response.setIssueCount(0);
            response.setCompletedIssueCount(0);
            return response;
        }
    }
}