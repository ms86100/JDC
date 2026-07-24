package com.jira.issue.service;

import com.jira.issue.entity.ChangeCardMetadata;
import com.jira.issue.entity.DclMetadata;
import com.jira.issue.entity.DeliverableMetadata;
import com.jira.issue.entity.DesignItemMetadata;
import com.jira.issue.repository.ChangeCardMetadataRepository;
import com.jira.issue.repository.DclMetadataRepository;
import com.jira.issue.repository.DeliverableMetadataRepository;
import com.jira.issue.repository.DesignItemMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
