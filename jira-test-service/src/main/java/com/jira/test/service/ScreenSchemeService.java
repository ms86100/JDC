package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenSchemeService {

    private final ScreenSchemeRepository screenSchemeRepository;
    private final ScreenSchemeScreenRepository screenSchemeScreenRepository;
    private final ScreenRepository screenRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ScreenSchemeResponse createScreenScheme(UUID projectId, String name, String description) {
        log.info("Creating screen scheme: {} for project: {}", name, projectId);

        if (screenSchemeRepository.existsByProjectIdAndName(projectId, name)) {
            throw new DuplicateResourceException("Screen scheme with name '" + name + "' already exists in this project");
        }

        ScreenScheme scheme = ScreenScheme.builder()
                .projectId(projectId)
                .name(name)
                .description(description)
                .isDefault(false)
                .build();

        scheme = screenSchemeRepository.save(scheme);
        log.info("Screen scheme created with id: {}", scheme.getId());

        return mapToResponse(scheme);
    }

    @Transactional
    public ScreenSchemeResponse createScreenScheme(UUID projectId, String name, String description, boolean isDefault) {
        if (isDefault) {
            clearDefaultForProject(projectId);
        }

        ScreenScheme scheme = ScreenScheme.builder()
                .projectId(projectId)
                .name(name)
                .description(description)
                .isDefault(isDefault)
                .build();

        scheme = screenSchemeRepository.save(scheme);
        log.info("Screen scheme created with id: {} (default: {})", scheme.getId(), isDefault);

        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public ScreenSchemeResponse getScreenScheme(UUID schemeId) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));
        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public ScreenSchemeResponse getScreenScheme(UUID schemeId, UUID projectId) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        if (projectId != null && !scheme.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("ScreenScheme", "id", schemeId);
        }

        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public List<ScreenSchemeResponse> listScreenSchemes(UUID projectId) {
        return screenSchemeRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScreenSchemeResponse> listScreenSchemesWithDefault(UUID projectId) {
        List<ScreenScheme> schemes = screenSchemeRepository.findByProjectIdOrderByNameAsc(projectId);

        ScreenScheme defaultScheme = screenSchemeRepository.findByProjectIdAndIsDefaultTrue(projectId).orElse(null);

        List<ScreenSchemeResponse> responses = schemes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        if (defaultScheme != null) {
            responses.sort((a, b) -> {
                if (a.getId().equals(defaultScheme.getId())) return -1;
                if (b.getId().equals(defaultScheme.getId())) return 1;
                return a.getName().compareTo(b.getName());
            });
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public ScreenSchemeResponse getDefaultSchemeForProject(UUID projectId) {
        ScreenScheme defaultScheme = screenSchemeRepository.findByProjectIdAndIsDefaultTrue(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No default screen scheme found for project", "projectId", projectId));
        return mapToResponse(defaultScheme);
    }

    @Transactional(readOnly = true)
    public List<ScreenSchemeResponse> searchScreenSchemes(UUID projectId, String searchTerm) {
        return screenSchemeRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, searchTerm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScreenSchemeResponse addScreenToScheme(UUID schemeId, UUID screenId, String screenTypeStr) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        Screen.ScreenType screenType;
        try {
            screenType = Screen.ScreenType.valueOf(screenTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid screen type: " + screenTypeStr +
                    ". Valid types are: CREATE, EDIT, VIEW, SEARCH");
        }

        if (screenSchemeScreenRepository.existsByScreenSchemeIdAndScreenType(schemeId, screenType)) {
            throw new DuplicateResourceException("Screen of type '" + screenType + "' already exists in this scheme");
        }

        ScreenSchemeScreen mapping = ScreenSchemeScreen.builder()
                .screenSchemeId(schemeId)
                .screenId(screenId)
                .screenType(screenType)
                .build();

        screenSchemeScreenRepository.save(mapping);
        log.info("Screen {} added to scheme {} with type {}", screenId, schemeId, screenType);

        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public List<ScreenSchemeScreenResponse> getSchemeScreens(UUID schemeId) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        List<ScreenSchemeScreen> screens = screenSchemeScreenRepository.findByScreenSchemeId(schemeId);
        return screens.stream()
                .map(s -> {
                    Screen screen = screenRepository.findById(s.getScreenId()).orElse(null);
                    return ScreenSchemeScreenResponse.builder()
                            .id(s.getId())
                            .screenId(s.getScreenId())
                            .screenName(screen != null ? screen.getName() : null)
                            .screenType(s.getScreenType().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScreenSchemeScreenResponse getScreenForType(UUID schemeId, String screenTypeStr) {
        Screen.ScreenType screenType;
        try {
            screenType = Screen.ScreenType.valueOf(screenTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid screen type: " + screenTypeStr);
        }

        ScreenSchemeScreen screenMapping = screenSchemeScreenRepository
                .findByScreenSchemeIdAndScreenType(schemeId, screenType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No screen of type '" + screenType + "' found in scheme", "schemeId", schemeId));

        Screen screen = screenRepository.findById(screenMapping.getScreenId()).orElse(null);

        return ScreenSchemeScreenResponse.builder()
                .id(screenMapping.getId())
                .screenId(screenMapping.getScreenId())
                .screenName(screen != null ? screen.getName() : null)
                .screenType(screenMapping.getScreenType().name())
                .build();
    }

    @Transactional(readOnly = true)
    public ScreenSchemeValidationResult validateSchemeConfiguration(UUID schemeId) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        List<ScreenSchemeScreen> screens = screenSchemeScreenRepository.findByScreenSchemeId(schemeId);
        Set<Screen.ScreenType> configuredTypes = screens.stream()
                .map(ScreenSchemeScreen::getScreenType)
                .collect(Collectors.toSet());

        for (Screen.ScreenType type : Screen.ScreenType.values()) {
            if (!configuredTypes.contains(type)) {
                warnings.add("Screen scheme does not have a screen configured for type: " + type.name());
            }
        }

        if (scheme.getIsDefault()) {
            warnings.add("This scheme is the default scheme for the project");
        }

        if (screens.isEmpty()) {
            errors.add("Screen scheme has no screens configured");
        }

        return ScreenSchemeValidationResult.builder()
                .schemeId(schemeId)
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    @Transactional
    public ScreenSchemeScreen removeScreenFromScheme(UUID schemeId, UUID screenId) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        ScreenSchemeScreen mapping = screenSchemeScreenRepository.findByScreenSchemeIdAndScreenId(schemeId, screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found in scheme", "screenId", screenId));

        screenSchemeScreenRepository.delete(mapping);
        log.info("Screen {} removed from scheme {}", screenId, schemeId);

        return mapping;
    }

    @Transactional
    public ScreenSchemeResponse setDefaultScheme(UUID schemeId) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        clearDefaultForProject(scheme.getProjectId());

        scheme.setIsDefault(true);
        scheme = screenSchemeRepository.save(scheme);
        log.info("Screen scheme {} set as default for project {}", schemeId, scheme.getProjectId());

        return mapToResponse(scheme);
    }

    @Transactional
    public ScreenSchemeResponse cloneScheme(UUID sourceId, String newName) {
        return cloneScheme(sourceId, newName, null);
    }

    @Transactional
    public ScreenSchemeResponse cloneScheme(UUID sourceId, String newName, UUID targetProjectId) {
        ScreenScheme source = screenSchemeRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", sourceId));

        UUID projectId = targetProjectId != null ? targetProjectId : source.getProjectId();

        if (screenSchemeRepository.existsByProjectIdAndName(projectId, newName)) {
            throw new DuplicateResourceException("Screen scheme with name '" + newName + "' already exists in this project");
        }

        ScreenScheme cloned = ScreenScheme.builder()
                .projectId(projectId)
                .name(newName)
                .description(source.getDescription())
                .isDefault(false)
                .build();

        cloned = screenSchemeRepository.save(cloned);

        List<ScreenSchemeScreen> sourceScreens = screenSchemeScreenRepository.findByScreenSchemeId(sourceId);
        for (ScreenSchemeScreen sourceScreen : sourceScreens) {
            ScreenSchemeScreen clonedScreen = ScreenSchemeScreen.builder()
                    .screenSchemeId(cloned.getId())
                    .screenId(sourceScreen.getScreenId())
                    .screenType(sourceScreen.getScreenType())
                    .build();
            screenSchemeScreenRepository.save(clonedScreen);
        }

        log.info("Screen scheme {} cloned to {} with id {} in project {}", sourceId, newName, cloned.getId(), projectId);
        return mapToResponse(cloned);
    }

    @Transactional
    public List<ScreenSchemeResponse> cloneSchemesBulk(List<UUID> sourceIds, UUID targetProjectId) {
        List<ScreenSchemeResponse> clonedSchemes = new ArrayList<>();

        for (UUID sourceId : sourceIds) {
            ScreenScheme source = screenSchemeRepository.findById(sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", sourceId));

            String newName = source.getName() + " (Copy)";
            int counter = 1;
            while (screenSchemeRepository.existsByProjectIdAndName(targetProjectId, newName)) {
                counter++;
                newName = source.getName() + " (Copy " + counter + ")";
            }

            clonedSchemes.add(cloneScheme(sourceId, newName, targetProjectId));
        }

        log.info("Bulk cloned {} screen schemes to project {}", sourceIds.size(), targetProjectId);
        return clonedSchemes;
    }

    @Transactional
    public List<ScreenSchemeScreen> updateSchemeScreens(UUID schemeId, List<ScreenSchemeScreenUpdate> updates) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        List<ScreenSchemeScreen> updatedMappings = new ArrayList<>();

        for (ScreenSchemeScreenUpdate update : updates) {
            Screen.ScreenType screenType;
            try {
                screenType = Screen.ScreenType.valueOf(update.getScreenType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid screen type: " + update.getScreenType());
            }

            Optional<ScreenSchemeScreen> existing = screenSchemeScreenRepository
                    .findByScreenSchemeIdAndScreenType(schemeId, screenType);

            if (existing.isPresent()) {
                ScreenSchemeScreen mapping = existing.get();
                if (update.getScreenId() != null) {
                    mapping.setScreenId(update.getScreenId());
                }
                updatedMappings.add(screenSchemeScreenRepository.save(mapping));
            } else {
                ScreenSchemeScreen newMapping = ScreenSchemeScreen.builder()
                        .screenSchemeId(schemeId)
                        .screenId(update.getScreenId())
                        .screenType(screenType)
                        .build();
                updatedMappings.add(screenSchemeScreenRepository.save(newMapping));
            }
        }

        log.info("Updated {} screen assignments for scheme {}", updatedMappings.size(), schemeId);
        return updatedMappings;
    }

    @Transactional
    public ScreenSchemeResponse updateScreenScheme(UUID schemeId, String name, String description) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        if (name != null && !name.equals(scheme.getName())) {
            if (screenSchemeRepository.existsByProjectIdAndName(scheme.getProjectId(), name)) {
                throw new DuplicateResourceException("Screen scheme with name '" + name + "' already exists");
            }
            scheme.setName(name);
        }

        if (description != null) {
            scheme.setDescription(description);
        }

        scheme = screenSchemeRepository.save(scheme);
        log.info("Screen scheme updated: {}", schemeId);

        return mapToResponse(scheme);
    }

    @Transactional
    public void deleteScreenScheme(UUID schemeId) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        screenSchemeScreenRepository.deleteByScreenSchemeId(schemeId);
        screenSchemeRepository.delete(scheme);
        log.info("Screen scheme deleted: {}", schemeId);
    }

    private void clearDefaultForProject(UUID projectId) {
        screenSchemeRepository.findByProjectIdAndIsDefaultTrue(projectId)
                .ifPresent(scheme -> {
                    scheme.setIsDefault(false);
                    screenSchemeRepository.save(scheme);
                });
    }

    private ScreenSchemeResponse mapToResponse(ScreenScheme scheme) {
        List<ScreenSchemeScreen> screens = screenSchemeScreenRepository.findByScreenSchemeId(scheme.getId());

        List<ScreenSchemeScreenResponse> screenResponses = screens.stream()
                .map(s -> {
                    Screen screen = screenRepository.findById(s.getScreenId()).orElse(null);
                    return ScreenSchemeScreenResponse.builder()
                            .id(s.getId())
                            .screenId(s.getScreenId())
                            .screenName(screen != null ? screen.getName() : null)
                            .screenType(s.getScreenType().name())
                            .build();
                })
                .collect(Collectors.toList());

        return ScreenSchemeResponse.builder()
                .id(scheme.getId())
                .projectId(scheme.getProjectId())
                .name(scheme.getName())
                .description(scheme.getDescription())
                .isDefault(scheme.getIsDefault())
                .screens(screenResponses)
                .createdAt(scheme.getCreatedAt())
                .updatedAt(scheme.getUpdatedAt())
                .build();
    }

    @Transactional
    public ScreenSchemeResponse setProjectDefaultScheme(UUID projectId, UUID schemeId) {
        if (!screenSchemeRepository.existsById(schemeId)) {
            throw new ResourceNotFoundException("ScreenScheme", "id", schemeId);
        }

        ScreenScheme scheme = screenSchemeRepository.findById(schemeId).get();
        if (!scheme.getProjectId().equals(projectId)) {
            throw new ValidationException("Screen scheme does not belong to the specified project");
        }

        clearDefaultForProject(projectId);

        scheme.setIsDefault(true);
        scheme = screenSchemeRepository.save(scheme);
        log.info("Screen scheme {} set as default for project {}", schemeId, projectId);

        return mapToResponse(scheme);
    }

    @Transactional
    public List<ScreenSchemeResponse> createDefaultSchemesForProject(UUID projectId) {
        List<ScreenSchemeResponse> createdSchemes = new ArrayList<>();

        ScreenScheme defaultScheme = ScreenScheme.builder()
                .projectId(projectId)
                .name("Default Issue Screen Scheme")
                .description("Default screen scheme with standard issue screens")
                .isDefault(true)
                .build();
        defaultScheme = screenSchemeRepository.save(defaultScheme);
        createdSchemes.add(mapToResponse(defaultScheme));

        for (Screen.ScreenType screenType : Screen.ScreenType.values()) {
            Screen screen = Screen.builder()
                    .name("Default " + screenType.name() + " Screen")
                    .screenType(screenType)
                    .position(screenType.ordinal())
                    .build();
            screen = screenRepository.save(screen);

            ScreenSchemeScreen mapping = ScreenSchemeScreen.builder()
                    .screenSchemeId(defaultScheme.getId())
                    .screenId(screen.getId())
                    .screenType(screenType)
                    .build();
            screenSchemeScreenRepository.save(mapping);
        }

        log.info("Created default screen scheme with all screen types for project {}", projectId);
        return createdSchemes;
    }

    @Transactional(readOnly = true)
    public ScreenSchemeUsageReport getSchemeUsage(UUID schemeId) {
        ScreenScheme scheme = screenSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("ScreenScheme", "id", schemeId));

        return ScreenSchemeUsageReport.builder()
                .schemeId(schemeId)
                .schemeName(scheme.getName())
                .projectId(scheme.getProjectId())
                .isDefault(scheme.getIsDefault())
                .screenCount(screenSchemeScreenRepository.findByScreenSchemeId(schemeId).size())
                .build();
    }
}