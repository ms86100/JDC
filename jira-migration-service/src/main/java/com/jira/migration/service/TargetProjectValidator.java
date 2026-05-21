package com.jira.migration.service;

import com.jira.migration.exception.EntityNotFoundException;
import com.jira.migration.service.clients.ProjectServiceClient;
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
        } catch (Exception e) {
            log.warn("Target project {} not found: {}", projectId, e.getMessage());
            throw new EntityNotFoundException("Project", projectId.toString());
        }
    }
}
