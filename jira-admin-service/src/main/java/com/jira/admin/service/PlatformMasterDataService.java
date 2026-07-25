package com.jira.admin.service;

import com.jira.admin.dto.MasterDataRequest;
import com.jira.admin.dto.MasterDataResponse;
import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD service for platform-level master data types (statuses, priorities,
 * issue types, resolutions, link types, roles, permissions, board types,
 * notification events, quick filters).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformMasterDataService {

    private final MasterStatusRepository statusRepo;
    private final MasterPriorityRepository priorityRepo;
    private final MasterIssueTypeRepository issueTypeRepo;
    private final MasterResolutionRepository resolutionRepo;
    private final MasterLinkTypeRepository linkTypeRepo;
    private final MasterRoleRepository roleRepo;
    private final MasterPermissionRepository permissionRepo;
    private final MasterBoardTypeRepository boardTypeRepo;
    private final MasterBoardColumnTemplateRepository columnTemplateRepo;
    private final MasterNotificationEventRepository notifEventRepo;
    private final MasterQuickFilterPresetRepository quickFilterRepo;

    // ==================== Statuses ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllStatuses() {
        return statusRepo.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(MasterDataResponse::fromStatus).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getStatus(UUID id) {
        return MasterDataResponse.fromStatus(statusRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createStatus(MasterDataRequest req) {
        if (statusRepo.existsByStatusKey(req.getKey())) {
            throw new IllegalArgumentException("Status key already exists: " + req.getKey());
        }
        MasterStatusEntity entity = MasterStatusEntity.builder()
                .statusKey(req.getKey())
                .displayName(req.getDisplayName())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : "TODO")
                .color(req.getColor() != null ? req.getColor() : "#6C757D")
                .icon(req.getIcon())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isSystem(req.getIsSystem() != null ? req.getIsSystem() : false)
                .isActive(true)
                .build();
        entity = statusRepo.save(entity);
        log.info("Master status created: {}", req.getKey());
        return MasterDataResponse.fromStatus(entity);
    }

    @Transactional
    public MasterDataResponse updateStatus(UUID id, MasterDataRequest req) {
        MasterStatusEntity entity = statusRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found: " + id));
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getCategory() != null) entity.setCategory(req.getCategory());
        if (req.getColor() != null) entity.setColor(req.getColor());
        if (req.getIcon() != null) entity.setIcon(req.getIcon());
        if (req.getSortOrder() != null) entity.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = statusRepo.save(entity);
        log.info("Master status updated: {}", entity.getStatusKey());
        return MasterDataResponse.fromStatus(entity);
    }

    @Transactional
    public void deleteStatus(UUID id) {
        MasterStatusEntity entity = statusRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status not found: " + id));
        if (entity.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system status: " + entity.getStatusKey());
        }
        statusRepo.delete(entity);
        log.info("Master status deleted: {}", entity.getStatusKey());
    }

    // ==================== Priorities ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllPriorities() {
        return priorityRepo.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(MasterDataResponse::fromPriority).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getPriority(UUID id) {
        return MasterDataResponse.fromPriority(priorityRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Priority not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createPriority(MasterDataRequest req) {
        if (priorityRepo.existsByPriorityKey(req.getKey())) {
            throw new IllegalArgumentException("Priority key already exists: " + req.getKey());
        }
        MasterPriorityEntity entity = MasterPriorityEntity.builder()
                .priorityKey(req.getKey())
                .displayName(req.getDisplayName())
                .description(req.getDescription())
                .color(req.getColor() != null ? req.getColor() : "#6C757D")
                .iconUrl(req.getIconUrl())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isDefault(req.getIsDefault() != null ? req.getIsDefault() : false)
                .isActive(true)
                .build();
        entity = priorityRepo.save(entity);
        log.info("Master priority created: {}", req.getKey());
        return MasterDataResponse.fromPriority(entity);
    }

    @Transactional
    public MasterDataResponse updatePriority(UUID id, MasterDataRequest req) {
        MasterPriorityEntity entity = priorityRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Priority not found: " + id));
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getColor() != null) entity.setColor(req.getColor());
        if (req.getIconUrl() != null) entity.setIconUrl(req.getIconUrl());
        if (req.getSortOrder() != null) entity.setSortOrder(req.getSortOrder());
        if (req.getIsDefault() != null) entity.setIsDefault(req.getIsDefault());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = priorityRepo.save(entity);
        log.info("Master priority updated: {}", entity.getPriorityKey());
        return MasterDataResponse.fromPriority(entity);
    }

    @Transactional
    public void deletePriority(UUID id) {
        MasterPriorityEntity entity = priorityRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Priority not found: " + id));
        priorityRepo.delete(entity);
        log.info("Master priority deleted: {}", entity.getPriorityKey());
    }

    // ==================== Issue Types ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllIssueTypes() {
        return issueTypeRepo.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(MasterDataResponse::fromIssueType).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getIssueType(UUID id) {
        return MasterDataResponse.fromIssueType(issueTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue type not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createIssueType(MasterDataRequest req) {
        if (issueTypeRepo.existsByTypeKey(req.getKey())) {
            throw new IllegalArgumentException("Issue type key already exists: " + req.getKey());
        }
        MasterIssueTypeEntity entity = MasterIssueTypeEntity.builder()
                .typeKey(req.getKey())
                .displayName(req.getDisplayName())
                .description(req.getDescription())
                .icon(req.getIcon() != null ? req.getIcon() : "standard")
                .color(req.getColor())
                .isSubtask(req.getIsSubtask() != null ? req.getIsSubtask() : false)
                .isSystem(req.getIsSystem() != null ? req.getIsSystem() : false)
                .isActive(true)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();
        entity = issueTypeRepo.save(entity);
        log.info("Master issue type created: {}", req.getKey());
        return MasterDataResponse.fromIssueType(entity);
    }

    @Transactional
    public MasterDataResponse updateIssueType(UUID id, MasterDataRequest req) {
        MasterIssueTypeEntity entity = issueTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue type not found: " + id));
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getIcon() != null) entity.setIcon(req.getIcon());
        if (req.getColor() != null) entity.setColor(req.getColor());
        if (req.getSortOrder() != null) entity.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = issueTypeRepo.save(entity);
        log.info("Master issue type updated: {}", entity.getTypeKey());
        return MasterDataResponse.fromIssueType(entity);
    }

    @Transactional
    public void deleteIssueType(UUID id) {
        MasterIssueTypeEntity entity = issueTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue type not found: " + id));
        if (entity.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system issue type: " + entity.getTypeKey());
        }
        issueTypeRepo.delete(entity);
        log.info("Master issue type deleted: {}", entity.getTypeKey());
    }

    // ==================== Resolutions ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllResolutions() {
        return resolutionRepo.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(MasterDataResponse::fromResolution).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getResolution(UUID id) {
        return MasterDataResponse.fromResolution(resolutionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resolution not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createResolution(MasterDataRequest req) {
        if (resolutionRepo.existsByResolutionKey(req.getKey())) {
            throw new IllegalArgumentException("Resolution key already exists: " + req.getKey());
        }
        MasterResolutionEntity entity = MasterResolutionEntity.builder()
                .resolutionKey(req.getKey())
                .displayName(req.getDisplayName())
                .description(req.getDescription())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isDefault(req.getIsDefault() != null ? req.getIsDefault() : false)
                .isActive(true)
                .build();
        entity = resolutionRepo.save(entity);
        log.info("Master resolution created: {}", req.getKey());
        return MasterDataResponse.fromResolution(entity);
    }

    @Transactional
    public MasterDataResponse updateResolution(UUID id, MasterDataRequest req) {
        MasterResolutionEntity entity = resolutionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resolution not found: " + id));
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getSortOrder() != null) entity.setSortOrder(req.getSortOrder());
        if (req.getIsDefault() != null) entity.setIsDefault(req.getIsDefault());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = resolutionRepo.save(entity);
        log.info("Master resolution updated: {}", entity.getResolutionKey());
        return MasterDataResponse.fromResolution(entity);
    }

    @Transactional
    public void deleteResolution(UUID id) {
        MasterResolutionEntity entity = resolutionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resolution not found: " + id));
        resolutionRepo.delete(entity);
        log.info("Master resolution deleted: {}", entity.getResolutionKey());
    }

    // ==================== Link Types ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllLinkTypes() {
        return linkTypeRepo.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(MasterDataResponse::fromLinkType).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getLinkType(UUID id) {
        return MasterDataResponse.fromLinkType(linkTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Link type not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createLinkType(MasterDataRequest req) {
        if (linkTypeRepo.existsByLinkKey(req.getKey())) {
            throw new IllegalArgumentException("Link type key already exists: " + req.getKey());
        }
        MasterLinkTypeEntity entity = MasterLinkTypeEntity.builder()
                .linkKey(req.getKey())
                .outwardName(req.getOutwardName() != null ? req.getOutwardName() : req.getDisplayName())
                .inwardName(req.getInwardName() != null ? req.getInwardName() : req.getDisplayName())
                .description(req.getDescription())
                .isSystem(req.getIsSystem() != null ? req.getIsSystem() : false)
                .isActive(true)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();
        entity = linkTypeRepo.save(entity);
        log.info("Master link type created: {}", req.getKey());
        return MasterDataResponse.fromLinkType(entity);
    }

    @Transactional
    public MasterDataResponse updateLinkType(UUID id, MasterDataRequest req) {
        MasterLinkTypeEntity entity = linkTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Link type not found: " + id));
        if (req.getOutwardName() != null) entity.setOutwardName(req.getOutwardName());
        if (req.getInwardName() != null) entity.setInwardName(req.getInwardName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getSortOrder() != null) entity.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = linkTypeRepo.save(entity);
        log.info("Master link type updated: {}", entity.getLinkKey());
        return MasterDataResponse.fromLinkType(entity);
    }

    @Transactional
    public void deleteLinkType(UUID id) {
        MasterLinkTypeEntity entity = linkTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Link type not found: " + id));
        if (entity.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system link type: " + entity.getLinkKey());
        }
        linkTypeRepo.delete(entity);
        log.info("Master link type deleted: {}", entity.getLinkKey());
    }

    // ==================== Roles ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllRoles() {
        return roleRepo.findByIsActiveTrueOrderByDisplayNameAsc().stream()
                .map(MasterDataResponse::fromRole).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getRole(UUID id) {
        return MasterDataResponse.fromRole(roleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createRole(MasterDataRequest req) {
        if (roleRepo.existsByRoleKey(req.getKey())) {
            throw new IllegalArgumentException("Role key already exists: " + req.getKey());
        }
        MasterRoleEntity entity = MasterRoleEntity.builder()
                .roleKey(req.getKey())
                .displayName(req.getDisplayName())
                .description(req.getDescription())
                .isSystem(req.getIsSystem() != null ? req.getIsSystem() : false)
                .isActive(true)
                .build();
        entity = roleRepo.save(entity);
        log.info("Master role created: {}", req.getKey());
        return MasterDataResponse.fromRole(entity);
    }

    @Transactional
    public MasterDataResponse updateRole(UUID id, MasterDataRequest req) {
        MasterRoleEntity entity = roleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = roleRepo.save(entity);
        log.info("Master role updated: {}", entity.getRoleKey());
        return MasterDataResponse.fromRole(entity);
    }

    @Transactional
    public void deleteRole(UUID id) {
        MasterRoleEntity entity = roleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        if (entity.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system role: " + entity.getRoleKey());
        }
        roleRepo.delete(entity);
        log.info("Master role deleted: {}", entity.getRoleKey());
    }

    // ==================== Permissions ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllPermissions() {
        return permissionRepo.findByIsActiveTrueOrderByCategoryAscDisplayNameAsc().stream()
                .map(MasterDataResponse::fromPermission).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getPermission(UUID id) {
        return MasterDataResponse.fromPermission(permissionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createPermission(MasterDataRequest req) {
        if (permissionRepo.existsByPermissionKey(req.getKey())) {
            throw new IllegalArgumentException("Permission key already exists: " + req.getKey());
        }
        MasterPermissionEntity entity = MasterPermissionEntity.builder()
                .permissionKey(req.getKey())
                .displayName(req.getDisplayName())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : "CUSTOM")
                .isSystem(req.getIsSystem() != null ? req.getIsSystem() : false)
                .isActive(true)
                .build();
        entity = permissionRepo.save(entity);
        log.info("Master permission created: {}", req.getKey());
        return MasterDataResponse.fromPermission(entity);
    }

    @Transactional
    public MasterDataResponse updatePermission(UUID id, MasterDataRequest req) {
        MasterPermissionEntity entity = permissionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getCategory() != null) entity.setCategory(req.getCategory());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = permissionRepo.save(entity);
        log.info("Master permission updated: {}", entity.getPermissionKey());
        return MasterDataResponse.fromPermission(entity);
    }

    @Transactional
    public void deletePermission(UUID id) {
        MasterPermissionEntity entity = permissionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
        if (entity.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system permission: " + entity.getPermissionKey());
        }
        permissionRepo.delete(entity);
        log.info("Master permission deleted: {}", entity.getPermissionKey());
    }

    // ==================== Board Types ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllBoardTypes() {
        return boardTypeRepo.findByIsActiveTrueOrderByDisplayNameAsc().stream()
                .map(MasterDataResponse::fromBoardType).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getBoardType(UUID id) {
        return MasterDataResponse.fromBoardType(boardTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Board type not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createBoardType(MasterDataRequest req) {
        if (boardTypeRepo.existsByTypeKey(req.getKey())) {
            throw new IllegalArgumentException("Board type key already exists: " + req.getKey());
        }
        MasterBoardTypeEntity entity = MasterBoardTypeEntity.builder()
                .typeKey(req.getKey())
                .displayName(req.getDisplayName())
                .description(req.getDescription())
                .isActive(true)
                .build();
        entity = boardTypeRepo.save(entity);
        log.info("Master board type created: {}", req.getKey());
        return MasterDataResponse.fromBoardType(entity);
    }

    @Transactional
    public MasterDataResponse updateBoardType(UUID id, MasterDataRequest req) {
        MasterBoardTypeEntity entity = boardTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Board type not found: " + id));
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = boardTypeRepo.save(entity);
        log.info("Master board type updated: {}", entity.getTypeKey());
        return MasterDataResponse.fromBoardType(entity);
    }

    @Transactional
    public void deleteBoardType(UUID id) {
        MasterBoardTypeEntity entity = boardTypeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Board type not found: " + id));
        boardTypeRepo.delete(entity);
        log.info("Master board type deleted: {}", entity.getTypeKey());
    }

    // ==================== Notification Events ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllNotificationEvents() {
        return notifEventRepo.findByIsActiveTrueOrderByCategoryAscDisplayNameAsc().stream()
                .map(MasterDataResponse::fromNotificationEvent).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getNotificationEvent(UUID id) {
        return MasterDataResponse.fromNotificationEvent(notifEventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification event not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createNotificationEvent(MasterDataRequest req) {
        if (notifEventRepo.existsByEventKey(req.getKey())) {
            throw new IllegalArgumentException("Notification event key already exists: " + req.getKey());
        }
        MasterNotificationEventEntity entity = MasterNotificationEventEntity.builder()
                .eventKey(req.getKey())
                .displayName(req.getDisplayName())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : "CUSTOM")
                .isSystem(req.getIsSystem() != null ? req.getIsSystem() : false)
                .isActive(true)
                .build();
        entity = notifEventRepo.save(entity);
        log.info("Master notification event created: {}", req.getKey());
        return MasterDataResponse.fromNotificationEvent(entity);
    }

    @Transactional
    public MasterDataResponse updateNotificationEvent(UUID id, MasterDataRequest req) {
        MasterNotificationEventEntity entity = notifEventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification event not found: " + id));
        if (req.getDisplayName() != null) entity.setDisplayName(req.getDisplayName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getCategory() != null) entity.setCategory(req.getCategory());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = notifEventRepo.save(entity);
        log.info("Master notification event updated: {}", entity.getEventKey());
        return MasterDataResponse.fromNotificationEvent(entity);
    }

    @Transactional
    public void deleteNotificationEvent(UUID id) {
        MasterNotificationEventEntity entity = notifEventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification event not found: " + id));
        if (entity.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system notification event: " + entity.getEventKey());
        }
        notifEventRepo.delete(entity);
        log.info("Master notification event deleted: {}", entity.getEventKey());
    }

    // ==================== Quick Filters ====================

    @Transactional(readOnly = true)
    public List<MasterDataResponse> getAllQuickFilters() {
        return quickFilterRepo.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(MasterDataResponse::fromQuickFilter).toList();
    }

    @Transactional(readOnly = true)
    public MasterDataResponse getQuickFilter(UUID id) {
        return MasterDataResponse.fromQuickFilter(quickFilterRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quick filter not found: " + id)));
    }

    @Transactional
    public MasterDataResponse createQuickFilter(MasterDataRequest req) {
        MasterQuickFilterPresetEntity entity = MasterQuickFilterPresetEntity.builder()
                .filterName(req.getDisplayName() != null ? req.getDisplayName() : req.getKey())
                .jqlQuery(req.getJqlQuery())
                .icon(req.getIcon())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isSystem(req.getIsSystem() != null ? req.getIsSystem() : false)
                .isActive(true)
                .build();
        entity = quickFilterRepo.save(entity);
        log.info("Master quick filter created: {}", entity.getFilterName());
        return MasterDataResponse.fromQuickFilter(entity);
    }

    @Transactional
    public MasterDataResponse updateQuickFilter(UUID id, MasterDataRequest req) {
        MasterQuickFilterPresetEntity entity = quickFilterRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quick filter not found: " + id));
        if (req.getDisplayName() != null) entity.setFilterName(req.getDisplayName());
        if (req.getJqlQuery() != null) entity.setJqlQuery(req.getJqlQuery());
        if (req.getIcon() != null) entity.setIcon(req.getIcon());
        if (req.getSortOrder() != null) entity.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) entity.setIsActive(req.getIsActive());
        entity = quickFilterRepo.save(entity);
        log.info("Master quick filter updated: {}", entity.getFilterName());
        return MasterDataResponse.fromQuickFilter(entity);
    }

    @Transactional
    public void deleteQuickFilter(UUID id) {
        MasterQuickFilterPresetEntity entity = quickFilterRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quick filter not found: " + id));
        if (entity.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system quick filter: " + entity.getFilterName());
        }
        quickFilterRepo.delete(entity);
        log.info("Master quick filter deleted: {}", entity.getFilterName());
    }
}
