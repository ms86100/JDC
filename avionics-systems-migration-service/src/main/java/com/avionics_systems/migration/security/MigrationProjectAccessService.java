package com.avionics_systems.migration.security;

import com.avionics_systems.migration.exception.EntityNotFoundException;
import com.avionics_systems.migration.service.TargetProjectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Project-scoped authorization for migration write operations (MG-P0-4).
 */
@Service
@RequiredArgsConstructor
public class MigrationProjectAccessService {

    private final TargetProjectValidator targetProjectValidator;

    public void assertProjectAccess(UUID projectId, String migrationRole) {
        if (projectId == null) {
            return;
        }
        try {
            targetProjectValidator.assertProjectExists(projectId);
        } catch (EntityNotFoundException e) {
            throw new SecurityException("Unknown target project: " + projectId);
        }
        if ("MIGRATION_ADMIN".equals(migrationRole)) {
            return;
        }
        if ("MIGRATION_OPERATOR".equals(migrationRole)) {
            return;
        }
        throw new SecurityException("Insufficient migration role for project " + projectId);
    }

    public void requireProjectHeaderForImport(UUID projectId, String migrationRole) {
        // Project CSV / bootstrap imports create projects and may omit targetProjectId.
        if (projectId == null) {
            return;
        }
        assertProjectAccess(projectId, migrationRole);
    }
}
