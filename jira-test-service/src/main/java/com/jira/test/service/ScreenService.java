package com.jira.test.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final ScreenFieldRepository screenFieldRepository;
    private final CustomFieldRepository customFieldRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_FIELDS_PER_SCREEN = 100;
    private static final int MAX_POSITION_VALUE = 10000;

    @Transactional
    public ScreenResponse createScreen(String name, String screenTypeStr) {
        log.info("Creating screen: {} with type: {}", name, screenTypeStr);

        Screen.ScreenType screenType;
        try {
            screenType = Screen.ScreenType.valueOf(screenTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid screen type: " + screenTypeStr +
                    ". Valid types are: CREATE, EDIT, VIEW, SEARCH");
        }

        Screen screen = Screen.builder()
                .name(name)
                .screenType(screenType)
                .position(0)
                .build();

        screen = screenRepository.save(screen);
        log.info("Screen created with id: {}", screen.getId());

        return mapToResponse(screen);
    }

    @Transactional
    public ScreenResponse createScreen(String name, String screenTypeStr, Integer position) {
        Screen.ScreenType screenType;
        try {
            screenType = Screen.ScreenType.valueOf(screenTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid screen type: " + screenTypeStr +
                    ". Valid types are: CREATE, EDIT, VIEW, SEARCH");
        }

        Screen screen = Screen.builder()
                .name(name)
                .screenType(screenType)
                .position(position != null ? position : 0)
                .build();

        screen = screenRepository.save(screen);
        log.info("Screen created with id: {} and position: {}", screen.getId(), screen.getPosition());

        return mapToResponse(screen);
    }

    @Transactional(readOnly = true)
    public ScreenResponse getScreen(UUID screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));
        return mapToResponse(screen);
    }

    @Transactional
    public ScreenFieldResponse addField(UUID screenId, UUID fieldId, Integer position, Boolean isRequired) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        if (screenFieldRepository.existsByScreenIdAndFieldId(screenId, fieldId)) {
            throw new DuplicateResourceException("Field already exists on this screen");
        }

        int fieldPosition = position != null ? position :
                screenFieldRepository.findMaxPositionByScreenId(screenId).orElse(-1) + 1;

        if (position != null) {
            screenFieldRepository.incrementPositionsFrom(screenId, position);
        }

        ScreenField screenField = ScreenField.builder()
                .screenId(screenId)
                .fieldId(fieldId)
                .position(fieldPosition)
                .isRequired(isRequired != null ? isRequired : false)
                .isEditable(true)
                .isVisible(true)
                .build();

        screenField = screenFieldRepository.save(screenField);
        log.info("Field {} added to screen {} at position {}", fieldId, screenId, fieldPosition);

        return mapToFieldResponse(screenField);
    }

    @Transactional
    public ScreenFieldResponse updateField(UUID screenId, UUID fieldId, Boolean isRequired, Boolean isEditable, Boolean isVisible) {
        ScreenField screenField = screenFieldRepository.findByScreenIdAndFieldId(screenId, fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Field not found on screen", "screenId", screenId));

        if (isRequired != null) screenField.setIsRequired(isRequired);
        if (isEditable != null) screenField.setIsEditable(isEditable);
        if (isVisible != null) screenField.setIsVisible(isVisible);

        screenField = screenFieldRepository.save(screenField);
        log.info("Field {} updated on screen {}", fieldId, screenId);

        return mapToFieldResponse(screenField);
    }

    @Transactional
    public void removeField(UUID screenId, UUID fieldId) {
        ScreenField screenField = screenFieldRepository.findByScreenIdAndFieldId(screenId, fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Field not found on screen", "screenId", screenId));

        screenFieldRepository.delete(screenField);
        screenFieldRepository.decrementPositionsAbove(screenId, screenField.getPosition());

        log.info("Field {} removed from screen {}", fieldId, screenId);
    }

    @Transactional
    public List<ScreenFieldResponse> reorderFields(UUID screenId, List<UUID> fieldOrder) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        List<ScreenField> fields = screenFieldRepository.findByScreenIdOrderByPositionAsc(screenId);

        if (fieldOrder.size() != fields.size()) {
            throw new ValidationException("Field order size must match current field count");
        }

        Set<UUID> fieldIdSet = new HashSet<>(fieldOrder);
        Set<UUID> currentFieldIds = fields.stream()
                .map(ScreenField::getFieldId)
                .collect(Collectors.toSet());

        if (!fieldIdSet.equals(currentFieldIds)) {
            throw new ValidationException("Field order must contain exactly the same fields as the screen");
        }

        List<ScreenField> updatedFields = new ArrayList<>();
        for (int i = 0; i < fieldOrder.size(); i++) {
            UUID fieldId = fieldOrder.get(i);
            final int newPosition = i;

            ScreenField field = fields.stream()
                    .filter(f -> f.getFieldId().equals(fieldId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Field", "id", fieldId));

            field.setPosition(newPosition);
            updatedFields.add(screenFieldRepository.save(field));
        }

        log.info("Fields reordered on screen {}", screenId);
        return updatedFields.stream()
                .map(this::mapToFieldResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScreenFieldResponse> getFieldsForScreen(UUID screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        return screenFieldRepository.findByScreenIdOrderByPositionAsc(screenId).stream()
                .map(this::mapToFieldResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> listScreens() {
        return screenRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> listScreensByType(String screenTypeStr) {
        Screen.ScreenType screenType;
        try {
            screenType = Screen.ScreenType.valueOf(screenTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid screen type: " + screenTypeStr);
        }

        return screenRepository.findByScreenTypeOrderByNameAsc(screenType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> searchScreens(String searchTerm) {
        return screenRepository.findByNameContainingIgnoreCase(searchTerm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScreenConfigurationPreview getScreenPreview(UUID screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        List<ScreenField> fields = screenFieldRepository.findByScreenIdOrderByPositionAsc(screenId);

        List<ScreenFieldPreview> fieldPreviews = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (ScreenField field : fields) {
            CustomField customField = customFieldRepository.findById(field.getFieldId()).orElse(null);

            String fieldName = customField != null ? customField.getName() : "Unknown Field (" + field.getFieldId() + ")";
            String fieldType = customField != null ? customField.getFieldType().name() : "UNKNOWN";

            fieldPreviews.add(ScreenFieldPreview.builder()
                    .fieldId(field.getFieldId())
                    .fieldName(fieldName)
                    .fieldType(fieldType)
                    .position(field.getPosition())
                    .isRequired(field.getIsRequired())
                    .isEditable(field.getIsEditable())
                    .isVisible(field.getIsVisible())
                    .build());

            if (!field.getIsVisible() && field.getIsRequired()) {
                warnings.add("Field '" + fieldName + "' is required but not visible");
            }

            if (customField != null && field.getIsRequired() &&
                (customField.getFieldType() == CustomField.FieldType.TEXT ||
                 customField.getFieldType() == CustomField.FieldType.TEXTAREA)) {
                if (customField.getDefaultValue() == null || customField.getDefaultValue().isEmpty()) {
                    warnings.add("Required text field '" + fieldName + "' has no default value");
                }
            }
        }

        return ScreenConfigurationPreview.builder()
                .screenId(screenId)
                .screenName(screen.getName())
                .screenType(screen.getScreenType().name())
                .fieldCount(fields.size())
                .fields(fieldPreviews)
                .warnings(warnings)
                .build();
    }

    @Transactional(readOnly = true)
    public ScreenValidationResult validateScreen(UUID screenId) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        List<ScreenField> fields = screenFieldRepository.findByScreenIdOrderByPositionAsc(screenId);

        if (fields.isEmpty()) {
            warnings.add("Screen has no fields configured");
        }

        Set<UUID> seenFields = new HashSet<>();
        for (ScreenField field : fields) {
            if (seenFields.contains(field.getFieldId())) {
                errors.add("Duplicate field detected: " + field.getFieldId());
            }
            seenFields.add(field.getFieldId());

            if (field.getPosition() < 0) {
                errors.add("Field has invalid position: " + field.getPosition());
            }

            if (!field.getIsVisible() && field.getIsRequired()) {
                warnings.add("Required field is not visible");
            }
        }

        if (fields.size() > MAX_FIELDS_PER_SCREEN) {
            warnings.add("Screen has more than " + MAX_FIELDS_PER_SCREEN + " fields - consider splitting");
        }

        return ScreenValidationResult.builder()
                .screenId(screenId)
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensForField(UUID fieldId) {
        List<ScreenField> screenFields = screenFieldRepository.findByScreenIdOrderByPositionAsc(fieldId);

        return screenFields.stream()
                .map(sf -> screenRepository.findById(sf.getScreenId()).orElse(null))
                .filter(Objects::nonNull)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScreenFieldResponse updateFieldPosition(UUID screenId, UUID fieldId, Integer newPosition) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        ScreenField field = screenFieldRepository.findByScreenIdAndFieldId(screenId, fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Field not found on screen", "screenId", screenId));

        if (newPosition < 0 || newPosition > MAX_POSITION_VALUE) {
            throw new ValidationException("Position must be between 0 and " + MAX_POSITION_VALUE);
        }

        int oldPosition = field.getPosition();

        if (newPosition > oldPosition) {
            screenFieldRepository.decrementPositionsBetween(screenId, oldPosition, newPosition);
        } else if (newPosition < oldPosition) {
            screenFieldRepository.incrementPositionsBetween(screenId, newPosition, oldPosition);
        }

        field.setPosition(newPosition);
        field = screenFieldRepository.save(field);

        log.info("Field {} position updated from {} to {} on screen {}", fieldId, oldPosition, newPosition, screenId);
        return mapToFieldResponse(field);
    }

    @Transactional
    public List<ScreenFieldResponse> bulkAddFields(UUID screenId, List<UUID> fieldIds) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        List<ScreenFieldResponse> responses = new ArrayList<>();

        for (UUID fieldId : fieldIds) {
            if (!screenFieldRepository.existsByScreenIdAndFieldId(screenId, fieldId)) {
                responses.add(addField(screenId, fieldId, null, false));
            }
        }

        log.info("Bulk added {} fields to screen {}", responses.size(), screenId);
        return responses;
    }

    @Transactional
    public int bulkRemoveFields(UUID screenId, List<UUID> fieldIds) {
        int removedCount = 0;

        for (UUID fieldId : fieldIds) {
            if (screenFieldRepository.existsByScreenIdAndFieldId(screenId, fieldId)) {
                removeField(screenId, fieldId);
                removedCount++;
            }
        }

        log.info("Bulk removed {} fields from screen {}", removedCount, screenId);
        return removedCount;
    }

    @Transactional
    public ScreenResponse updateScreen(UUID screenId, String name, Integer position) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        if (name != null) screen.setName(name);
        if (position != null) screen.setPosition(position);

        screen = screenRepository.save(screen);
        log.info("Screen updated: {}", screenId);

        return mapToResponse(screen);
    }

    @Transactional
    public void deleteScreen(UUID screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));

        screenFieldRepository.deleteAll(screenFieldRepository.findByScreenIdOrderByPositionAsc(screenId));
        screenRepository.delete(screen);
        log.info("Screen deleted: {}", screenId);
    }

    private ScreenResponse mapToResponse(Screen screen) {
        return ScreenResponse.builder()
                .id(screen.getId())
                .name(screen.getName())
                .screenType(screen.getScreenType().name())
                .position(screen.getPosition())
                .createdAt(null)
                .updatedAt(null)
                .build();
    }

    private ScreenFieldResponse mapToFieldResponse(ScreenField screenField) {
        return ScreenFieldResponse.builder()
                .id(screenField.getId())
                .screenId(screenField.getScreenId())
                .fieldId(screenField.getFieldId())
                .position(screenField.getPosition())
                .isRequired(screenField.getIsRequired())
                .isEditable(screenField.getIsEditable())
                .isVisible(screenField.getIsVisible())
                .build();
    }
}