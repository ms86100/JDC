package com.jira.admin.service;

import com.jira.admin.dto.asset.*;
import com.jira.admin.entity.AssetEntity;
import com.jira.admin.entity.AssetIssueLinkEntity;
import com.jira.admin.entity.AssetTypeEntity;
import com.jira.admin.repository.AssetIssueLinkRepository;
import com.jira.admin.repository.AssetRepository;
import com.jira.admin.repository.AssetTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final AssetTypeRepository assetTypeRepository;
    private final AssetRepository assetRepository;
    private final AssetIssueLinkRepository assetIssueLinkRepository;

    // ==================== Asset Types ====================

    @Transactional(readOnly = true)
    public List<AssetTypeResponse> getAllAssetTypes() {
        return assetTypeRepository.findByIsActiveTrue().stream()
                .map(this::mapAssetTypeToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssetTypeResponse getAssetTypeById(UUID id) {
        AssetTypeEntity entity = assetTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset type not found: " + id));
        return mapAssetTypeToResponse(entity);
    }

    @Transactional
    public AssetTypeResponse createAssetType(CreateAssetTypeRequest request) {
        AssetTypeEntity entity = AssetTypeEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .attributeSchema(request.getAttributeSchema())
                .permissionScheme(request.getPermissionScheme())
                .build();
        AssetTypeEntity saved = assetTypeRepository.save(entity);
        log.info("Created asset type: {} ({})", saved.getName(), saved.getId());
        return mapAssetTypeToResponse(saved);
    }

    @Transactional
    public AssetTypeResponse updateAssetType(UUID id, CreateAssetTypeRequest request) {
        AssetTypeEntity entity = assetTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset type not found: " + id));
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getAttributeSchema() != null) {
            entity.setAttributeSchema(request.getAttributeSchema());
        }
        if (request.getPermissionScheme() != null) {
            entity.setPermissionScheme(request.getPermissionScheme());
        }
        AssetTypeEntity saved = assetTypeRepository.save(entity);
        log.info("Updated asset type: {} ({})", saved.getName(), saved.getId());
        return mapAssetTypeToResponse(saved);
    }

    @Transactional
    public void deactivateAssetType(UUID id) {
        AssetTypeEntity entity = assetTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset type not found: " + id));
        entity.setIsActive(false);
        assetTypeRepository.save(entity);
        log.info("Deactivated asset type: {} ({})", entity.getName(), entity.getId());
    }

    // ==================== Assets ====================

    @Transactional(readOnly = true)
    public List<AssetResponse> getAllAssets() {
        return assetRepository.findByIsActiveTrue().stream()
                .map(this::mapAssetToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByType(UUID assetTypeId) {
        return assetRepository.findByAssetTypeIdAndIsActiveTrue(assetTypeId).stream()
                .map(this::mapAssetToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByStatus(String status) {
        return assetRepository.findByStatusAndIsActiveTrue(status).stream()
                .map(this::mapAssetToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByLocation(String location) {
        return assetRepository.findByLocationAndIsActiveTrue(location).stream()
                .map(this::mapAssetToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssetResponse getAssetById(UUID id) {
        AssetEntity entity = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + id));
        return mapAssetToResponse(entity);
    }

    @Transactional
    public AssetResponse createAsset(CreateAssetRequest request) {
        // Verify asset type exists
        assetTypeRepository.findById(request.getAssetTypeId())
                .orElseThrow(() -> new RuntimeException("Asset type not found: " + request.getAssetTypeId()));

        AssetEntity entity = AssetEntity.builder()
                .assetTypeId(request.getAssetTypeId())
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .subStatus(request.getSubStatus())
                .location(request.getLocation())
                .attributes(request.getAttributes())
                .serialNumber(request.getSerialNumber())
                .qrCodeData(generateQrCodeData(request))
                .build();
        AssetEntity saved = assetRepository.save(entity);
        log.info("Created asset: {} ({}) of type {}", saved.getName(), saved.getId(), saved.getAssetTypeId());
        return mapAssetToResponse(saved);
    }

    @Transactional
    public AssetResponse updateAsset(UUID id, CreateAssetRequest request) {
        AssetEntity entity = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + id));
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getSubStatus() != null) {
            entity.setSubStatus(request.getSubStatus());
        }
        if (request.getLocation() != null) {
            entity.setLocation(request.getLocation());
        }
        if (request.getAttributes() != null) {
            entity.setAttributes(request.getAttributes());
        }
        if (request.getSerialNumber() != null) {
            entity.setSerialNumber(request.getSerialNumber());
        }
        AssetEntity saved = assetRepository.save(entity);
        log.info("Updated asset: {} ({})", saved.getName(), saved.getId());
        return mapAssetToResponse(saved);
    }

    @Transactional
    public void deactivateAsset(UUID id) {
        AssetEntity entity = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + id));
        entity.setIsActive(false);
        assetRepository.save(entity);
        log.info("Deactivated asset: {} ({})", entity.getName(), entity.getId());
    }

    // ==================== Asset-Issue Links ====================

    @Transactional
    public AssetIssueLinkResponse linkAssetToIssue(AssetIssueLinkRequest request) {
        if (assetIssueLinkRepository.existsByAssetIdAndIssueId(request.getAssetId(), request.getIssueId())) {
            throw new IllegalStateException("Asset is already linked to this issue");
        }
        AssetIssueLinkEntity entity = AssetIssueLinkEntity.builder()
                .assetId(request.getAssetId())
                .issueId(request.getIssueId())
                .linkType(request.getLinkType() != null ? request.getLinkType() : "RELATED")
                .build();
        AssetIssueLinkEntity saved = assetIssueLinkRepository.save(entity);
        log.info("Linked asset {} to issue {} (type={})", saved.getAssetId(), saved.getIssueId(), saved.getLinkType());
        return mapLinkToResponse(saved);
    }

    @Transactional
    public void unlinkAssetFromIssue(UUID linkId) {
        AssetIssueLinkEntity entity = assetIssueLinkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Asset-issue link not found: " + linkId));
        assetIssueLinkRepository.delete(entity);
        log.info("Unlinked asset {} from issue {}", entity.getAssetId(), entity.getIssueId());
    }

    @Transactional(readOnly = true)
    public List<AssetIssueLinkResponse> getLinkedIssues(UUID assetId) {
        return assetIssueLinkRepository.findByAssetId(assetId).stream()
                .map(this::mapLinkToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetIssueLinkResponse> getLinkedAssets(UUID issueId) {
        return assetIssueLinkRepository.findByIssueId(issueId).stream()
                .map(this::mapLinkToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Private Helpers ====================

    private String generateQrCodeData(CreateAssetRequest request) {
        return String.format("ASSET:%s|SN:%s|NAME:%s",
                request.getAssetTypeId(),
                request.getSerialNumber() != null ? request.getSerialNumber() : "N/A",
                request.getName());
    }

    private AssetTypeResponse mapAssetTypeToResponse(AssetTypeEntity entity) {
        return AssetTypeResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .attributeSchema(entity.getAttributeSchema())
                .permissionScheme(entity.getPermissionScheme())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AssetResponse mapAssetToResponse(AssetEntity entity) {
        return AssetResponse.builder()
                .id(entity.getId())
                .assetTypeId(entity.getAssetTypeId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .subStatus(entity.getSubStatus())
                .location(entity.getLocation())
                .attributes(entity.getAttributes())
                .serialNumber(entity.getSerialNumber())
                .qrCodeData(entity.getQrCodeData())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AssetIssueLinkResponse mapLinkToResponse(AssetIssueLinkEntity entity) {
        return AssetIssueLinkResponse.builder()
                .id(entity.getId())
                .assetId(entity.getAssetId())
                .issueId(entity.getIssueId())
                .linkType(entity.getLinkType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
