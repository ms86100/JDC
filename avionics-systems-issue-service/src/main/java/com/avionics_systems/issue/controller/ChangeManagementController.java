package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.entity.ChangeCardMetadata;
import com.avionics_systems.issue.entity.DclMetadata;
import com.avionics_systems.issue.entity.DeliverableMetadata;
import com.avionics_systems.issue.entity.DesignItemMetadata;
import com.avionics_systems.issue.entity.ModificationMetadata;
import com.avionics_systems.issue.entity.ReviewSubTaskMetadata;
import com.avionics_systems.issue.entity.SystemStandardMetadata;
import com.avionics_systems.issue.service.ChangeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Change Management", description = "Change Card, Design Item, DCL, Deliverable, System Standard, and Review Sub-Task metadata")
public class ChangeManagementController {

    private final ChangeManagementService service;

    // ========== Change Card Endpoints ==========

    @PostMapping("/{issueId}/change-card")
    @Operation(summary = "Create change card metadata for an issue")
    public ResponseEntity<ChangeCardMetadata> createChangeCard(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Change type: ANOMALY or EVOLUTION") @RequestParam String changeType,
            @Parameter(description = "Classification") @RequestParam(required = false) String classification,
            @Parameter(description = "Parent design item ID") @RequestParam(required = false) UUID parentDesignItemId) {
        ChangeCardMetadata card = service.createChangeCard(issueId, changeType, classification, parentDesignItemId);
        return new ResponseEntity<>(card, HttpStatus.CREATED);
    }

