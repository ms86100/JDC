package com.avionics_systems.admin.controller;

import com.avionics_systems.admin.dto.asset.*;
import com.avionics_systems.admin.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/assets")
@RequiredArgsConstructor
@Tag(name = "Assets & Inventory", description = "Asset type, asset, and asset-issue link management")
public class AssetController {

    private final AssetService assetService;

    // ==================== Asset Types ====================

    @GetMapping("/types")
    @Operation(summary = "List all active asset types")
    public ResponseEntity<List<AssetTypeResponse>> getAllAssetTypes() {
        return ResponseEntity.ok(assetService.getAllAssetTypes());
    }

    @GetMapping("/types/{id}")
    @Operation(summary = "Get asset type by ID")
    public ResponseEntity<AssetTypeResponse> getAssetType(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getAssetTypeById(id));
    }

    @PostMapping("/types")
    @Operation(summary = "Create asset type")
    public ResponseEntity<AssetTypeResponse> createAssetType(
            @Valid @RequestBody CreateAssetTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.createAssetType(request));
    }

    @PutMapping("/types/{id}")
    @Operation(summary = "Update asset type")
    public ResponseEntity<AssetTypeResponse> updateAssetType(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAssetTypeRequest request) {
        return ResponseEntity.ok(assetService.updateAssetType(id, request));
    }

    @DeleteMapping("/types/{id}")
    @Operation(summary = "Deactivate asset type")
    public ResponseEntity<Void> deactivateAssetType(@PathVariable UUID id) {
        assetService.deactivateAssetType(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Assets ====================

    @GetMapping
    @Operation(summary = "List all active assets, optionally filtered by type, status, or location")
    public ResponseEntity<List<AssetResponse>> getAssets(
            @RequestParam(required = false) UUID assetTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location) {
        if (assetTypeId != null) {
            return ResponseEntity.ok(assetService.getAssetsByType(assetTypeId));
        }
        if (status != null) {
            return ResponseEntity.ok(assetService.getAssetsByStatus(status));
        }
        if (location != null) {
            return ResponseEntity.ok(assetService.getAssetsByLocation(location));
        }
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset by ID")
    public ResponseEntity<AssetResponse> getAsset(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @PostMapping
    @Operation(summary = "Create asset")
    public ResponseEntity<AssetResponse> createAsset(
            @Valid @RequestBody CreateAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.createAsset(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update asset")
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAssetRequest request) {
        return ResponseEntity.ok(assetService.updateAsset(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate asset")
    public ResponseEntity<Void> deactivateAsset(@PathVariable UUID id) {
        assetService.deactivateAsset(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Asset-Issue Links ====================

    @PostMapping("/links")
    @Operation(summary = "Link an asset to an issue")
    public ResponseEntity<AssetIssueLinkResponse> linkAssetToIssue(
            @Valid @RequestBody AssetIssueLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.linkAssetToIssue(request));
    }

    @DeleteMapping("/links/{linkId}")
    @Operation(summary = "Remove an asset-issue link")
    public ResponseEntity<Void> unlinkAssetFromIssue(@PathVariable UUID linkId) {
        assetService.unlinkAssetFromIssue(linkId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/linked-issues")
    @Operation(summary = "Get issues linked to an asset")
    public ResponseEntity<List<AssetIssueLinkResponse>> getLinkedIssues(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getLinkedIssues(id));
    }

    @GetMapping("/issues/{issueId}/linked-assets")
    @Operation(summary = "Get assets linked to an issue")
    public ResponseEntity<List<AssetIssueLinkResponse>> getLinkedAssets(@PathVariable UUID issueId) {
        return ResponseEntity.ok(assetService.getLinkedAssets(issueId));
    }
}
