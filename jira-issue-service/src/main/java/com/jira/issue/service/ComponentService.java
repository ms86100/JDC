package com.jira.issue.service;

import com.jira.issue.dto.ComponentResponse;
import com.jira.issue.dto.CreateComponentRequest;
import com.jira.issue.dto.UpdateComponentRequest;
import com.jira.issue.entity.ProjectComponent;
import com.jira.issue.exception.DuplicateResourceException;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.ProjectComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComponentService {

    private final ProjectComponentRepository componentRepository;

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public ComponentResponse createComponent(CreateComponentRequest request) {
        log.info("Creating component '{}' for project: {}", request.getName(), request.getProjectId());

        // Verify project exists via REST
        if (!projectExists(request.getProjectId())) {
            throw new ResourceNotFoundException("Project", "id", request.getProjectId());
        }

        // Check for duplicate name
        if (componentRepository.existsByProjectIdAndName(request.getProjectId(), request.getName())) {
            throw new DuplicateResourceException("Component with name '" + request.getName() + "' already exists in this project");
        }

        ProjectComponent component = ProjectComponent.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .leadId(request.getLeadId())
                .assigneeType(request.getAssigneeType() != null ? request.getAssigneeType() : ProjectComponent.ASSIGNEE_TYPE_PROJECT_LEAD)
                .defaultAssigneeId(request.getDefaultAssigneeId())
                .isAssigneeTypeEnabled(request.getIsAssigneeTypeEnabled() != null ? request.getIsAssigneeTypeEnabled() : false)
                .build();

        component = componentRepository.save(component);
        log.info("Component created successfully: {} ({})", component.getName(), component.getId());

        return mapToComponentResponse(component);
    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> getComponentsForProject(UUID projectId) {
        log.debug("Fetching components for project: {}", projectId);

        List<ProjectComponent> components = componentRepository.findByProjectId(projectId);

        return components.stream()
                .map(this::mapToComponentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComponentResponse getComponent(UUID componentId) {
        ProjectComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));
        return mapToComponentResponse(component);
    }

    @Transactional
    public ComponentResponse updateComponent(UUID componentId, UpdateComponentRequest request) {
        log.info("Updating component: {}", componentId);

        ProjectComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        if (request.getName() != null) {
            // Check for duplicate name (excluding current component)
            componentRepository.findByProjectIdAndName(component.getProjectId(), request.getName())
                    .filter(c -> !c.getId().equals(componentId))
                    .ifPresent(c -> { throw new DuplicateResourceException("Component with name '" + request.getName() + "' already exists"); });
            component.setName(request.getName());
        }
        if (request.getDescription() != null) {
            component.setDescription(request.getDescription());
        }
        if (request.getLeadId() != null) {
            component.setLeadId(request.getLeadId());
        }
        if (request.getAssigneeType() != null) {
            component.setAssigneeType(request.getAssigneeType());
        }
        if (request.getDefaultAssigneeId() != null) {
            component.setDefaultAssigneeId(request.getDefaultAssigneeId());
        }
        if (request.getIsAssigneeTypeEnabled() != null) {
            component.setIsAssigneeTypeEnabled(request.getIsAssigneeTypeEnabled());
        }

        component = componentRepository.save(component);
        log.info("Component updated successfully: {}", componentId);

        return mapToComponentResponse(component);
    }

    @Transactional
    public void deleteComponent(UUID componentId) {
        log.info("Deleting component: {}", componentId);

        if (!componentRepository.existsById(componentId)) {
            throw new ResourceNotFoundException("Component", "id", componentId);
        }

        componentRepository.deleteById(componentId);
        log.info("Component deleted successfully: {}", componentId);
    }

    private ComponentResponse mapToComponentResponse(ProjectComponent component) {
        return ComponentResponse.builder()
                .id(component.getId())
                .projectId(component.getProjectId())
                .name(component.getName())
                .description(component.getDescription())
                .leadId(component.getLeadId())
                .assigneeType(component.getAssigneeType())
                .defaultAssigneeId(component.getDefaultAssigneeId())
                .isAssigneeTypeEnabled(component.getIsAssigneeTypeEnabled())
                .createdAt(component.getCreatedAt())
                .updatedAt(component.getUpdatedAt())
                .build();
    }

    private boolean projectExists(UUID projectId) {
        try {
            String url = String.format("%s/api/projects/%s", projectServiceUrl, projectId);
            restTemplate.getForEntity(url, Object.class);
            return true;
        } catch (Exception e) {
            log.warn("Project check failed for {}: {}", projectId, e.getMessage());
            return false;
        }
    }
}