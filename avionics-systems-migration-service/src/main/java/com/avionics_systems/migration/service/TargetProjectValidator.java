package com.avionics_systems.migration.service;

import com.avionics_systems.migration.exception.EntityNotFoundException;
import com.avionics_systems.migration.service.clients.ProjectServiceClient;
import com.avionics_systems.migration.service.clients.ServiceClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Ensures target project exists before import (P4-03).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TargetProjectValidator {

    private final ProjectServiceClient projectServiceClient;

    public void assertProjectExists(UUID projectId) {
        if (projectId == null) {
            return;
        }
        try {
            projectServiceClient.getProject(projectId.toString());
        } catch (ServiceClientException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Target project {} not found: {}", projectId, e.getMessage());
                throw new EntityNotFoundException("Project", projectId.toString());
            }
            log.warn("Could not verify target project {} (service returned status {}): {}. Proceeding with import.",
                    projectId, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("Could not verify target project {} (service may be unavailable): {}. Proceeding with import.",
                    projectId, e.getMessage());
        }
    }
}
