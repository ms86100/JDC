package com.jira.admin.service;

import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrioritySchemeService {

    private final PrioritySchemeRepository prioritySchemeRepository;
    private final PrioritySchemeItemRepository prioritySchemeItemRepository;
    private final ProjectPrioritySchemeRepository projectPrioritySchemeRepository;
    private final PriorityRepository priorityRepository;
    private final MessageSource messageSource;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSchemes() {
        return prioritySchemeRepository.findAll().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getSchemeById(String id) {
        return prioritySchemeRepository.findById(id).map(this::toMap);
    }

    @Transactional
    public PrioritySchemeEntity createScheme(PrioritySchemeEntity scheme) {
        if (Boolean.TRUE.equals(scheme.getIsDefault())) {
            unsetDefaultScheme();
        }
        return prioritySchemeRepository.save(scheme);
    }

    @Transactional
    public Optional<PrioritySchemeEntity> updateScheme(String id, PrioritySchemeEntity updates) {
        return prioritySchemeRepository.findById(id).map(existing -> {
            existing.setName(updates.getName());
            existing.setDescription(updates.getDescription());
            if (Boolean.TRUE.equals(updates.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
                unsetDefaultScheme();
            }
            existing.setIsDefault(updates.getIsDefault());
            return prioritySchemeRepository.save(existing);
        });
    }

    @Transactional
    public boolean deleteScheme(String id) {
        if (!prioritySchemeRepository.existsById(id)) {
            return false;
        }
        prioritySchemeItemRepository.deleteBySchemeId(id);
        List<ProjectPrioritySchemeEntity> assignments = projectPrioritySchemeRepository.findBySchemeId(id);
        projectPrioritySchemeRepository.deleteAll(assignments);
        prioritySchemeRepository.deleteById(id);
        return true;
    }

    @Transactional
    public PrioritySchemeEntity assignSchemeToProject(String projectId, String schemeId) {
        PrioritySchemeEntity scheme = prioritySchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Priority scheme not found: " + schemeId));

        Optional<ProjectPrioritySchemeEntity> existing = projectPrioritySchemeRepository.findByProjectId(projectId);
        if (existing.isPresent()) {
            ProjectPrioritySchemeEntity assignment = existing.get();
            assignment.setSchemeId(schemeId);
            projectPrioritySchemeRepository.save(assignment);
        } else {
            ProjectPrioritySchemeEntity assignment = ProjectPrioritySchemeEntity.builder()
                    .projectId(projectId)
                    .schemeId(schemeId)
                    .build();
            projectPrioritySchemeRepository.save(assignment);
        }

        log.info("Assigned priority scheme {} to project {}", schemeId, projectId);
        return scheme;
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getProjectPriorityScheme(String projectId) {
        return projectPrioritySchemeRepository.findByProjectId(projectId)
                .flatMap(assignment -> prioritySchemeRepository.findById(assignment.getSchemeId()))
                .map(this::toMap);
    }

    @Transactional
    public List<PrioritySchemeItemEntity> setSchemeItems(String schemeId, List<PrioritySchemeItemEntity> items) {
        if (!prioritySchemeRepository.existsById(schemeId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.priority.scheme.not.found", new Object[]{schemeId}, Locale.ENGLISH));
        }
        prioritySchemeItemRepository.deleteBySchemeId(schemeId);
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSchemeId(schemeId);
            items.get(i).setPosition(i);
        }
        return prioritySchemeItemRepository.saveAll(items);
    }

    @Transactional
    public void seedDefaultScheme() {
        if (prioritySchemeRepository.findByIsDefaultTrue().isPresent()) {
            log.debug("Default priority scheme already exists, skipping seed");
            return;
        }

        PrioritySchemeEntity defaultScheme = PrioritySchemeEntity.builder()
                .name("Default Priority Scheme")
                .description("Default scheme containing all standard priorities")
                .isDefault(true)
                .build();
        defaultScheme = prioritySchemeRepository.save(defaultScheme);

        List<PriorityEntity> allPriorities = priorityRepository.findAll();
        int position = 0;
        for (PriorityEntity priority : allPriorities) {
            PrioritySchemeItemEntity item = PrioritySchemeItemEntity.builder()
                    .schemeId(defaultScheme.getId())
                    .priorityId(priority.getId())
                    .position(position++)
                    .build();
            prioritySchemeItemRepository.save(item);
        }

        log.info("Seeded default priority scheme with {} priorities", allPriorities.size());
    }

    private void unsetDefaultScheme() {
        prioritySchemeRepository.findByIsDefaultTrue().ifPresent(scheme -> {
            scheme.setIsDefault(false);
            prioritySchemeRepository.save(scheme);
        });
    }

    private Map<String, Object> toMap(PrioritySchemeEntity scheme) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", scheme.getId());
        map.put("name", scheme.getName());
        map.put("description", scheme.getDescription());
        map.put("isDefault", scheme.getIsDefault());
        map.put("createdAt", scheme.getCreatedAt());
        map.put("updatedAt", scheme.getUpdatedAt());

        List<PrioritySchemeItemEntity> items =
                prioritySchemeItemRepository.findBySchemeIdOrderByPositionAsc(scheme.getId());
        map.put("priorities", items);

        List<ProjectPrioritySchemeEntity> assignments =
                projectPrioritySchemeRepository.findBySchemeId(scheme.getId());
        map.put("projectIds", assignments.stream()
                .map(ProjectPrioritySchemeEntity::getProjectId)
                .collect(Collectors.toList()));

        return map;
    }
}
