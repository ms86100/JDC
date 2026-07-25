package com.jira.plan.service;

import com.jira.plan.dto.request.CreateReleaseRequest;
import com.jira.plan.dto.response.ReleaseResponse;
import com.jira.plan.entity.Plan;
import com.jira.plan.entity.PlanRelease;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.PlanReleaseRepository;
import com.jira.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final PlanReleaseRepository releaseRepository;
    private final PlanRepository planRepository;

    @Value("${app.release.status.draft:DRAFT}")
    private String releaseStatusDraft;

    @Value("${app.release.status.approved:APPROVED}")
    private String releaseStatusApproved;

    @Value("${app.release.status.released:RELEASED}")
    private String releaseStatusReleased;

    @Transactional(readOnly = true)
    public List<ReleaseResponse> getReleasesByPlanId(UUID planId) {
        return releaseRepository.findByPlanIdAndIsActiveTrueOrderByReleaseDateDesc(planId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReleaseResponse getReleaseById(UUID planId, UUID releaseId) {
        PlanRelease release = findReleaseById(releaseId);
        // IDOR check: verify release belongs to specified plan
        if (!release.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Release", "id", releaseId);
        }
        return toResponse(release);
    }

    @Transactional
    public ReleaseResponse createRelease(UUID planId, CreateReleaseRequest request) {
        Plan plan = findPlanById(planId);

        PlanRelease release = PlanRelease.builder()
                .planId(planId)
                .name(request.getName())
                .version(request.getVersion())
                .description(request.getDescription())
                .releaseDate(request.getReleaseDate())
                .status(releaseStatusDraft)
                .build();

        release = releaseRepository.save(release);
        return toResponse(release);
    }

    @Transactional
    public ReleaseResponse updateRelease(UUID planId, UUID releaseId, CreateReleaseRequest request) {
        PlanRelease release = findReleaseById(releaseId);
        // IDOR check: verify release belongs to specified plan
        if (!release.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Release", "id", releaseId);
        }

        if (request.getName() != null) {
            release.setName(request.getName());
        }
        if (request.getVersion() != null) {
            release.setVersion(request.getVersion());
        }
        if (request.getDescription() != null) {
            release.setDescription(request.getDescription());
        }
        if (request.getReleaseDate() != null) {
            release.setReleaseDate(request.getReleaseDate());
        }

        release = releaseRepository.save(release);
        return toResponse(release);
    }

    @Transactional
    public ReleaseResponse approveRelease(UUID planId, UUID releaseId, UUID approvedBy) {
        PlanRelease release = findReleaseById(releaseId);
        // IDOR check: verify release belongs to specified plan
        if (!release.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Release", "id", releaseId);
        }
        release.setStatus(releaseStatusApproved);
        release.setApprovedBy(approvedBy);
        release.setApprovedAt(LocalDateTime.now());
        release = releaseRepository.save(release);
        return toResponse(release);
    }

    @Transactional
    public ReleaseResponse releaseVersion(UUID planId, UUID releaseId) {
        PlanRelease release = findReleaseById(releaseId);
        // IDOR check: verify release belongs to specified plan
        if (!release.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Release", "id", releaseId);
        }
        release.setStatus(releaseStatusReleased);
        release = releaseRepository.save(release);
        return toResponse(release);
    }

    @Transactional
    public void deleteRelease(UUID planId, UUID releaseId) {
        PlanRelease release = findReleaseById(releaseId);
        // IDOR check: verify release belongs to specified plan
        if (!release.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Release", "id", releaseId);
        }
        // Soft delete to maintain referential integrity
        release.setIsActive(false);
        releaseRepository.save(release);
    }

    private Plan findPlanById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
    }

    private PlanRelease findReleaseById(UUID id) {
        return releaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release", "id", id));
    }

    private ReleaseResponse toResponse(PlanRelease release) {
        return ReleaseResponse.builder()
                .id(release.getId())
                .planId(release.getPlanId())
                .name(release.getName())
                .version(release.getVersion())
                .description(release.getDescription())
                .releaseDate(release.getReleaseDate())
                .status(release.getStatus())
                .approvedBy(release.getApprovedBy())
                .approvedAt(release.getApprovedAt())
                .createdAt(release.getCreatedAt())
                .updatedAt(release.getUpdatedAt())
                .isActive(release.getIsActive())
                .build();
    }
}
