package com.jira.test.service;

import com.jira.test.dto.BaselineSummaryResponse;
import com.jira.test.dto.BulkOperationResponse;
import com.jira.test.entity.VvoDefinition;
import com.jira.test.repository.HlvvoDefinitionRepository;
import com.jira.test.repository.VvoDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VvoBaselineService {

    private final VvoDefinitionRepository vvoRepo;
    private final HlvvoDefinitionRepository hlvvoRepo;

    /**
     * Step 1: Tag VVOs with a baseline (Fix Version).
     * Only VVOs in VERIFIED, RELEASED, or CANCELLED status are eligible.
     */
    @Transactional
    public BulkOperationResponse tagBaseline(UUID projectId, UUID fixVersionId, List<UUID> vvoIds) {
        log.info("Tagging {} VVOs with baseline {} in project {}", vvoIds.size(), fixVersionId, projectId);

        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        List<String> validStatuses = List.of("VERIFIED", "RELEASED", "CANCELLED");

        for (UUID vvoId : vvoIds) {
            try {
                VvoDefinition vvo = vvoRepo.findById(vvoId)
                        .orElseThrow(() -> new RuntimeException("VVO not found: " + vvoId));

                if (!vvo.getProjectId().equals(projectId)) {
                    errors.add("VVO " + vvoId + " does not belong to project " + projectId);
                    failed++;
                    continue;
                }

                if (!validStatuses.contains(vvo.getStatus())) {
                    errors.add("VVO " + vvo.getIssueKey() + " status " + vvo.getStatus()
                            + " not eligible for baselining");
                    failed++;
                    continue;
                }

                vvo.setFixVersionId(fixVersionId);
                vvoRepo.save(vvo);
                success++;
            } catch (Exception e) {
                errors.add("Error processing VVO " + vvoId + ": " + e.getMessage());
                failed++;
            }
        }

        log.info("Baseline tagging complete: {} success, {} failed", success, failed);
        return BulkOperationResponse.builder()
                .successCount(success)
                .failedCount(failed)
                .errors(errors)
                .build();
    }

    /**
     * Step 2: Bulk transition VVOs from VERIFIED to RELEASED for a given baseline.
     */
    @Transactional
    public BulkOperationResponse publishBaseline(UUID projectId, UUID fixVersionId) {
        log.info("Publishing baseline {} for project {}", fixVersionId, projectId);

        List<VvoDefinition> vvos = vvoRepo.findByFixVersionId(fixVersionId);
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (VvoDefinition vvo : vvos) {
            if (!vvo.getProjectId().equals(projectId)) {
                continue;
            }

            if ("VERIFIED".equals(vvo.getStatus())) {
                vvo.setStatus("RELEASED");
                vvoRepo.save(vvo);
                success++;
            } else if ("RELEASED".equals(vvo.getStatus()) || "CANCELLED".equals(vvo.getStatus())) {
                // Already in acceptable terminal state, skip silently
            } else {
                errors.add("VVO " + vvo.getIssueKey() + " cannot be released from status " + vvo.getStatus());
                failed++;
            }
        }

        log.info("Baseline published: {} released, {} failed", success, failed);
        return BulkOperationResponse.builder()
                .successCount(success)
                .failedCount(failed)
                .errors(errors)
                .build();
    }

    /**
     * Get a summary of a baseline showing VVO counts by status.
     */
    @Transactional(readOnly = true)
    public BaselineSummaryResponse getBaselineSummary(UUID projectId, UUID fixVersionId) {
        List<VvoDefinition> vvos = vvoRepo.findByFixVersionId(fixVersionId).stream()
                .filter(v -> v.getProjectId().equals(projectId))
                .toList();

        long released = vvos.stream().filter(v -> "RELEASED".equals(v.getStatus())).count();
        long verified = vvos.stream().filter(v -> "VERIFIED".equals(v.getStatus())).count();
        long cancelled = vvos.stream().filter(v -> "CANCELLED".equals(v.getStatus())).count();
        long superseded = vvos.stream().filter(v -> "SUPERSEDED".equals(v.getStatus())).count();

        return BaselineSummaryResponse.builder()
                .fixVersionId(fixVersionId)
                .projectId(projectId)
                .totalVvos((int) vvos.size())
                .releasedCount((int) released)
                .verifiedCount((int) verified)
                .cancelledCount((int) cancelled)
                .supersededCount((int) superseded)
                .build();
    }

    /**
     * Clone a VVO with version increment and clear HLVVO/DOORS/fixVersion links.
     * The original will be superseded when the clone reaches VERIFIED or RELEASED status.
     */
    @Transactional
    public VvoDefinition cloneWithSupersede(UUID originalId) {
        VvoDefinition original = vvoRepo.findById(originalId)
                .orElseThrow(() -> new RuntimeException("VVO not found: " + originalId));

        int newVersion = (original.getVvoVersion() != null ? original.getVvoVersion() : 1) + 1;
        long sequence = vvoRepo.countByProjectId(original.getProjectId()) + 1;

        VvoDefinition clone = VvoDefinition.builder()
                .projectId(original.getProjectId())
                .issueKey("VVO-" + sequence)
                .summary(original.getSummary())
                .description(original.getDescription())
                .status("NEW")
                .hlvvoId(null)
                .executionResponsible(original.getExecutionResponsible())
                .executionDelegation(original.getExecutionDelegation())
                .vvoUsage(original.getVvoUsage())
                .vvoScope(original.getVvoScope())
                .testMeanTypeRequested(original.getTestMeanTypeRequested())
                .operationalConditions(original.getOperationalConditions())
                .expectedResults(original.getExpectedResults())
                .realSystemNeeded(original.getRealSystemNeeded())
                .applicability(original.getApplicability())
                .supplierApplicability(original.getSupplierApplicability())
                .associatedRequirements(original.getAssociatedRequirements())
                .idDoors(null)
                .vvoVersion(newVersion)
                .cloneSourceId(original.getId())
                .fixVersionId(null)
                .milestoneTarget(original.getMilestoneTarget())
                .specificationReference(original.getSpecificationReference())
                .assigneeId(original.getAssigneeId())
                .storyPoints(original.getStoryPoints())
                .labels(original.getLabels())
                .componentIds(original.getComponentIds())
                .archived(false)
                .build();

        clone = vvoRepo.save(clone);
        log.info("Cloned VVO {} v{} -> {} v{}", original.getIssueKey(), original.getVvoVersion(),
                clone.getIssueKey(), newVersion);

        return clone;
    }

    /**
     * Supersede the original VVO when a clone transitions to VERIFIED or RELEASED.
     * Called as a side-effect of status transitions on cloned VVOs.
     */
    @Transactional
    public void supersedeOriginal(UUID cloneId) {
        VvoDefinition clone = vvoRepo.findById(cloneId)
                .orElseThrow(() -> new RuntimeException("Clone VVO not found: " + cloneId));

        if (clone.getCloneSourceId() == null) {
            return;
        }

        vvoRepo.findById(clone.getCloneSourceId()).ifPresent(original -> {
            if (!"SUPERSEDED".equals(original.getStatus())) {
                original.setStatus("SUPERSEDED");
                vvoRepo.save(original);
                log.info("Superseded original VVO {} (replaced by {} v{})",
                        original.getIssueKey(), clone.getIssueKey(), clone.getVvoVersion());
            }
        });
    }
}
