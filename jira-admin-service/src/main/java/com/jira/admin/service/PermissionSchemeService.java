package com.jira.admin.service;

import com.jira.admin.dto.*;
import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.context.MessageSource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Permission Scheme Management Service.
 * Handles CRUD operations for permission schemes and grants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionSchemeService {

    private final PermissionSchemeRepository permissionSchemeRepository;
    private final PermissionSchemeGrantRepository permissionSchemeGrantRepository;
    private final AuditLogRepository auditLogRepository;
    private final MessageSource messageSource;

    // ==================== Permission Scheme CRUD ====================

    @Transactional(readOnly = true)
    public List<PermissionSchemeDto> getAllPermissionSchemes() {
        List<PermissionSchemeEntity> schemes = permissionSchemeRepository.findAll();
        return schemes.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PermissionSchemeDto> getPermissionSchemeById(String id) {
        return permissionSchemeRepository.findById(id)
                .map(this::mapToDto);
    }

    @Transactional
    public PermissionSchemeDto createPermissionScheme(CreatePermissionSchemeRequest request) {
        // Check if name already exists
        if (permissionSchemeRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.permission.scheme.name.exists", new Object[]{request.getName()}, Locale.ENGLISH));
        }

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetDefaultScheme();
        }

        PermissionSchemeEntity entity = PermissionSchemeEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions("[]")
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        entity = permissionSchemeRepository.save(entity);

        logAudit("CREATE", "PERMISSION_SCHEME", entity.getId(), entity.getName(),
                "Permission scheme created");

        return mapToDto(entity);
    }

    @Transactional
    public Optional<PermissionSchemeDto> updatePermissionScheme(String id, CreatePermissionSchemeRequest request) {
        Optional<PermissionSchemeEntity> existing = permissionSchemeRepository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        PermissionSchemeEntity entity = existing.get();

        // Check for name uniqueness if name changed
        if (!entity.getName().equals(request.getName())) {
            Optional<PermissionSchemeEntity> byName = permissionSchemeRepository.findByName(request.getName());
            if (byName.isPresent() && !byName.get().getId().equals(id)) {
                throw new IllegalArgumentException(
                    messageSource.getMessage("error.permission.scheme.name.exists", new Object[]{request.getName()}, Locale.ENGLISH));
            }
        }

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(entity.getIsDefault())) {
            unsetDefaultScheme();
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }

        entity = permissionSchemeRepository.save(entity);

        logAudit("UPDATE", "PERMISSION_SCHEME", entity.getId(), entity.getName(),
                "Permission scheme updated");

        return Optional.of(mapToDto(entity));
    }

    @Transactional
    public boolean deletePermissionScheme(String id) {
        Optional<PermissionSchemeEntity> existing = permissionSchemeRepository.findById(id);
        if (existing.isEmpty()) {
            return false;
        }

        PermissionSchemeEntity entity = existing.get();

        // Delete all grants for this scheme
        List<PermissionSchemeGrantEntity> grants = permissionSchemeGrantRepository.findByPermissionSchemeId(id);
        permissionSchemeGrantRepository.deleteAll(grants);

        // Delete the scheme
        permissionSchemeRepository.delete(entity);

        logAudit("DELETE", "PERMISSION_SCHEME", id, entity.getName(),
                "Permission scheme deleted");

        return true;
    }

    // ==================== Permission Grants ====================

    @Transactional(readOnly = true)
    public List<PermissionGrantDto> getGrantsForScheme(String schemeId) {
        List<PermissionSchemeGrantEntity> grants = permissionSchemeGrantRepository.findByPermissionSchemeId(schemeId);
        return grants.stream()
                .map(this::mapGrantToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<PermissionGrantDto> addPermissionGrant(String schemeId, CreatePermissionGrantRequest request) {
        // Verify scheme exists
        if (permissionSchemeRepository.findById(schemeId).isEmpty()) {
            return Optional.empty();
        }

        // Check if grant already exists
        if (permissionSchemeGrantRepository.existsByPermissionSchemeIdAndHolderTypeAndHolderId(
                schemeId, request.getHolderType(), request.getHolderId())) {
            // Check permission as well
            Optional<PermissionSchemeGrantEntity> existing = permissionSchemeGrantRepository
                    .findByPermissionSchemeIdAndPermissionIdAndHolderTypeAndHolderId(
                            schemeId, request.getPermissionId(), request.getHolderType(), request.getHolderId());
            if (existing.isPresent()) {
                throw new IllegalArgumentException(
                        messageSource.getMessage("error.permission.grant.exists", null, Locale.ENGLISH));
            }
        }

        PermissionSchemeGrantEntity grant = PermissionSchemeGrantEntity.builder()
                .permissionSchemeId(schemeId)
                .permissionId(request.getPermissionId())
                .holderType(request.getHolderType())
                .holderId(request.getHolderId())
                .createdAt(LocalDateTime.now())
                .build();

        grant = permissionSchemeGrantRepository.save(grant);

        logAudit("CREATE", "PERMISSION_SCHEME_GRANT", grant.getId(), schemeId,
                "Permission grant added: " + request.getHolderType() + "/" + request.getHolderId());

        return Optional.of(mapGrantToDto(grant));
    }

    @Transactional
    public boolean removePermissionGrant(String schemeId, String grantId) {
        Optional<PermissionSchemeGrantEntity> existing = permissionSchemeGrantRepository.findById(grantId);
        if (existing.isEmpty()) {
            return false;
        }

        PermissionSchemeGrantEntity grant = existing.get();

        // Verify the grant belongs to the specified scheme
        if (!grant.getPermissionSchemeId().equals(schemeId)) {
            return false;
        }

        permissionSchemeGrantRepository.delete(grant);

        logAudit("DELETE", "PERMISSION_SCHEME_GRANT", grantId, schemeId,
                "Permission grant removed");

        return true;
    }

    // ==================== Helper Methods ====================

    private void unsetDefaultScheme() {
        Optional<PermissionSchemeEntity> currentDefault = permissionSchemeRepository.findByIsDefaultTrue();
        currentDefault.ifPresent(scheme -> {
            scheme.setIsDefault(false);
            permissionSchemeRepository.save(scheme);
        });
    }

    private PermissionSchemeDto mapToDto(PermissionSchemeEntity entity) {
        List<PermissionSchemeGrantEntity> grants = permissionSchemeGrantRepository.findByPermissionSchemeId(entity.getId());
        List<String> permissionKeys = grants.stream()
                .map(PermissionSchemeGrantEntity::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        return PermissionSchemeDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .scope(null)  // Scope not in entity, would need separate field
                .isDefault(entity.getIsDefault())
                .permissions(permissionKeys)
                .build();
    }

    private PermissionGrantDto mapGrantToDto(PermissionSchemeGrantEntity entity) {
        return PermissionGrantDto.builder()
                .id(entity.getId())
                .permissionSchemeId(entity.getPermissionSchemeId())
                .permissionId(entity.getPermissionId())
                .holderType(entity.getHolderType())
                .holderId(entity.getHolderId())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .build();
    }

    private void logAudit(String action, String entityType, String entityId, String entityName, String description) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .details(description)
                .userId("SYSTEM") // populated by security context in production
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}