    @GetMapping("/{issueId}/change-card")
    @Operation(summary = "Get change card metadata for an issue")
    public ResponseEntity<ChangeCardMetadata> getChangeCard(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return service.getChangeCardByIssueId(issueId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{issueId}/change-card")
    @Operation(summary = "Update change card metadata for an issue")
    public ResponseEntity<ChangeCardMetadata> updateChangeCard(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Change type") @RequestParam(required = false) String changeType,
            @Parameter(description = "Classification") @RequestParam(required = false) String classification,
            @Parameter(description = "Closure rationale") @RequestParam(required = false) String closureRationale,
            @Parameter(description = "Resolved by user ID") @RequestParam(required = false) UUID resolvedBy) {
        ChangeCardMetadata updated = service.updateChangeCard(issueId, changeType, classification,
                closureRationale, resolvedBy);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/design-items/{designItemId}/change-cards")
    @Operation(summary = "Get change cards linked to a design item")
    public ResponseEntity<List<ChangeCardMetadata>> getChangeCardsByDesignItem(
            @Parameter(description = "Design item ID") @PathVariable UUID designItemId) {
        List<ChangeCardMetadata> cards = service.getChangeCardsByDesignItem(designItemId);
        return ResponseEntity.ok(cards);
    }

    // ========== Modification (MOD) Endpoints ==========

    @PostMapping("/{issueId}/modification")
    @Operation(summary = "Create modification metadata for an issue")
    public ResponseEntity<ModificationMetadata> createModification(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Modification type: MAJOR or MINOR") @RequestParam String modType,
            @Parameter(description = "ATA chapter reference") @RequestParam(required = false) String ataChapter,
            @Parameter(description = "Certification impact description") @RequestParam(required = false) String certificationImpact,
            @Parameter(description = "Modification rationale") @RequestParam(required = false) String modRationale,
            @Parameter(description = "Affected document references") @RequestParam(required = false) List<String> affectedDocuments) {
        ModificationMetadata mod = service.createModification(issueId, modType, ataChapter,
                certificationImpact, modRationale, affectedDocuments);
        return new ResponseEntity<>(mod, HttpStatus.CREATED);
    }

    @GetMapping("/{issueId}/modification")
    @Operation(summary = "Get modification metadata for an issue")
    public ResponseEntity<ModificationMetadata> getModification(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return service.getModification(issueId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{issueId}/modification")
    @Operation(summary = "Update modification metadata for an issue")
    public ResponseEntity<ModificationMetadata> updateModification(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Modification type: MAJOR or MINOR") @RequestParam(required = false) String modType,
            @Parameter(description = "ATA chapter reference") @RequestParam(required = false) String ataChapter,
            @Parameter(description = "Certification impact description") @RequestParam(required = false) String certificationImpact,
            @Parameter(description = "Modification rationale") @RequestParam(required = false) String modRationale,
            @Parameter(description = "Affected document references") @RequestParam(required = false) List<String> affectedDocuments) {
        ModificationMetadata updated = service.updateModification(issueId, modType, ataChapter,
                certificationImpact, modRationale, affectedDocuments);
        return ResponseEntity.ok(updated);
    }

    // ========== Design Item Endpoints ==========

    @PostMapping("/{issueId}/design-item")
    @Operation(summary = "Create design item metadata for an issue")
    public ResponseEntity<DesignItemMetadata> createDesignItem(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Applicability list") @RequestParam(required = false) List<String> applicability,
            @Parameter(description = "Whether supplier sharing is enabled") @RequestParam(required = false, defaultValue = "false") boolean supplierSharing) {
        DesignItemMetadata item = service.createDesignItem(issueId, applicability, supplierSharing);
        return new ResponseEntity<>(item, HttpStatus.CREATED);
    }

    @GetMapping("/{issueId}/design-item")
    @Operation(summary = "Get design item metadata for an issue")
    public ResponseEntity<DesignItemMetadata> getDesignItem(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return service.getDesignItemByIssueId(issueId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{issueId}/design-item")
    @Operation(summary = "Update design item metadata for an issue")
    public ResponseEntity<DesignItemMetadata> updateDesignItem(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Applicability list") @RequestParam(required = false) List<String> applicability,
            @Parameter(description = "Whether supplier sharing is enabled") @RequestParam(required = false, defaultValue = "false") boolean supplierSharing,
            @Parameter(description = "Shared supplier IDs") @RequestParam(required = false) List<String> sharedSupplierIds) {
        DesignItemMetadata updated = service.updateDesignItem(issueId, applicability, supplierSharing, sharedSupplierIds);
        return ResponseEntity.ok(updated);
    }

    // ========== DCL Endpoints ==========

    @PostMapping("/{issueId}/dcl")
    @Operation(summary = "Create DCL metadata for an issue")
    public ResponseEntity<DclMetadata> createDcl(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Action responsible") @RequestParam(required = false) String actionResponsible,
            @Parameter(description = "Requested by") @RequestParam(required = false) String requestedBy,
            @Parameter(description = "DCL abstract") @RequestParam(required = false) String dclAbstract) {
        DclMetadata dcl = service.createDcl(issueId, actionResponsible, requestedBy, dclAbstract);
        return new ResponseEntity<>(dcl, HttpStatus.CREATED);
    }

    @GetMapping("/{issueId}/dcl")
    @Operation(summary = "Get DCL metadata for an issue")
    public ResponseEntity<DclMetadata> getDcl(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return service.getDclByIssueId(issueId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{issueId}/dcl")
    @Operation(summary = "Update DCL metadata for an issue")
    public ResponseEntity<DclMetadata> updateDcl(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestBody DclMetadata updates) {
        DclMetadata updated = service.updateDcl(issueId, updates);
        return ResponseEntity.ok(updated);
    }

    // ========== Deliverable Endpoints ==========

    @PostMapping("/{issueId}/deliverable")
    @Operation(summary = "Create deliverable metadata for an issue")
    public ResponseEntity<DeliverableMetadata> createDeliverable(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Deliverable type: SID, MSID, FRD, FDD, FRD_FDD, ICD") @RequestParam(required = false) String deliverableType,
            @Parameter(description = "Milestone type: EVM, CRITICAL_EVM, OTHER_DELIVERABLE, CRITICAL_DELIVERABLE") @RequestParam(required = false) String milestoneType) {
        DeliverableMetadata deliverable = service.createDeliverable(issueId, deliverableType, milestoneType);
        return new ResponseEntity<>(deliverable, HttpStatus.CREATED);
    }

    @GetMapping("/{issueId}/deliverable")
    @Operation(summary = "Get deliverable metadata for an issue")
    public ResponseEntity<DeliverableMetadata> getDeliverable(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return service.getDeliverableByIssueId(issueId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{issueId}/deliverable")
    @Operation(summary = "Update deliverable metadata for an issue")
    public ResponseEntity<DeliverableMetadata> updateDeliverable(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestBody DeliverableMetadata updates) {
        DeliverableMetadata updated = service.updateDeliverable(issueId, updates);
        return ResponseEntity.ok(updated);
    }

    // ========== System Standard Endpoints ==========

    @PostMapping("/{issueId}/system-standard")
    @Operation(summary = "Create system standard metadata for an issue (M1659.2)")
    public ResponseEntity<SystemStandardMetadata> createSystemStandard(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Standard type: LAB or LAB_AND_FLIGHT") @RequestParam String standardType,
            @Parameter(description = "Spec freeze date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate specFreezeDate,
            @Parameter(description = "Delivery to lab date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryToLabDate,
            @Parameter(description = "Requested lab clearance date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedLabClearanceDate,
            @Parameter(description = "Planned flight clearance date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedFlightClearanceDate,
            @Parameter(description = "Target flight date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetFlightDate,
            @Parameter(description = "Applicability list") @RequestParam(required = false) List<String> applicability,
            @Parameter(description = "Component IDs") @RequestParam(required = false) List<String> componentIds) {
        SystemStandardMetadata std = service.createSystemStandard(issueId, standardType,
                specFreezeDate, deliveryToLabDate, requestedLabClearanceDate,
                plannedFlightClearanceDate, targetFlightDate, applicability, componentIds);
        return new ResponseEntity<>(std, HttpStatus.CREATED);
    }

    @GetMapping("/{issueId}/system-standard")
    @Operation(summary = "Get system standard metadata for an issue")
    public ResponseEntity<SystemStandardMetadata> getSystemStandard(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return service.getSystemStandardByIssueId(issueId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{issueId}/system-standard")
    @Operation(summary = "Update system standard metadata for an issue")
    public ResponseEntity<SystemStandardMetadata> updateSystemStandard(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Standard type: LAB or LAB_AND_FLIGHT") @RequestParam(required = false) String standardType,
            @Parameter(description = "Spec freeze date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate specFreezeDate,
            @Parameter(description = "Delivery to lab date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryToLabDate,
            @Parameter(description = "Requested lab clearance date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedLabClearanceDate,
            @Parameter(description = "Planned flight clearance date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate plannedFlightClearanceDate,
            @Parameter(description = "Target flight date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetFlightDate,
            @Parameter(description = "Applicability list") @RequestParam(required = false) List<String> applicability,
            @Parameter(description = "Component IDs") @RequestParam(required = false) List<String> componentIds) {
        SystemStandardMetadata updated = service.updateSystemStandard(issueId, standardType,
                specFreezeDate, deliveryToLabDate, requestedLabClearanceDate,
                plannedFlightClearanceDate, targetFlightDate, applicability, componentIds);
        return ResponseEntity.ok(updated);
    }

    // ========== Review Sub-Task Endpoints ==========

    @PostMapping("/{issueId}/review-sub-task")
    @Operation(summary = "Create review sub-task metadata for an issue")
    public ResponseEntity<ReviewSubTaskMetadata> createReviewSubTask(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Parent system standard ID") @RequestParam UUID parentSystemStandardId,
            @Parameter(description = "Review type: INTERNAL_KOM, COMMON_KOM, PLANS_REVIEW, FCR, PDR, DDR, CDR, LAR, FAR, FFR, CR") @RequestParam String reviewType) {
        ReviewSubTaskMetadata review = service.createReviewSubTask(issueId, parentSystemStandardId, reviewType);
        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }

    @GetMapping("/{issueId}/review-sub-task")
    @Operation(summary = "Get review sub-task metadata for an issue")
    public ResponseEntity<ReviewSubTaskMetadata> getReviewSubTask(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return service.getReviewSubTaskByIssueId(issueId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{issueId}/review-sub-task")
    @Operation(summary = "Update review sub-task status (auto-clones on PASSED_RED)")
    public ResponseEntity<ReviewSubTaskMetadata> updateReviewSubTask(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "New review status: NOT_REQUIRED, BACKLOG, PLANNED, PASSED_GREEN, PASSED_AMBER, PASSED_RED") @RequestParam(required = false) String reviewStatus,
            @Parameter(description = "Baseline start date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baselineStartDate,
            @Parameter(description = "Baseline end date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baselineEndDate) {
        ReviewSubTaskMetadata updated = service.updateReviewStatus(issueId, reviewStatus,
                baselineStartDate, baselineEndDate);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/system-standards/{systemStandardId}/reviews")
    @Operation(summary = "Get all review sub-tasks for a system standard")
    public ResponseEntity<List<ReviewSubTaskMetadata>> getReviewsBySystemStandard(
            @Parameter(description = "System standard entity ID") @PathVariable UUID systemStandardId) {
        List<ReviewSubTaskMetadata> reviews = service.getReviewSubTasksBySystemStandard(systemStandardId);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/system-standards/{systemStandardId}/auto-create-reviews")
    @Operation(summary = "Auto-create the 10 standard M1659.2 review sub-tasks for a system standard")
    public ResponseEntity<List<ReviewSubTaskMetadata>> autoCreateReviews(
            @Parameter(description = "System standard entity ID") @PathVariable UUID systemStandardId) {
        List<ReviewSubTaskMetadata> reviews = service.autoCreateReviewSubTasks(systemStandardId);
        return new ResponseEntity<>(reviews, HttpStatus.CREATED);
    }
}
