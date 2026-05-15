package com.jira.plan.service;

import com.jira.plan.dto.response.WarningResponse;
import com.jira.plan.entity.Plan;
import com.jira.plan.entity.PlanWarning;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.PlanRepository;
import com.jira.plan.repository.PlanWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarningService {

    private final PlanWarningRepository warningRepository;
    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<WarningResponse> getWarningsByPlanId(UUID planId) {
        return warningRepository.findByPlanIdAndIsActiveTrue(planId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarningResponse dismissWarning(UUID planId, UUID warningId) {
        PlanWarning warning = findWarningById(warningId);
        warning.setIsActive(false);
        warning.setDismissedAt(LocalDateTime.now());
        warning = warningRepository.save(warning);
        return toResponse(warning);
    }

    @Transactional
    public void generateWarnings(UUID planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            // This would be called by a scheduled job to generate warnings
            // based on dependency cycles, missing target dates, etc.
        });
    }

    private Plan findPlanById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
    }

    private PlanWarning findWarningById(UUID id) {
        return warningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warning", "id", id));
    }

    private WarningResponse toResponse(PlanWarning warning) {
        return WarningResponse.builder()
                .id(warning.getId())
                .planId(warning.getPlanId())
                .issueId(warning.getIssueId())
                .issueKey(warning.getIssueKey())
                .warningType(warning.getWarningType())
                .message(warning.getMessage())
                .severity(warning.getSeverity())
                .isActive(warning.getIsActive())
                .dismissedAt(warning.getDismissedAt())
                .createdAt(warning.getCreatedAt())
                .build();
    }
}
