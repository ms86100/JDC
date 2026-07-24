package com.jira.issue.controller;

import com.jira.issue.entity.ChangeCardMetadata;
import com.jira.issue.entity.DclMetadata;
import com.jira.issue.entity.DeliverableMetadata;
import com.jira.issue.entity.DesignItemMetadata;
import com.jira.issue.service.ChangeManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Change Management", description = "Change Card, Design Item, DCL, and Deliverable metadata")
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
}
