package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.TestFolder;
import com.avionics_systems.test.entity.TestIssue;
import com.avionics_systems.test.entity.TestExecution;
import com.avionics_systems.test.entity.TestFolderAccess;
import com.avionics_systems.test.entity.TestFolderTemplate;
import com.avionics_systems.test.entity.TestFolderAccess.AccessLevel;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.exception.InvalidOperationException;
import com.avionics_systems.test.repository.TestFolderRepository;
import com.avionics_systems.test.repository.TestIssueRepository;
import com.avionics_systems.test.repository.TestExecutionRepository;
import com.avionics_systems.test.repository.TestFolderAccessRepository;
import com.avionics_systems.test.repository.TestFolderTemplateRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestFolderService {

    private final TestFolderRepository folderRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final TestFolderAccessRepository folderAccessRepository;
    private final TestFolderTemplateRepository folderTemplateRepository;

    @Value("${app.defaults.folder-type:FOLDER}")
    private String defaultFolderType;

    @Value("${app.defaults.folder-permission-level:READ}")
    private String defaultPermissionLevel;

    @Value("${app.defaults.folder-search-page-size:50}")
    private int defaultSearchPageSize;

    @Value("${app.defaults.folder-health.pass-rate-warning:70}")
    private double healthPassRateWarning;

    @Value("${app.defaults.folder-health.flaky-rate-warning:10}")
    private double healthFlakyRateWarning;

    // ==================== Core Folder Operations ====================

    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request) {
        log.info("Creating folder: {} for project: {}", request.getName(), request.getProjectId());

        String path = "";
        int depth = 0;

        if (request.getParentId() != null) {
            TestFolder parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder", "id", request.getParentId()));
            path = parent.getPath() + "/" + request.getName();
            depth = parent.getDepth() + 1;
        } else {
            path = "/" + request.getName();
        }

        TestFolder folder = TestFolder.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .parentId(request.getParentId())
                .path(path)
                .depth(depth)
                .folderType(request.getFolderType() != null ? request.getFolderType() : defaultFolderType)
                .icon(request.getIcon())
                .color(request.getColor())
                .filterCriteria(request.getFilterCriteria())
                .tags(request.getTags())
                .build();

        folder = folderRepository.save(folder);

        // Create owner access permission
        UUID currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            folder.setOwnerId(currentUserId);
            folder = folderRepository.save(folder);
        }

        log.info("Folder created with id: {}", folder.getId());
        return mapToFolderResponse(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getRootFolders(UUID projectId) {
        return folderRepository.findByProjectIdAndParentIdIsNullOrderBySortOrderAsc(projectId).stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getChildFolders(UUID parentId) {
        return folderRepository.findByParentIdOrderBySortOrderAsc(parentId).stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFolderTree(UUID projectId) {
        return folderRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FolderResponse getById(UUID id) {
        TestFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));
        return mapToFolderResponse(folder);
    }

    @Transactional
    public FolderResponse updateFolder(UUID id, UpdateFolderRequest request) {
        TestFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));

        if (request.getName() != null) {
            folder.setName(request.getName());
        }
        if (request.getDescription() != null) {
            folder.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }
        if (request.getIcon() != null) {
            folder.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            folder.setColor(request.getColor());
        }
        if (request.getTags() != null) {
            folder.setTags(request.getTags());
        }

        folder = folderRepository.save(folder);
        return mapToFolderResponse(folder);
    }

    @Transactional
    public FolderResponse moveFolder(UUID folderId, UUID newParentId, Integer sortOrder) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Validate not moving to self or descendant
        if (newParentId != null) {
            if (newParentId.equals(folderId)) {
                throw new InvalidOperationException("Cannot move folder to itself");
            }
            if (isDescendant(folderId, newParentId)) {
                throw new InvalidOperationException("Cannot move folder to its own descendant");
            }

            TestFolder newParent = folderRepository.findById(newParentId)
                    .orElseThrow(() -> new ResourceNotFoundException("New parent folder", "id", newParentId));
            folder.setPath(newParent.getPath() + "/" + folder.getName());
            folder.setDepth(newParent.getDepth() + 1);
        } else {
            folder.setPath("/" + folder.getName());
            folder.setDepth(0);
        }

        folder.setParentId(newParentId);

        if (sortOrder != null) {
            folder.setSortOrder(sortOrder);
        }

        folder = folderRepository.save(folder);

        // Update paths of all descendants
        updateDescendantPaths(folder);

        return mapToFolderResponse(folder);
    }

    @Transactional
    public void deleteFolder(UUID id, boolean force) {
        TestFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));

        long childCount = folderRepository.countChildren(id);
        if (childCount > 0 && !force) {
            throw new InvalidOperationException("Cannot delete folder with children. Move or delete children first, or use force=true");
        }

        if (force) {
            // Delete all descendant folders recursively
            deleteDescendants(id);
        }

        // Delete folder access permissions
        folderAccessRepository.deleteByFolderId(id);

        folderRepository.deleteById(id);
        log.info("Folder deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getStarredFolders(UUID projectId) {
        return folderRepository.findStarredFolders(projectId).stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FolderResponse toggleStar(UUID id) {
        TestFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));
        folder.setIsStarred(!folder.getIsStarred());
        folder = folderRepository.save(folder);
        return mapToFolderResponse(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFoldersByType(UUID projectId, String type) {
        return folderRepository.findByProjectIdAndType(projectId, type).stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());
    }

    // ==================== Folder Hierarchy Management ====================

    @Transactional
    public FolderResponse copyFolder(UUID folderId, UUID targetParentId, UUID targetProjectId) {
        TestFolder sourceFolder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        String newPath;
        int newDepth;

        if (targetParentId != null) {
            TestFolder parent = folderRepository.findById(targetParentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target parent folder", "id", targetParentId));
            newPath = parent.getPath() + "/" + sourceFolder.getName();
            newDepth = parent.getDepth() + 1;
        } else {
            newPath = "/" + sourceFolder.getName();
            newDepth = 0;
        }

        // Copy the folder
        TestFolder newFolder = TestFolder.builder()
                .name(sourceFolder.getName() + " (Copy)")
                .description(sourceFolder.getDescription())
                .projectId(targetProjectId != null ? targetProjectId : sourceFolder.getProjectId())
                .parentId(targetParentId)
                .path(newPath)
                .depth(newDepth)
                .folderType(sourceFolder.getFolderType())
                .icon(sourceFolder.getIcon())
                .color(sourceFolder.getColor())
                .filterCriteria(sourceFolder.getFilterCriteria())
                .tags(new ArrayList<>(sourceFolder.getTags()))
                .build();

        newFolder = folderRepository.save(newFolder);

        // Copy children recursively
        List<TestFolder> children = folderRepository.findByParentIdOrderBySortOrderAsc(folderId);
        for (TestFolder child : children) {
            copyFolder(child.getId(), newFolder.getId(), targetProjectId);
        }

        log.info("Folder copied from {} to {}", folderId, newFolder.getId());
        return mapToFolderResponse(newFolder);
    }

    @Transactional
    public List<FolderResponse> getFolderBreadcrumb(UUID folderId) {
        List<FolderResponse> breadcrumb = new ArrayList<>();
        UUID currentId = folderId;

        while (currentId != null) {
            final UUID lookupId = currentId;
            TestFolder folder = folderRepository.findById(lookupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", lookupId));
            breadcrumb.add(0, mapToFolderResponse(folder));
            currentId = folder.getParentId();
        }

        return breadcrumb;
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFolderAncestors(UUID folderId) {
        return getFolderBreadcrumb(folderId);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getFolderDescendants(UUID folderId) {
        List<FolderResponse> descendants = new ArrayList<>();
        collectDescendants(folderId, descendants);
        return descendants;
    }

    private void collectDescendants(UUID parentId, List<FolderResponse> descendants) {
        List<TestFolder> children = folderRepository.findByParentIdOrderBySortOrderAsc(parentId);
        for (TestFolder child : children) {
            descendants.add(mapToFolderResponse(child));
            collectDescendants(child.getId(), descendants);
        }
    }

    @Transactional
    public FolderResponse renameFolder(UUID id, String newName) {
        TestFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", id));

        String oldName = folder.getName();
        folder.setName(newName);

        // Update path
        UUID parentId = folder.getParentId();
        if (parentId != null) {
            TestFolder parent = folderRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder", "id", parentId));
            folder.setPath(parent.getPath() + "/" + newName);
        } else {
            folder.setPath("/" + newName);
        }

        folder = folderRepository.save(folder);

        // Update descendant paths
        updateDescendantPaths(folder);

        log.info("Folder renamed from {} to {}", oldName, newName);
        return mapToFolderResponse(folder);
    }

    @Transactional
    public void reorderFolders(UUID projectId, List<UUID> folderOrder) {
        for (int i = 0; i < folderOrder.size(); i++) {
            UUID folderId = folderOrder.get(i);
            TestFolder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
            folder.setSortOrder(i);
            folderRepository.save(folder);
        }
        log.info("Reordered {} folders in project {}", folderOrder.size(), projectId);
    }

    // ==================== Folder Permissions ====================

    @Transactional
    public FolderPermissionResponse manageFolderPermissions(FolderPermissionRequest request) {
        TestFolder folder = folderRepository.findById(request.getFolderId())
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", request.getFolderId()));

        UUID currentUserId = getCurrentUserId();

        // Update user permissions
        if (request.getUserIds() != null) {
            for (UUID userId : request.getUserIds()) {
                TestFolderAccess access = folderAccessRepository
                        .findByFolderIdAndUserId(request.getFolderId(), userId)
                        .orElse(TestFolderAccess.builder()
                                .folderId(request.getFolderId())
                                .userId(userId)
                                .build());

                access.setAccessLevel(AccessLevel.valueOf(
                        request.getPermissionLevel() != null ? request.getPermissionLevel() : defaultPermissionLevel));
                access.setGrantedBy(currentUserId);
                access.setGrantedAt(LocalDateTime.now());
                folderAccessRepository.save(access);
            }
        }

        // Update group permissions
        if (request.getGroupIds() != null) {
            for (UUID groupId : request.getGroupIds()) {
                TestFolderAccess access = folderAccessRepository
                        .findByFolderIdAndGroupId(request.getFolderId(), groupId)
                        .orElse(TestFolderAccess.builder()
                                .folderId(request.getFolderId())
                                .groupId(groupId)
                                .build());

                access.setAccessLevel(AccessLevel.valueOf(
                        request.getPermissionLevel() != null ? request.getPermissionLevel() : defaultPermissionLevel));
                access.setGrantedBy(currentUserId);
                access.setGrantedAt(LocalDateTime.now());
                folderAccessRepository.save(access);
            }
        }

        log.info("Updated permissions for folder {}", request.getFolderId());
        return getFolderPermissions(request.getFolderId());
    }

    @Transactional(readOnly = true)
    public FolderPermissionResponse getFolderPermissions(UUID folderId) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        List<TestFolderAccess> accesses = folderAccessRepository.findByFolderId(folderId);

        List<FolderAccessEntry> userEntries = accesses.stream()
                .filter(a -> a.getUserId() != null)
                .map(a -> FolderAccessEntry.builder()
                        .id(a.getUserId())
                        .name(a.getUserId().toString())
                        .type("USER")
                        .permissionLevel(a.getAccessLevel().name())
                        .grantedAt(a.getGrantedAt())
                        .grantedBy(a.getGrantedBy())
                        .build())
                .collect(Collectors.toList());

        List<FolderAccessEntry> groupEntries = accesses.stream()
                .filter(a -> a.getGroupId() != null)
                .map(a -> FolderAccessEntry.builder()
                        .id(a.getGroupId())
                        .name(a.getGroupId().toString())
                        .type("GROUP")
                        .permissionLevel(a.getAccessLevel().name())
                        .grantedAt(a.getGrantedAt())
                        .grantedBy(a.getGrantedBy())
                        .build())
                .collect(Collectors.toList());

        return FolderPermissionResponse.builder()
                .folderId(folderId)
                .folderName(folder.getName())
                .users(userEntries)
                .groups(groupEntries)
                .inheritanceEnabled(true)
                .effectivePermission(defaultPermissionLevel)
                .build();
    }

    @Transactional
    public void removeFolderPermission(UUID folderId, UUID userId, UUID groupId) {
        if (userId != null) {
            folderAccessRepository.deleteByFolderIdAndUserId(folderId, userId);
        }
        if (groupId != null) {
            folderAccessRepository.deleteByFolderIdAndGroupId(folderId, groupId);
        }
        log.info("Removed permission from folder {} for user {} / group {}", folderId, userId, groupId);
    }

    @Transactional(readOnly = true)
    public boolean hasFolderAccess(UUID folderId, UUID userId, String requiredAccess) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        // Owner has full access
        if (folder.getOwnerId() != null && folder.getOwnerId().equals(userId)) {
            return true;
        }

        // Check direct permission
        Optional<TestFolderAccess> access = folderAccessRepository.findByFolderIdAndUserId(folderId, userId);
        if (access.isPresent()) {
            return hasRequiredAccess(access.get().getAccessLevel(), requiredAccess);
        }

        // Check inherited permission from parent
        if (folder.getParentId() != null) {
            return hasFolderAccess(folder.getParentId(), userId, requiredAccess);
        }

        return false;
    }

    private boolean hasRequiredAccess(AccessLevel granted, String required) {
        Map<String, Integer> levelMap = Map.of(
                "READ", 1,
                "WRITE", 2,
                "ADMIN", 3
        );
        return levelMap.getOrDefault(granted.name(), 0) >= levelMap.getOrDefault(required, 0);
    }

    // ==================== Folder Templates ====================

    @Transactional
    public List<FolderTemplateResponse> getAvailableTemplates() {
        return folderTemplateRepository.findAll().stream()
                .map(this::mapToTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FolderResponse createFromTemplate(UUID templateId, UUID projectId, UUID parentId) {
        TestFolderTemplate template = folderTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));

        String path;
        int depth;

        if (parentId != null) {
            TestFolder parent = folderRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder", "id", parentId));
            path = parent.getPath() + "/" + template.getName();
            depth = parent.getDepth() + 1;
        } else {
            path = "/" + template.getName();
            depth = 0;
        }

        TestFolder folder = TestFolder.builder()
                .name(template.getName())
                .description(template.getDescription())
                .projectId(projectId)
                .parentId(parentId)
                .path(path)
                .depth(depth)
                .folderType(template.getFolderType())
                .icon(template.getIcon())
                .color(template.getColor())
                .tags(new ArrayList<>(template.getTags()))
                .build();

        folder = folderRepository.save(folder);

        // Create subfolders from template
        if (template.getSubFolderTemplate() != null) {
            createSubFoldersFromTemplate(template.getSubFolderTemplate(), folder.getId(), projectId);
        }

        // Update template usage count
        template.setUsageCount(template.getUsageCount() + 1);
        folderTemplateRepository.save(template);

        log.info("Created folder {} from template {}", folder.getId(), templateId);
        return mapToFolderResponse(folder);
    }

    @Transactional
    public FolderTemplateResponse saveAsTemplate(UUID folderId, FolderTemplateRequest request) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        TestFolderTemplate template = TestFolderTemplate.builder()
                .name(request.getName() != null ? request.getName() : folder.getName() + " Template")
                .description(request.getDescription() != null ? request.getDescription() : folder.getDescription())
                .folderType(folder.getFolderType())
                .icon(folder.getIcon())
                .color(folder.getColor())
                .tags(request.getTags() != null ? request.getTags() : folder.getTags())
                .isSystemTemplate(false)
                .usageCount(0)
                .build();

        // Copy subfolder structure
        List<TestFolder> children = folderRepository.findByParentIdOrderBySortOrderAsc(folderId);
        if (!children.isEmpty()) {
            template.setSubFolderTemplate(buildSubFolderTemplate(children));
        }

        template = folderTemplateRepository.save(template);
        log.info("Saved folder {} as template {}", folderId, template.getId());
        return mapToTemplateResponse(template);
    }

    private void createSubFoldersFromTemplate(String subFolderJson, UUID parentId, UUID projectId) {
        // Parse JSON and create subfolders (simplified - actual implementation would parse JSON)
        log.info("Creating subfolders from template for parent {}", parentId);
    }

    private String buildSubFolderTemplate(List<TestFolder> children) {
        // Build JSON representation of subfolder structure
        return "[]";
    }

    // ==================== Bulk Operations ====================

    @Transactional
    public BulkFolderOperationResponse executeBulkOperation(BulkFolderOperationRequest request) {
        List<FolderOperationResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (UUID folderId : request.getFolderIds()) {
            try {
                TestFolder folder = folderRepository.findById(folderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

                switch (request.getOperationType()) {
                    case "MOVE":
                        if (request.getTargetParentId() != null) {
                            moveFolder(folderId, request.getTargetParentId(), null);
                        }
                        break;
                    case "UPDATE":
                        if (request.getUpdateFields() != null) {
                            updateFolderWithMap(folderId, request.getUpdateFields());
                        }
                        break;
                    case "ADD_TAGS":
                        if (request.getTagsToAdd() != null) {
                            addTags(folderId, request.getTagsToAdd());
                        }
                        break;
                    case "REMOVE_TAGS":
                        if (request.getTagsToRemove() != null) {
                            removeTags(folderId, request.getTagsToRemove());
                        }
                        break;
                    case "DELETE":
                        deleteFolder(folderId, Boolean.TRUE.equals(request.getRecursive()));
                        break;
                    case "STAR":
                        folder.setIsStarred(true);
                        folderRepository.save(folder);
                        break;
                    case "UNSTAR":
                        folder.setIsStarred(false);
                        folderRepository.save(folder);
                        break;
                    case "COPY":
                        copyFolder(folderId, request.getTargetParentId(), folder.getProjectId());
                        break;
                }

                results.add(FolderOperationResult.builder()
                        .folderId(folderId)
                        .folderName(folder.getName())
                        .success(true)
                        .message("Operation completed successfully")
                        .build());
                successCount++;

            } catch (Exception e) {
                results.add(FolderOperationResult.builder()
                        .folderId(folderId)
                        .folderName(null)
                        .success(false)
                        .message(e.getMessage())
                        .build());
                failedCount++;
            }
        }

        String status = failedCount == 0 ? "COMPLETED" :
                successCount == 0 ? "FAILED" : "PARTIAL_SUCCESS";

        log.info("Bulk operation {} completed: {} success, {} failed",
                request.getOperationType(), successCount, failedCount);

        return BulkFolderOperationResponse.builder()
                .totalRequested(request.getFolderIds().size())
                .successCount(successCount)
                .failedCount(failedCount)
                .status(status)
                .results(results)
                .build();
    }

    private void updateFolderWithMap(UUID folderId, Map<String, Object> fields) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        if (fields.containsKey("name")) {
            folder.setName((String) fields.get("name"));
        }
        if (fields.containsKey("description")) {
            folder.setDescription((String) fields.get("description"));
        }
        if (fields.containsKey("icon")) {
            folder.setIcon((String) fields.get("icon"));
        }
        if (fields.containsKey("color")) {
            folder.setColor((String) fields.get("color"));
        }
        if (fields.containsKey("sortOrder")) {
            folder.setSortOrder((Integer) fields.get("sortOrder"));
        }

        folderRepository.save(folder);
    }

    private void addTags(UUID folderId, List<String> tags) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
        List<String> currentTags = folder.getTags();
        if (currentTags == null) {
            currentTags = new ArrayList<>();
        }
        for (String tag : tags) {
            if (!currentTags.contains(tag)) {
                currentTags.add(tag);
            }
        }
        folder.setTags(currentTags);
        folderRepository.save(folder);
    }

    private void removeTags(UUID folderId, List<String> tags) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));
        List<String> currentTags = folder.getTags();
        if (currentTags != null) {
            currentTags.removeAll(tags);
            folder.setTags(currentTags);
            folderRepository.save(folder);
        }
    }

    // ==================== Folder Statistics ====================

    @Transactional(readOnly = true)
    public FolderAnalyticsResponse getFolderAnalytics(UUID folderId) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        return calculateFolderAnalytics(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> searchFolders(FolderSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getPageSize() != null ? request.getPageSize() : defaultSearchPageSize,
                Sort.by(request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("ASC")
                        ? Sort.Direction.ASC : Sort.Direction.DESC,
                        request.getSortBy() != null ? request.getSortBy() : "name")
        );

        List<TestFolder> folders;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            // Use repository search method
            String q = request.getQuery().toLowerCase();
            folders = folderRepository.findByProjectIdOrderBySortOrderAsc(request.getProjectId()).stream()
                    .filter(f -> f.getName() != null && f.getName().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        } else if (request.getParentId() != null) {
            folders = folderRepository.findByParentIdOrderBySortOrderAsc(request.getParentId());
        } else if (request.getFolderType() != null) {
            folders = folderRepository.findByProjectIdAndType(request.getProjectId(), request.getFolderType());
        } else if (Boolean.TRUE.equals(request.getStarredOnly())) {
            folders = folderRepository.findStarredFolders(request.getProjectId());
        } else if (request.getPathPrefix() != null) {
            folders = folderRepository.findByPathPrefix(request.getProjectId(), request.getPathPrefix());
        } else {
            folders = folderRepository.findByProjectIdOrderBySortOrderAsc(request.getProjectId());
        }

        // Filter by depth if specified
        if (request.getMinDepth() != null || request.getMaxDepth() != null) {
            folders = folders.stream()
                    .filter(f -> {
                        if (request.getMinDepth() != null && f.getDepth() < request.getMinDepth()) return false;
                        if (request.getMaxDepth() != null && f.getDepth() > request.getMaxDepth()) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // Filter by tags if specified
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            folders = folders.stream()
                    .filter(f -> f.getTags() != null && f.getTags().containsAll(request.getTags()))
                    .collect(Collectors.toList());
        }

        // Filter by status if specified
        if (request.getStatus() != null) {
            folders = folders.stream()
                    .filter(f -> request.getStatus().equals(f.getStatus()))
                    .collect(Collectors.toList());
        }

        return folders.stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecentFolderResponse getRecentFolders(UUID projectId, UUID userId, int maxResults) {
        // Get recently accessed folders (from access logs)
        List<TestFolderAccess> recentAccess = folderAccessRepository
                .findRecentByUser(userId, PageRequest.of(0, maxResults));

        List<FolderAccessRecord> recentlyAccessed = recentAccess.stream()
                .map(access -> {
                    TestFolder folder = folderRepository.findById(access.getFolderId()).orElse(null);
                    if (folder == null) return null;
                    return FolderAccessRecord.builder()
                            .folderId(folder.getId())
                            .folderName(folder.getName())
                            .projectId(folder.getProjectId())
                            .path(folder.getPath())
                            .accessedAt(access.getLastAccessedAt())
                            .accessedBy(access.getUserId())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Get recently modified folders
        List<TestFolder> recentModified = folderRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .sorted(Comparator.comparing(TestFolder::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(maxResults)
                .collect(Collectors.toList());

        List<FolderAccessRecord> recentlyModifiedRecords = recentModified.stream()
                .map(folder -> FolderAccessRecord.builder()
                        .folderId(folder.getId())
                        .folderName(folder.getName())
                        .projectId(folder.getProjectId())
                        .path(folder.getPath())
                        .modifiedAt(folder.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        // Get favorite folders
        List<TestFolder> starred = folderRepository.findStarredFolders(projectId);

        List<FolderAccessRecord> favorites = starred.stream()
                .map(folder -> FolderAccessRecord.builder()
                        .folderId(folder.getId())
                        .folderName(folder.getName())
                        .projectId(folder.getProjectId())
                        .path(folder.getPath())
                        .build())
                .collect(Collectors.toList());

        return RecentFolderResponse.builder()
                .recentlyAccessed(recentlyAccessed)
                .recentlyModified(recentlyModifiedRecords)
                .favorites(favorites)
                .maxResults(maxResults)
                .build();
    }

    // ==================== Folder Sharing ====================

    @Transactional
    public FolderResponse shareFolder(FolderShareRequest request) {
        TestFolder folder = folderRepository.findById(request.getFolderId())
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", request.getFolderId()));

        UUID currentUserId = getCurrentUserId();

        // Share with users
        if (request.getUserIds() != null) {
            for (UUID userId : request.getUserIds()) {
                TestFolderAccess access = folderAccessRepository
                        .findByFolderIdAndUserId(request.getFolderId(), userId)
                        .orElse(TestFolderAccess.builder()
                                .folderId(request.getFolderId())
                                .userId(userId)
                                .build());

                access.setAccessLevel(mapShareLevelToAccess(request.getShareLevel()));
                access.setGrantedBy(currentUserId);
                access.setGrantedAt(LocalDateTime.now());
                folderAccessRepository.save(access);
            }
        }

        // Share with groups
        if (request.getGroupIds() != null) {
            for (UUID groupId : request.getGroupIds()) {
                TestFolderAccess access = folderAccessRepository
                        .findByFolderIdAndGroupId(request.getFolderId(), groupId)
                        .orElse(TestFolderAccess.builder()
                                .folderId(request.getFolderId())
                                .groupId(groupId)
                                .build());

                access.setAccessLevel(mapShareLevelToAccess(request.getShareLevel()));
                access.setGrantedBy(currentUserId);
                access.setGrantedAt(LocalDateTime.now());
                folderAccessRepository.save(access);
            }
        }

        log.info("Folder {} shared with {} users and {} groups",
                request.getFolderId(),
                request.getUserIds() != null ? request.getUserIds().size() : 0,
                request.getGroupIds() != null ? request.getGroupIds().size() : 0);

        return mapToFolderResponse(folder);
    }

    private AccessLevel mapShareLevelToAccess(String shareLevel) {
        if (shareLevel == null) return AccessLevel.READ;
        switch (shareLevel.toUpperCase()) {
            case "READ": return AccessLevel.READ;
            case "WRITE": return AccessLevel.WRITE;
            case "ADMIN": return AccessLevel.ADMIN;
            default: return AccessLevel.READ;
        }
    }

    // ==================== Helper Methods ====================

    private boolean isDescendant(UUID ancestorId, UUID potentialDescendantId) {
        UUID currentId = potentialDescendantId;
        while (currentId != null) {
            if (currentId.equals(ancestorId)) {
                return true;
            }
            TestFolder folder = folderRepository.findById(currentId).orElse(null);
            if (folder == null) break;
            currentId = folder.getParentId();
        }
        return false;
    }

    private void updateDescendantPaths(TestFolder parent) {
        List<TestFolder> children = folderRepository.findByParentIdOrderBySortOrderAsc(parent.getId());
        for (TestFolder child : children) {
            child.setPath(parent.getPath() + "/" + child.getName());
            child.setDepth(parent.getDepth() + 1);
            folderRepository.save(child);
            updateDescendantPaths(child);
        }
    }

    private void deleteDescendants(UUID parentId) {
        List<TestFolder> children = folderRepository.findByParentIdOrderBySortOrderAsc(parentId);
        for (TestFolder child : children) {
            deleteDescendants(child.getId());
            folderAccessRepository.deleteByFolderId(child.getId());
            folderRepository.deleteById(child.getId());
        }
    }

    private FolderAnalyticsResponse calculateFolderAnalytics(TestFolder folder) {
        List<TestIssue> testsInFolder = testIssueRepository.findByFolderId(folder.getId());
        int totalTests = testsInFolder.size();
        int directTests = testsInFolder.size();

        // Count child folder tests
        List<UUID> allFolderIds = getAllChildFolderIds(folder.getId());
        for (UUID childId : allFolderIds) {
            totalTests += testIssueRepository.findByFolderId(childId).size();
        }

        // Calculate execution stats
        int passedTests = 0;
        int failedTests = 0;
        int blockedTests = 0;
        int notRunTests = 0;
        int flakyTests = 0;

        for (TestIssue test : testsInFolder) {
            List<TestExecution> executions = executionRepository.findByTestIdOrderByCreatedAtDesc(test.getId());
            if (!executions.isEmpty()) {
                TestExecution latestExecution = executions.get(0);
                String status = latestExecution.getStatus();

                switch (status) {
                    case "PASSED": passedTests++; break;
                    case "FAILED": failedTests++; break;
                    case "BLOCKED": blockedTests++; break;
                    case "FLAKY": flakyTests++; break;
                    default: notRunTests++; break;
                }
            } else {
                notRunTests++;
            }
        }

        double passRate = 0.0;
        int executedTests = passedTests + failedTests + blockedTests;
        if (executedTests > 0) {
            passRate = (double) passedTests / executedTests * 100.0;
        }

        double executionProgress = 0.0;
        if (totalTests > 0) {
            executionProgress = (double) (totalTests - notRunTests) / totalTests * 100.0;
        }

        double flakyRate = 0.0;
        if (executedTests > 0) {
            flakyRate = (double) flakyTests / executedTests * 100.0;
        }

        // Calculate health score
        double healthScore = 100.0;
        if (passRate < 50) healthScore -= 30;
        else if (passRate < 70) healthScore -= 15;
        else if (passRate < 80) healthScore -= 5;

        if (flakyRate > 20) healthScore -= 20;
        else if (flakyRate > 10) healthScore -= 10;
        else if (flakyRate > 5) healthScore -= 5;

        if (executionProgress < 50) healthScore -= 10;

        healthScore = Math.max(0, Math.min(100, healthScore));

        String healthStatus = healthScore >= 80 ? "HEALTHY" :
                healthScore >= 60 ? "WARNING" : "CRITICAL";

        List<String> issues = new ArrayList<>();
        if (flakyRate > healthFlakyRateWarning) issues.add("High flaky test rate detected");
        if (passRate < healthPassRateWarning) issues.add("Pass rate below acceptable threshold");
        if (executionProgress < 50) issues.add("Many tests have not been executed");
        if (allFolderIds.size() > 10) issues.add("Deep folder hierarchy may impact performance");

        return FolderAnalyticsResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getName())
                .projectId(folder.getProjectId())
                .path(folder.getPath())
                .totalTests(totalTests)
                .directTests(directTests)
                .childFolderCount(allFolderIds.size())
                .passedTests(passedTests)
                .failedTests(failedTests)
                .blockedTests(blockedTests)
                .notRunTests(notRunTests)
                .flakyTests(flakyTests)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .executionProgress(Math.round(executionProgress * 100.0) / 100.0)
                .flakyRate(Math.round(flakyRate * 100.0) / 100.0)
                .lastModifiedDate(folder.getUpdatedAt())
                .healthScore(Math.round(healthScore * 100.0) / 100.0)
                .healthStatus(healthStatus)
                .issues(issues)
                .build();
    }

    private List<UUID> getAllChildFolderIds(UUID parentId) {
        List<UUID> allIds = new ArrayList<>();
        Queue<UUID> toProcess = new LinkedList<>();
        toProcess.add(parentId);

        while (!toProcess.isEmpty()) {
            UUID currentId = toProcess.poll();
            List<TestFolder> children = folderRepository.findByParentIdOrderBySortOrderAsc(currentId);

            for (TestFolder child : children) {
                allIds.add(child.getId());
                toProcess.add(child.getId());
            }
        }

        return allIds;
    }

    private FolderResponse mapToFolderResponse(TestFolder folder) {
        List<TestFolder> children = folderRepository.findByParentIdOrderBySortOrderAsc(folder.getId());
        List<FolderResponse> childResponses = children.stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());

        long childCount = folderRepository.countChildren(folder.getId());
        int testCount = testIssueRepository.findByFolderId(folder.getId()).size();

        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .description(folder.getDescription())
                .projectId(folder.getProjectId())
                .parentId(folder.getParentId())
                .folderType(folder.getFolderType())
                .path(folder.getPath())
                .depth(folder.getDepth())
                .sortOrder(folder.getSortOrder())
                .status(folder.getStatus())
                .ownerId(folder.getOwnerId())
                .icon(folder.getIcon())
                .color(folder.getColor())
                .isStarred(folder.getIsStarred())
                .isExpanded(folder.getIsExpanded())
                .tags(folder.getTags())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .children(childResponses)
                .childCount((int) childCount)
                .testCount(testCount)
                .build();
    }

    private FolderTemplateResponse mapToTemplateResponse(TestFolderTemplate template) {
        return FolderTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .folderType(template.getFolderType())
                .icon(template.getIcon())
                .color(template.getColor())
                .tags(template.getTags())
                .isSystemTemplate(template.getIsSystemTemplate())
                .usageCount(template.getUsageCount())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .createdBy(template.getCreatedBy())
                .build();
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof UUID) {
            return (UUID) auth.getDetails();
        }
        return null;
    }
}
