package com.avionics_systems.issue.service;

import com.avionics_systems.issue.entity.ChangeCardMetadata;
import com.avionics_systems.issue.entity.DclMetadata;
import com.avionics_systems.issue.entity.DeliverableMetadata;
import com.avionics_systems.issue.entity.DesignItemMetadata;
import com.avionics_systems.issue.entity.GroupMetadata;
import com.avionics_systems.issue.entity.IvvCardMetadata;
import com.avionics_systems.issue.entity.ModificationMetadata;
import com.avionics_systems.issue.entity.ReviewSubTaskMetadata;
import com.avionics_systems.issue.entity.SubChangeMetadata;
import com.avionics_systems.issue.entity.SystemStandardMetadata;
import com.avionics_systems.issue.entity.VvmCardMetadata;
import com.avionics_systems.issue.repository.ChangeCardMetadataRepository;
import com.avionics_systems.issue.repository.DclMetadataRepository;
import com.avionics_systems.issue.repository.DeliverableMetadataRepository;
import com.avionics_systems.issue.repository.DesignItemMetadataRepository;
import com.avionics_systems.issue.repository.GroupMetadataRepository;
import com.avionics_systems.issue.repository.IvvCardMetadataRepository;
import com.avionics_systems.issue.repository.ModificationMetadataRepository;
import com.avionics_systems.issue.repository.ReviewSubTaskMetadataRepository;
import com.avionics_systems.issue.repository.SubChangeMetadataRepository;
import com.avionics_systems.issue.repository.SystemStandardMetadataRepository;
import com.avionics_systems.issue.repository.VvmCardMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeManagementService {

    private final ChangeCardMetadataRepository changeCardRepo;
    private final DesignItemMetadataRepository designItemRepo;
    private final DclMetadataRepository dclRepo;
    private final DeliverableMetadataRepository deliverableRepo;
    private final SystemStandardMetadataRepository systemStandardRepo;
    private final ReviewSubTaskMetadataRepository reviewSubTaskRepo;
    private final ModificationMetadataRepository modificationRepo;
    private final VvmCardMetadataRepository vvmCardRepo;
    private final IvvCardMetadataRepository ivvCardRepo;
    private final GroupMetadataRepository groupRepo;
    private final SubChangeMetadataRepository subChangeRepo;

    /**
     * Standard review types created by autoCreateReviewSubTasks.
     * Order follows M1659.2 milestone sequence (excluding INTERNAL_KOM which is
     * handled at project level).
     */
    @Value("${app.change-management.standard-review-types:COMMON_KOM,PLANS_REVIEW,FCR,PDR,DDR,CDR,LAR,FAR,FFR,CR}")
    private String standardReviewTypesStr;

    @Value("${app.change-management.default-review-status:BACKLOG}")
    private String defaultReviewStatus;

    @Value("${app.change-management.follow-up-trigger-status:PASSED_RED}")
    private String followUpTriggerStatus;

    private List<String> getStandardReviewTypes() {
        return Arrays.asList(standardReviewTypesStr.split(","));
    }

    // ========== Change Card CRUD ==========

    @Transactional
    public ChangeCardMetadata createChangeCard(UUID issueId, String changeType,
                                                String classification, UUID parentDesignItemId) {
        if (changeCardRepo.existsByIssueId(issueId)) {
            throw new IllegalStateException("Change card metadata already exists for issue " + issueId);
        }
        ChangeCardMetadata card = ChangeCardMetadata.builder()
                .issueId(issueId)
                .changeType(changeType)
                .classification(classification)
                .parentDesignItemId(parentDesignItemId)
                .build();
        ChangeCardMetadata saved = changeCardRepo.save(card);
        log.info("Created change card metadata for issue {}", issueId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<ChangeCardMetadata> getChangeCardByIssueId(UUID issueId) {
        return changeCardRepo.findByIssueId(issueId);
    }

    @Transactional
    public ChangeCardMetadata updateChangeCard(UUID issueId, String changeType,
                                                String classification, String closureRationale,
                                                UUID resolvedBy) {
        ChangeCardMetadata card = changeCardRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No change card metadata found for issue " + issueId));
        if (changeType != null) {
            card.setChangeType(changeType);
        }
        if (classification != null) {
            card.setClassification(classification);
        }
        if (closureRationale != null) {
            card.setClosureRationale(closureRationale);
        }
        if (resolvedBy != null) {
            card.setResolvedBy(resolvedBy);
        }
        ChangeCardMetadata updated = changeCardRepo.save(card);
        log.info("Updated change card metadata for issue {}", issueId);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<ChangeCardMetadata> getChangeCardsByDesignItem(UUID designItemId) {
        return changeCardRepo.findByParentDesignItemId(designItemId);
    }

    // ========== Design Item CRUD ==========

    @Transactional
    public DesignItemMetadata createDesignItem(UUID issueId, List<String> applicability,
                                                boolean supplierSharing) {
        if (designItemRepo.existsByIssueId(issueId)) {
            throw new IllegalStateException("Design item metadata already exists for issue " + issueId);
        }
        DesignItemMetadata item = DesignItemMetadata.builder()
                .issueId(issueId)
                .applicability(applicability != null ? applicability : List.of())
                .supplierSharing(supplierSharing)
                .build();
        DesignItemMetadata saved = designItemRepo.save(item);
        log.info("Created design item metadata for issue {}", issueId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DesignItemMetadata> getDesignItemByIssueId(UUID issueId) {
        return designItemRepo.findByIssueId(issueId);
    }

    @Transactional
    public DesignItemMetadata updateDesignItem(UUID issueId, List<String> applicability,
                                                boolean supplierSharing,
                                                List<String> sharedSupplierIds) {
        DesignItemMetadata item = designItemRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No design item metadata found for issue " + issueId));
        if (applicability != null) {
            item.setApplicability(applicability);
        }
        item.setSupplierSharing(supplierSharing);
        if (sharedSupplierIds != null) {
            item.setSharedSupplierIds(sharedSupplierIds);
        }
        DesignItemMetadata updated = designItemRepo.save(item);
        log.info("Updated design item metadata for issue {}", issueId);
        return updated;
    }

    // ========== DCL CRUD ==========

    @Transactional
    public DclMetadata createDcl(UUID issueId, String actionResponsible,
                                  String requestedBy, String dclAbstract) {
        if (dclRepo.existsByIssueId(issueId)) {
            throw new IllegalStateException("DCL metadata already exists for issue " + issueId);
        }
        DclMetadata dcl = DclMetadata.builder()
                .issueId(issueId)
                .actionResponsible(actionResponsible)
                .requestedBy(requestedBy)
                .dclAbstract(dclAbstract)
                .build();
        DclMetadata saved = dclRepo.save(dcl);
        log.info("Created DCL metadata for issue {}", issueId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DclMetadata> getDclByIssueId(UUID issueId) {
        return dclRepo.findByIssueId(issueId);
    }

    @Transactional
    public DclMetadata updateDcl(UUID issueId, DclMetadata updates) {
        DclMetadata dcl = dclRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No DCL metadata found for issue " + issueId));
        if (updates.getActionResponsible() != null) {
            dcl.setActionResponsible(updates.getActionResponsible());
        }
        if (updates.getRequestedBy() != null) {
            dcl.setRequestedBy(updates.getRequestedBy());
        }
        if (updates.getDclAbstract() != null) {
            dcl.setDclAbstract(updates.getDclAbstract());
        }
        if (updates.getDescriptionThales() != null) {
            dcl.setDescriptionThales(updates.getDescriptionThales());
        }
        if (updates.getDescriptionHoneywell() != null) {
            dcl.setDescriptionHoneywell(updates.getDescriptionHoneywell());
        }
        if (updates.getSupplierSyncProjectId() != null) {
            dcl.setSupplierSyncProjectId(updates.getSupplierSyncProjectId());
        }
        if (updates.getSupplierSyncIssueId() != null) {
            dcl.setSupplierSyncIssueId(updates.getSupplierSyncIssueId());
        }
        if (updates.getSyncDirection() != null) {
            dcl.setSyncDirection(updates.getSyncDirection());
        }
        if (updates.getLastSyncedAt() != null) {
            dcl.setLastSyncedAt(updates.getLastSyncedAt());
        }
        DclMetadata updated = dclRepo.save(dcl);
        log.info("Updated DCL metadata for issue {}", issueId);
        return updated;
    }

    // ========== Deliverable CRUD ==========

    @Transactional
    public DeliverableMetadata createDeliverable(UUID issueId, String deliverableType,
                                                  String milestoneType) {
        if (deliverableRepo.existsByIssueId(issueId)) {
            throw new IllegalStateException("Deliverable metadata already exists for issue " + issueId);
        }
        DeliverableMetadata deliverable = DeliverableMetadata.builder()
                .issueId(issueId)
                .deliverableType(deliverableType)
                .milestoneType(milestoneType)
                .build();
        DeliverableMetadata saved = deliverableRepo.save(deliverable);
        log.info("Created deliverable metadata for issue {}", issueId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DeliverableMetadata> getDeliverableByIssueId(UUID issueId) {
        return deliverableRepo.findByIssueId(issueId);
    }

    @Transactional
    public DeliverableMetadata updateDeliverable(UUID issueId, DeliverableMetadata updates) {
        DeliverableMetadata deliverable = deliverableRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No deliverable metadata found for issue " + issueId));
        if (updates.getDeliverableType() != null) {
            deliverable.setDeliverableType(updates.getDeliverableType());
        }
        if (updates.getMilestoneType() != null) {
            deliverable.setMilestoneType(updates.getMilestoneType());
        }
        if (updates.getBaselineStartDate() != null) {
            deliverable.setBaselineStartDate(updates.getBaselineStartDate());
        }
        if (updates.getBaselineEndDate() != null) {
            deliverable.setBaselineEndDate(updates.getBaselineEndDate());
        }
        if (updates.getExternalEndDate() != null) {
            deliverable.setExternalEndDate(updates.getExternalEndDate());
        }
        if (updates.getDeliveryDate() != null) {
            deliverable.setDeliveryDate(updates.getDeliveryDate());
        }
        if (updates.getProgramRebaselining() != null) {
            deliverable.setProgramRebaselining(updates.getProgramRebaselining());
        }
        if (updates.getSourceOfDelay() != null) {
            deliverable.setSourceOfDelay(updates.getSourceOfDelay());
        }
        if (updates.getRiskProbability() != null) {
            deliverable.setRiskProbability(updates.getRiskProbability());
        }
        if (updates.getRiskConsequence() != null) {
            deliverable.setRiskConsequence(updates.getRiskConsequence());
        }
        if (updates.getRiskDescription() != null) {
            deliverable.setRiskDescription(updates.getRiskDescription());
        }
        if (updates.getRiskOwner() != null) {
            deliverable.setRiskOwner(updates.getRiskOwner());
        }
        if (updates.getRiskMitigation() != null) {
            deliverable.setRiskMitigation(updates.getRiskMitigation());
        }
        if (updates.getReviewStatus() != null) {
            deliverable.setReviewStatus(updates.getReviewStatus());
        }
        if (updates.getReviewAssignee() != null) {
            deliverable.setReviewAssignee(updates.getReviewAssignee());
        }
        if (updates.getReviewComment() != null) {
            deliverable.setReviewComment(updates.getReviewComment());
        }
        if (updates.getReviewStartDate() != null) {
            deliverable.setReviewStartDate(updates.getReviewStartDate());
        }
        if (updates.getReviewDeadline() != null) {
            deliverable.setReviewDeadline(updates.getReviewDeadline());
        }
        if (updates.getDomainLeader() != null) {
            deliverable.setDomainLeader(updates.getDomainLeader());
        }
        if (updates.getComputer() != null) {
            deliverable.setComputer(updates.getComputer());
        }
        DeliverableMetadata updated = deliverableRepo.save(deliverable);
        log.info("Updated deliverable metadata for issue {}", issueId);
        return updated;
    }

    // ========== System Standard CRUD ==========

    @Transactional
    public SystemStandardMetadata createSystemStandard(UUID issueId, String standardType,
                                                        LocalDate specFreezeDate,
                                                        LocalDate deliveryToLabDate,
                                                        LocalDate requestedLabClearanceDate,
                                                        LocalDate plannedFlightClearanceDate,
                                                        LocalDate targetFlightDate,
                                                        List<String> applicability,
                                                        List<String> componentIds) {
        if (systemStandardRepo.existsByIssueId(issueId)) {
            throw new IllegalStateException("System standard metadata already exists for issue " + issueId);
        }
        SystemStandardMetadata std = SystemStandardMetadata.builder()
                .issueId(issueId)
                .standardType(standardType)
                .specFreezeDate(specFreezeDate)
                .deliveryToLabDate(deliveryToLabDate)
                .requestedLabClearanceDate(requestedLabClearanceDate)
                .plannedFlightClearanceDate(plannedFlightClearanceDate)
                .targetFlightDate(targetFlightDate)
                .applicability(applicability != null ? applicability : List.of())
                .componentIds(componentIds != null ? componentIds : List.of())
                .build();
        SystemStandardMetadata saved = systemStandardRepo.save(std);
        log.info("Created system standard metadata for issue {}", issueId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<SystemStandardMetadata> getSystemStandardByIssueId(UUID issueId) {
        return systemStandardRepo.findByIssueId(issueId);
    }

    @Transactional
    public SystemStandardMetadata updateSystemStandard(UUID issueId, String standardType,
                                                        LocalDate specFreezeDate,
                                                        LocalDate deliveryToLabDate,
                                                        LocalDate requestedLabClearanceDate,
                                                        LocalDate plannedFlightClearanceDate,
                                                        LocalDate targetFlightDate,
                                                        List<String> applicability,
                                                        List<String> componentIds) {
        SystemStandardMetadata std = systemStandardRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No system standard metadata found for issue " + issueId));
        if (standardType != null) {
            std.setStandardType(standardType);
        }
        if (specFreezeDate != null) {
            std.setSpecFreezeDate(specFreezeDate);
        }
        if (deliveryToLabDate != null) {
            std.setDeliveryToLabDate(deliveryToLabDate);
        }
        if (requestedLabClearanceDate != null) {
            std.setRequestedLabClearanceDate(requestedLabClearanceDate);
        }
        if (plannedFlightClearanceDate != null) {
            std.setPlannedFlightClearanceDate(plannedFlightClearanceDate);
        }
        if (targetFlightDate != null) {
            std.setTargetFlightDate(targetFlightDate);
        }
        if (applicability != null) {
            std.setApplicability(applicability);
        }
        if (componentIds != null) {
            std.setComponentIds(componentIds);
        }
        SystemStandardMetadata updated = systemStandardRepo.save(std);
        log.info("Updated system standard metadata for issue {}", issueId);
        return updated;
    }

    // ========== Modification (MOD) CRUD ==========

    @Transactional
    public ModificationMetadata createModification(UUID issueId, String modType,
                                                     String ataChapter, String certificationImpact,
                                                     String modRationale, List<String> affectedDocuments) {
        if (modificationRepo.existsByIssueId(issueId)) {
            throw new IllegalStateException("Modification metadata already exists for issue " + issueId);
        }
        ModificationMetadata mod = ModificationMetadata.builder()
                .issueId(issueId)
                .modType(modType)
                .ataChapter(ataChapter)
                .certificationImpact(certificationImpact)
                .modRationale(modRationale)
                .affectedDocuments(affectedDocuments != null
                        ? affectedDocuments.toArray(new String[0])
                        : null)
                .build();
        ModificationMetadata saved = modificationRepo.save(mod);
        log.info("Created modification metadata for issue {} (type={})", issueId, modType);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<ModificationMetadata> getModification(UUID issueId) {
        return modificationRepo.findByIssueId(issueId);
    }

    @Transactional
    public ModificationMetadata updateModification(UUID issueId, String modType,
                                                     String ataChapter, String certificationImpact,
                                                     String modRationale, List<String> affectedDocuments) {
        ModificationMetadata mod = modificationRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No modification metadata found for issue " + issueId));
        if (modType != null) {
            mod.setModType(modType);
        }
        if (ataChapter != null) {
            mod.setAtaChapter(ataChapter);
        }
        if (certificationImpact != null) {
            mod.setCertificationImpact(certificationImpact);
        }
        if (modRationale != null) {
            mod.setModRationale(modRationale);
        }
        if (affectedDocuments != null) {
            mod.setAffectedDocuments(affectedDocuments.toArray(new String[0]));
        }
        ModificationMetadata updated = modificationRepo.save(mod);
        log.info("Updated modification metadata for issue {}", issueId);
        return updated;
    }

    // ========== Review Sub-Task CRUD ==========

    @Transactional
    public ReviewSubTaskMetadata createReviewSubTask(UUID issueId, UUID parentSystemStandardId,
                                                      String reviewType) {
        if (reviewSubTaskRepo.existsByIssueId(issueId)) {
            throw new IllegalStateException("Review sub-task metadata already exists for issue " + issueId);
        }
        ReviewSubTaskMetadata review = ReviewSubTaskMetadata.builder()
                .issueId(issueId)
                .parentSystemStandardId(parentSystemStandardId)
                .reviewType(reviewType)
                .build();
        ReviewSubTaskMetadata saved = reviewSubTaskRepo.save(review);
        log.info("Created review sub-task metadata for issue {} (type={}, parent={})",
                issueId, reviewType, parentSystemStandardId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<ReviewSubTaskMetadata> getReviewSubTaskByIssueId(UUID issueId) {
        return reviewSubTaskRepo.findByIssueId(issueId);
    }

    @Transactional(readOnly = true)
    public List<ReviewSubTaskMetadata> getReviewSubTasksBySystemStandard(UUID parentSystemStandardId) {
        return reviewSubTaskRepo.findByParentSystemStandardId(parentSystemStandardId);
    }

    /**
     * Updates the review status. When status changes to PASSED_RED, a follow-up
     * review sub-task is automatically cloned with the same type and parent, and
     * the original review's followUpReviewId is set to point to the clone.
     */
    @Transactional
    public ReviewSubTaskMetadata updateReviewStatus(UUID issueId, String newStatus,
                                                     LocalDate baselineStartDate,
                                                     LocalDate baselineEndDate) {
        ReviewSubTaskMetadata review = reviewSubTaskRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No review sub-task metadata found for issue " + issueId));
        if (newStatus != null) {
            review.setReviewStatus(newStatus);
        }
        if (baselineStartDate != null) {
            review.setBaselineStartDate(baselineStartDate);
        }
        if (baselineEndDate != null) {
            review.setBaselineEndDate(baselineEndDate);
        }

        // Auto-clone on PASSED_RED
        if (followUpTriggerStatus.equals(newStatus) && review.getFollowUpReviewId() == null) {
            UUID followUpIssueId = UUID.randomUUID();
            ReviewSubTaskMetadata followUp = ReviewSubTaskMetadata.builder()
                    .issueId(followUpIssueId)
                    .parentSystemStandardId(review.getParentSystemStandardId())
                    .reviewType(review.getReviewType())
                    .reviewStatus(defaultReviewStatus)
                    .build();
            ReviewSubTaskMetadata savedFollowUp = reviewSubTaskRepo.save(followUp);
            review.setFollowUpReviewId(savedFollowUp.getIssueId());
            log.info("Auto-cloned follow-up review sub-task {} for PASSED_RED review {}",
                    savedFollowUp.getIssueId(), issueId);
        }

        ReviewSubTaskMetadata updated = reviewSubTaskRepo.save(review);
        log.info("Updated review sub-task status for issue {} to {}", issueId, newStatus);
        return updated;
    }

    /**
     * Automatically creates the 10 standard M1659.2 review sub-tasks for a
     * system standard. Each review gets a generated issue UUID.
     */
    @Transactional
    public List<ReviewSubTaskMetadata> autoCreateReviewSubTasks(UUID systemStandardId) {
        SystemStandardMetadata parent = systemStandardRepo.findById(systemStandardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No system standard found with id " + systemStandardId));
        List<ReviewSubTaskMetadata> reviews = getStandardReviewTypes().stream()
                .map(type -> {
                    UUID reviewIssueId = UUID.randomUUID();
                    return ReviewSubTaskMetadata.builder()
                            .issueId(reviewIssueId)
                            .parentSystemStandardId(parent.getId())
                            .reviewType(type)
                            .reviewStatus(defaultReviewStatus)
                            .build();
                })
                .toList();
        List<ReviewSubTaskMetadata> saved = reviewSubTaskRepo.saveAll(reviews);
        log.info("Auto-created {} review sub-tasks for system standard {} (issue {})",
                saved.size(), systemStandardId, parent.getIssueId());
        return saved;
    }

    // ========== VVM Card CRUD (IFCS) ==========

    @Transactional
    public VvmCardMetadata createVvmCard(UUID issueId, String scope, String ltrReference) {
        VvmCardMetadata card = VvmCardMetadata.builder()
                .issueId(issueId)
                .scope(scope)
                .ltrReference(ltrReference)
                .build();
        return vvmCardRepo.save(card);
    }

    @Transactional(readOnly = true)
    public Optional<VvmCardMetadata> getVvmCard(UUID issueId) {
        return vvmCardRepo.findByIssueId(issueId);
    }

    @Transactional
    public VvmCardMetadata updateVvmCard(UUID issueId, String scope, String pipelineStatus,
                                          String expertReview, String testingReview, String safetyReview) {
        VvmCardMetadata card = vvmCardRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalStateException("VVM Card not found for issue " + issueId));
        if (scope != null) card.setScope(scope);
        if (pipelineStatus != null) card.setPipelineStatus(pipelineStatus);
        if (expertReview != null) card.setExpertReviewStatus(expertReview);
        if (testingReview != null) card.setTestingReviewStatus(testingReview);
        if (safetyReview != null) card.setSafetyReviewStatus(safetyReview);
        return vvmCardRepo.save(card);
    }

    // ========== IVV Card CRUD (IFCS) ==========

    @Transactional
    public IvvCardMetadata createIvvCard(UUID issueId, UUID vvmCardId, String ivvType,
                                          String requirementImpact, String level) {
        IvvCardMetadata card = IvvCardMetadata.builder()
                .issueId(issueId)
                .vvmCardId(vvmCardId)
                .ivvType(ivvType != null ? ivvType : "VALIDATION")
                .requirementImpact(requirementImpact)
                .level(level)
                .build();
        return ivvCardRepo.save(card);
    }

    @Transactional(readOnly = true)
    public Optional<IvvCardMetadata> getIvvCard(UUID issueId) {
        return ivvCardRepo.findByIssueId(issueId);
    }

    @Transactional(readOnly = true)
    public List<IvvCardMetadata> getIvvCardsByVvm(UUID vvmCardId) {
        return ivvCardRepo.findByVvmCardId(vvmCardId);
    }

    @Transactional
    public IvvCardMetadata updateIvvCard(UUID issueId, String testsStatus, String ivvPriority, String evidence) {
        IvvCardMetadata card = ivvCardRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalStateException("IVV Card not found for issue " + issueId));
        if (testsStatus != null) card.setTestsStatus(testsStatus);
        if (ivvPriority != null) card.setIvvPriority(ivvPriority);
        if (evidence != null) card.setEvidence(evidence);
        return ivvCardRepo.save(card);
    }

    // ========== Group CRUD (IFCS) ==========

    @Transactional
    public GroupMetadata createGroup(UUID issueId, String impactedTeam) {
        GroupMetadata group = GroupMetadata.builder()
                .issueId(issueId)
                .impactedTeam(impactedTeam)
                .build();
        return groupRepo.save(group);
    }

    @Transactional(readOnly = true)
    public Optional<GroupMetadata> getGroup(UUID issueId) {
        return groupRepo.findByIssueId(issueId);
    }

    @Transactional
    public GroupMetadata updateGroup(UUID issueId, String impactedTeam) {
        GroupMetadata group = groupRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalStateException("Group not found for issue " + issueId));
        if (impactedTeam != null) group.setImpactedTeam(impactedTeam);
        return groupRepo.save(group);
    }

    // ========== Sub-Change CRUD (IFCS) ==========

    @Transactional
    public SubChangeMetadata createSubChange(UUID issueId, UUID parentChangeCardId, String gitBranch) {
        SubChangeMetadata sc = SubChangeMetadata.builder()
                .issueId(issueId)
                .parentChangeCardId(parentChangeCardId)
                .gitBranch(gitBranch)
                .build();
        return subChangeRepo.save(sc);
    }

    @Transactional(readOnly = true)
    public Optional<SubChangeMetadata> getSubChange(UUID issueId) {
        return subChangeRepo.findByIssueId(issueId);
    }

    @Transactional(readOnly = true)
    public List<SubChangeMetadata> getSubChangesByParent(UUID parentChangeCardId) {
        return subChangeRepo.findByParentChangeCardId(parentChangeCardId);
    }

    @Transactional
    public SubChangeMetadata updateSubChange(UUID issueId, String gitBranch, String prStatus, String prUrl) {
        SubChangeMetadata sc = subChangeRepo.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalStateException("Sub-Change not found for issue " + issueId));
        if (gitBranch != null) sc.setGitBranch(gitBranch);
        if (prStatus != null) sc.setPrStatus(prStatus);
        if (prUrl != null) sc.setPrUrl(prUrl);
        return subChangeRepo.save(sc);
    }
}
