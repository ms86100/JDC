package com.jira.admin.controller;

import com.jira.admin.dto.MasterDataRequest;
import com.jira.admin.dto.MasterDataResponse;
import com.jira.admin.service.PlatformMasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for platform-level master data CRUD.
 * All endpoints follow the pattern: /api/admin/master-data/{type}
 */
@RestController
@RequestMapping("/api/admin/master-data")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Platform master data CRUD (statuses, priorities, issue-types, etc.)")
public class PlatformMasterDataController {

    private final PlatformMasterDataService masterDataService;

    // ==================== Statuses ====================

    @GetMapping("/statuses")
    @Operation(summary = "List all active master statuses")
    public ResponseEntity<List<MasterDataResponse>> listStatuses() {
        return ResponseEntity.ok(masterDataService.getAllStatuses());
    }

    @GetMapping("/statuses/{id}")
    @Operation(summary = "Get a master status by ID")
    public ResponseEntity<MasterDataResponse> getStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getStatus(id));
    }

    @PostMapping("/statuses")
    @Operation(summary = "Create a new master status")
    public ResponseEntity<MasterDataResponse> createStatus(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createStatus(request), HttpStatus.CREATED);
    }

    @PutMapping("/statuses/{id}")
    @Operation(summary = "Update a master status")
    public ResponseEntity<MasterDataResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updateStatus(id, request));
    }

    @DeleteMapping("/statuses/{id}")
    @Operation(summary = "Delete a master status")
    public ResponseEntity<Void> deleteStatus(@PathVariable UUID id) {
        masterDataService.deleteStatus(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Priorities ====================

    @GetMapping("/priorities")
    @Operation(summary = "List all active master priorities")
    public ResponseEntity<List<MasterDataResponse>> listPriorities() {
        return ResponseEntity.ok(masterDataService.getAllPriorities());
    }

    @GetMapping("/priorities/{id}")
    @Operation(summary = "Get a master priority by ID")
    public ResponseEntity<MasterDataResponse> getPriority(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getPriority(id));
    }

    @PostMapping("/priorities")
    @Operation(summary = "Create a new master priority")
    public ResponseEntity<MasterDataResponse> createPriority(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createPriority(request), HttpStatus.CREATED);
    }

    @PutMapping("/priorities/{id}")
    @Operation(summary = "Update a master priority")
    public ResponseEntity<MasterDataResponse> updatePriority(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updatePriority(id, request));
    }

    @DeleteMapping("/priorities/{id}")
    @Operation(summary = "Delete a master priority")
    public ResponseEntity<Void> deletePriority(@PathVariable UUID id) {
        masterDataService.deletePriority(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Issue Types ====================

    @GetMapping("/issue-types")
    @Operation(summary = "List all active master issue types")
    public ResponseEntity<List<MasterDataResponse>> listIssueTypes() {
        return ResponseEntity.ok(masterDataService.getAllIssueTypes());
    }

    @GetMapping("/issue-types/{id}")
    @Operation(summary = "Get a master issue type by ID")
    public ResponseEntity<MasterDataResponse> getIssueType(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getIssueType(id));
    }

    @PostMapping("/issue-types")
    @Operation(summary = "Create a new master issue type")
    public ResponseEntity<MasterDataResponse> createIssueType(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createIssueType(request), HttpStatus.CREATED);
    }

    @PutMapping("/issue-types/{id}")
    @Operation(summary = "Update a master issue type")
    public ResponseEntity<MasterDataResponse> updateIssueType(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updateIssueType(id, request));
    }

    @DeleteMapping("/issue-types/{id}")
    @Operation(summary = "Delete a master issue type")
    public ResponseEntity<Void> deleteIssueType(@PathVariable UUID id) {
        masterDataService.deleteIssueType(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Resolutions ====================

    @GetMapping("/resolutions")
    @Operation(summary = "List all active master resolutions")
    public ResponseEntity<List<MasterDataResponse>> listResolutions() {
        return ResponseEntity.ok(masterDataService.getAllResolutions());
    }

    @GetMapping("/resolutions/{id}")
    @Operation(summary = "Get a master resolution by ID")
    public ResponseEntity<MasterDataResponse> getResolution(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getResolution(id));
    }

    @PostMapping("/resolutions")
    @Operation(summary = "Create a new master resolution")
    public ResponseEntity<MasterDataResponse> createResolution(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createResolution(request), HttpStatus.CREATED);
    }

    @PutMapping("/resolutions/{id}")
    @Operation(summary = "Update a master resolution")
    public ResponseEntity<MasterDataResponse> updateResolution(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updateResolution(id, request));
    }

    @DeleteMapping("/resolutions/{id}")
    @Operation(summary = "Delete a master resolution")
    public ResponseEntity<Void> deleteResolution(@PathVariable UUID id) {
        masterDataService.deleteResolution(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Link Types ====================

    @GetMapping("/link-types")
    @Operation(summary = "List all active master link types")
    public ResponseEntity<List<MasterDataResponse>> listLinkTypes() {
        return ResponseEntity.ok(masterDataService.getAllLinkTypes());
    }

    @GetMapping("/link-types/{id}")
    @Operation(summary = "Get a master link type by ID")
    public ResponseEntity<MasterDataResponse> getLinkType(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getLinkType(id));
    }

    @PostMapping("/link-types")
    @Operation(summary = "Create a new master link type")
    public ResponseEntity<MasterDataResponse> createLinkType(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createLinkType(request), HttpStatus.CREATED);
    }

    @PutMapping("/link-types/{id}")
    @Operation(summary = "Update a master link type")
    public ResponseEntity<MasterDataResponse> updateLinkType(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updateLinkType(id, request));
    }

    @DeleteMapping("/link-types/{id}")
    @Operation(summary = "Delete a master link type")
    public ResponseEntity<Void> deleteLinkType(@PathVariable UUID id) {
        masterDataService.deleteLinkType(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Roles ====================

    @GetMapping("/roles")
    @Operation(summary = "List all active master roles")
    public ResponseEntity<List<MasterDataResponse>> listRoles() {
        return ResponseEntity.ok(masterDataService.getAllRoles());
    }

    @GetMapping("/roles/{id}")
    @Operation(summary = "Get a master role by ID")
    public ResponseEntity<MasterDataResponse> getRole(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getRole(id));
    }

    @PostMapping("/roles")
    @Operation(summary = "Create a new master role")
    public ResponseEntity<MasterDataResponse> createRole(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createRole(request), HttpStatus.CREATED);
    }

    @PutMapping("/roles/{id}")
    @Operation(summary = "Update a master role")
    public ResponseEntity<MasterDataResponse> updateRole(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updateRole(id, request));
    }

    @DeleteMapping("/roles/{id}")
    @Operation(summary = "Delete a master role")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        masterDataService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Permissions ====================

    @GetMapping("/permissions")
    @Operation(summary = "List all active master permissions")
    public ResponseEntity<List<MasterDataResponse>> listPermissions() {
        return ResponseEntity.ok(masterDataService.getAllPermissions());
    }

    @GetMapping("/permissions/{id}")
    @Operation(summary = "Get a master permission by ID")
    public ResponseEntity<MasterDataResponse> getPermission(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getPermission(id));
    }

    @PostMapping("/permissions")
    @Operation(summary = "Create a new master permission")
    public ResponseEntity<MasterDataResponse> createPermission(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createPermission(request), HttpStatus.CREATED);
    }

    @PutMapping("/permissions/{id}")
    @Operation(summary = "Update a master permission")
    public ResponseEntity<MasterDataResponse> updatePermission(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updatePermission(id, request));
    }

    @DeleteMapping("/permissions/{id}")
    @Operation(summary = "Delete a master permission")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        masterDataService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Board Types ====================

    @GetMapping("/board-types")
    @Operation(summary = "List all active master board types")
    public ResponseEntity<List<MasterDataResponse>> listBoardTypes() {
        return ResponseEntity.ok(masterDataService.getAllBoardTypes());
    }

    @GetMapping("/board-types/{id}")
    @Operation(summary = "Get a master board type by ID")
    public ResponseEntity<MasterDataResponse> getBoardType(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getBoardType(id));
    }

    @PostMapping("/board-types")
    @Operation(summary = "Create a new master board type")
    public ResponseEntity<MasterDataResponse> createBoardType(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createBoardType(request), HttpStatus.CREATED);
    }

    @PutMapping("/board-types/{id}")
    @Operation(summary = "Update a master board type")
    public ResponseEntity<MasterDataResponse> updateBoardType(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updateBoardType(id, request));
    }

    @DeleteMapping("/board-types/{id}")
    @Operation(summary = "Delete a master board type")
    public ResponseEntity<Void> deleteBoardType(@PathVariable UUID id) {
        masterDataService.deleteBoardType(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Notification Events ====================

    @GetMapping("/notification-events")
    @Operation(summary = "List all active master notification events")
    public ResponseEntity<List<MasterDataResponse>> listNotificationEvents() {
        return ResponseEntity.ok(masterDataService.getAllNotificationEvents());
    }

    @GetMapping("/notification-events/{id}")
    @Operation(summary = "Get a master notification event by ID")
    public ResponseEntity<MasterDataResponse> getNotificationEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getNotificationEvent(id));
    }

    @PostMapping("/notification-events")
    @Operation(summary = "Create a new master notification event")
    public ResponseEntity<MasterDataResponse> createNotificationEvent(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createNotificationEvent(request), HttpStatus.CREATED);
    }

    @PutMapping("/notification-events/{id}")
    @Operation(summary = "Update a master notification event")
    public ResponseEntity<MasterDataResponse> updateNotificationEvent(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updateNotificationEvent(id, request));
    }

    @DeleteMapping("/notification-events/{id}")
    @Operation(summary = "Delete a master notification event")
    public ResponseEntity<Void> deleteNotificationEvent(@PathVariable UUID id) {
        masterDataService.deleteNotificationEvent(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Quick Filters ====================

    @GetMapping("/quick-filters")
    @Operation(summary = "List all active quick filter presets")
    public ResponseEntity<List<MasterDataResponse>> listQuickFilters() {
        return ResponseEntity.ok(masterDataService.getAllQuickFilters());
    }

    @GetMapping("/quick-filters/{id}")
    @Operation(summary = "Get a quick filter preset by ID")
    public ResponseEntity<MasterDataResponse> getQuickFilter(@PathVariable UUID id) {
        return ResponseEntity.ok(masterDataService.getQuickFilter(id));
    }

    @PostMapping("/quick-filters")
    @Operation(summary = "Create a new quick filter preset")
    public ResponseEntity<MasterDataResponse> createQuickFilter(@Valid @RequestBody MasterDataRequest request) {
        return new ResponseEntity<>(masterDataService.createQuickFilter(request), HttpStatus.CREATED);
    }

    @PutMapping("/quick-filters/{id}")
    @Operation(summary = "Update a quick filter preset")
    public ResponseEntity<MasterDataResponse> updateQuickFilter(@PathVariable UUID id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(masterDataService.updateQuickFilter(id, request));
    }

    @DeleteMapping("/quick-filters/{id}")
    @Operation(summary = "Delete a quick filter preset")
    public ResponseEntity<Void> deleteQuickFilter(@PathVariable UUID id) {
        masterDataService.deleteQuickFilter(id);
        return ResponseEntity.noContent().build();
    }
}
