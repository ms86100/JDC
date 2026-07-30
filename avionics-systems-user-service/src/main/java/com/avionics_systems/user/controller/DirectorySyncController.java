package com.avionics_systems.user.controller;

import com.avionics_systems.user.dto.DirectoryLdapConfigRequest;
import com.avionics_systems.user.dto.DirectoryLdapConfigResponse;
import com.avionics_systems.user.dto.DirectorySyncLogResponse;
import com.avionics_systems.user.dto.DirectorySyncRequest;
import com.avionics_systems.user.entity.Directory;
import com.avionics_systems.user.entity.DirectorySyncLog;
import com.avionics_systems.user.exception.ResourceNotFoundException;
import com.avionics_systems.user.repository.DirectoryRepository;
import com.avionics_systems.user.repository.DirectorySyncLogRepository;
import com.avionics_systems.user.service.LdapSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/directories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Directory Sync", description = "LDAP/AD directory synchronization endpoints")
public class DirectorySyncController {

    private final LdapSyncService ldapSyncService;
    private final DirectoryRepository directoryRepository;
    private final DirectorySyncLogRepository syncLogRepository;

    @PostMapping("/{id}/sync")
    @Operation(summary = "Trigger manual LDAP sync for a directory")
    public ResponseEntity<DirectorySyncLogResponse> triggerSync(
            @PathVariable UUID id,
            @RequestBody(required = false) DirectorySyncRequest request) {

        DirectorySyncLog syncLog = ldapSyncService.syncDirectory(id);
        return ResponseEntity.ok(toSyncLogResponse(syncLog));
    }

    @GetMapping("/{id}/sync-log")
    @Operation(summary = "Get sync history for a directory")
    public ResponseEntity<Page<DirectorySyncLogResponse>> getSyncLog(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        directoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Directory not found: " + id));

        Page<DirectorySyncLog> logs = syncLogRepository
                .findByDirectoryIdOrderByStartedAtDesc(id, PageRequest.of(page, size));

        return ResponseEntity.ok(logs.map(this::toSyncLogResponse));
    }

    @PutMapping("/{id}/ldap-config")
    @Operation(summary = "Update LDAP configuration for a directory")
    public ResponseEntity<DirectoryLdapConfigResponse> updateLdapConfig(
            @PathVariable UUID id,
            @Valid @RequestBody DirectoryLdapConfigRequest request) {

        Directory directory = directoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Directory not found: " + id));

        directory.setServerUrl(request.getServerUrl());
        directory.setBaseDn(request.getBaseDn());
        directory.setBindDn(request.getBindDn());

        if (request.getBindPassword() != null && !request.getBindPassword().isBlank()) {
            directory.setEncryptedBindPassword(request.getBindPassword());
        }

        if (request.getUserSearchBase() != null) {
            directory.setUserSearchBase(request.getUserSearchBase());
        }
        if (request.getUserSearchFilter() != null) {
            directory.setUserSearchFilter(request.getUserSearchFilter());
        }
        if (request.getGroupSearchBase() != null) {
            directory.setGroupSearchBase(request.getGroupSearchBase());
        }
        if (request.getGroupSearchFilter() != null) {
            directory.setGroupSearchFilter(request.getGroupSearchFilter());
        }
        if (request.getSyncIntervalMinutes() != null) {
            directory.setSyncIntervalMinutes(request.getSyncIntervalMinutes());
        }

        directory = directoryRepository.save(directory);
        log.info("Updated LDAP config for directory: {} ({})", directory.getDirectoryName(), id);

        return ResponseEntity.ok(toLdapConfigResponse(directory));
    }

    @GetMapping("/{id}/ldap-config")
    @Operation(summary = "Get LDAP configuration for a directory")
    public ResponseEntity<DirectoryLdapConfigResponse> getLdapConfig(@PathVariable UUID id) {

        Directory directory = directoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Directory not found: " + id));

        return ResponseEntity.ok(toLdapConfigResponse(directory));
    }

    private DirectorySyncLogResponse toSyncLogResponse(DirectorySyncLog log) {
        return DirectorySyncLogResponse.builder()
                .id(log.getId())
                .directoryId(log.getDirectoryId())
                .startedAt(log.getStartedAt())
                .completedAt(log.getCompletedAt())
                .usersAdded(log.getUsersAdded())
                .usersUpdated(log.getUsersUpdated())
                .usersRemoved(log.getUsersRemoved())
                .groupsSynced(log.getGroupsSynced())
                .status(log.getStatus())
                .errors(log.getErrors())
                .build();
    }

    private DirectoryLdapConfigResponse toLdapConfigResponse(Directory directory) {
        return DirectoryLdapConfigResponse.builder()
                .directoryId(directory.getId())
                .directoryName(directory.getDirectoryName())
                .serverUrl(directory.getServerUrl())
                .baseDn(directory.getBaseDn())
                .bindDn(directory.getBindDn())
                .userSearchBase(directory.getUserSearchBase())
                .userSearchFilter(directory.getUserSearchFilter())
                .groupSearchBase(directory.getGroupSearchBase())
                .groupSearchFilter(directory.getGroupSearchFilter())
                .syncIntervalMinutes(directory.getSyncIntervalMinutes())
                .lastSyncAt(directory.getLastSyncAt())
                .syncStatus(directory.getSyncStatus())
                .build();
    }
}
