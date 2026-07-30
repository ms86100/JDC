package com.avionics_systems.migration.service.field;

import com.avionics_systems.migration.dto.IssueVisibleFieldsResponse;
import com.avionics_systems.migration.dto.VisibleFieldResponse;
import com.avionics_systems.migration.entity.field.*;
import com.avionics_systems.migration.entity.field.FieldScreenMapping.FieldScreenType;
import com.avionics_systems.migration.repository.field.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central Legacy DC-style visibility resolver: context → screen mapping → field configuration → values.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldVisibilityEngine {

    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CustomFieldContextRepository customFieldContextRepository;
    private final FieldScreenMappingRepository fieldScreenMappingRepository;
    private final FieldConfigurationOverrideRepository fieldConfigurationOverrideRepository;
    private final IssueFieldValueRepository issueFieldValueRepository;
    private final FieldValueService fieldValueService;

    @Transactional(readOnly = true)
    public IssueVisibleFieldsResponse resolveVisibleFieldsForIssue(
            UUID issueId,
            String issueKey,
            UUID projectId,
            UUID issueTypeId,
            FieldScreenType screenType) {

        FieldValueService.FieldValueResult stored = fieldValueService.getAllFieldValues(issueId);
        Set<String> keysWithValues = stored.values().keySet();

        boolean strictScreenMappings = hasScreenMappings(projectId, screenType);

        List<VisibleFieldResponse> visible = new ArrayList<>();

        for (CustomFieldDefinition cf : customFieldDefinitionRepository.findAllEnabled()) {
            Optional<FieldDefinition> defOpt = fieldDefinitionRepository.findByFieldKey(cf.getFieldKey());
            if (defOpt.isEmpty()) {
                continue;
            }
            FieldDefinition def = defOpt.get();
            if (Boolean.TRUE.equals(def.getDeprecated())) {
                continue;
            }

            if (!contextApplies(cf.getId(), projectId, issueTypeId)) {
                continue;
            }

            FieldConfigurationOverride override = resolveConfigurationOverride(projectId, issueTypeId, def.getFieldKey());
            if (isHidden(def, override)) {
                if (!keysWithValues.contains(def.getFieldKey())) {
                    continue;
                }
            }

            if (!isOnScreen(def.getFieldKey(), projectId, screenType)) {
                if (strictScreenMappings && !keysWithValues.contains(def.getFieldKey())) {
                    continue;
                }
                if (!strictScreenMappings && !keysWithValues.contains(def.getFieldKey())) {
                    continue;
                }
            }

            Object value = stored.values().get(def.getFieldKey());
            int order = screenDisplayOrder(def.getFieldKey(), projectId, screenType);

            visible.add(VisibleFieldResponse.builder()
                    .fieldKey(def.getFieldKey())
                    .displayName(def.getDisplayName())
                    .fieldType(def.getFieldType() != null ? def.getFieldType().name() : cf.getType())
                    .renderer(def.getRenderer() != null ? def.getRenderer().name() : cf.getRendererKey())
                    .value(value)
                    .required(override != null && Boolean.TRUE.equals(override.getRequired()) || Boolean.TRUE.equals(def.getRequired()))
                    .readOnly(override != null && Boolean.TRUE.equals(override.getReadOnly()) || Boolean.TRUE.equals(def.getReadOnly()))
                    .custom(true)
                    .displayOrder(order)
                    .build());
        }

        visible.sort(Comparator.comparingInt(VisibleFieldResponse::getDisplayOrder)
                .thenComparing(VisibleFieldResponse::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        return IssueVisibleFieldsResponse.builder()
                .issueId(issueId)
                .issueKey(issueKey)
                .projectId(projectId)
                .issueTypeId(issueTypeId)
                .screenType(screenType.name())
                .fields(visible)
                .totalCount(visible.size())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isFieldVisible(
            String fieldKey,
            UUID projectId,
            UUID issueTypeId,
            FieldScreenType screenType,
            UUID issueId) {

        Optional<FieldDefinition> defOpt = fieldDefinitionRepository.findByFieldKey(fieldKey);
        if (defOpt.isEmpty() || Boolean.TRUE.equals(defOpt.get().getDeprecated())) {
            return false;
        }
        FieldDefinition def = defOpt.get();

        Optional<CustomFieldDefinition> cfOpt = customFieldDefinitionRepository.findByFieldKey(fieldKey);
        if (cfOpt.isEmpty() || !Boolean.TRUE.equals(cfOpt.get().getEnabled())) {
            return !Boolean.TRUE.equals(def.getCustom());
        }

        if (!contextApplies(cfOpt.get().getId(), projectId, issueTypeId)) {
            return false;
        }

        FieldConfigurationOverride override = resolveConfigurationOverride(projectId, issueTypeId, fieldKey);
        boolean hasValue = issueId != null && issueFieldValueRepository
                .findByIssueIdWithFieldDefinition(issueId).stream()
                .anyMatch(fv -> fv.getFieldDefinition() != null
                        && fieldKey.equals(fv.getFieldDefinition().getFieldKey()));

        if (isHidden(def, override) && !hasValue) {
            return false;
        }

        if (hasScreenMappings(projectId, screenType)) {
            return isOnScreen(fieldKey, projectId, screenType) || hasValue;
        }
        return hasValue || !Boolean.TRUE.equals(def.getHidden());
    }

    @Transactional
    public void addFieldToScreen(UUID projectId, String fieldKey, FieldScreenType screenType, int displayOrder) {
        Optional<FieldScreenMapping> existing = projectId != null
                ? fieldScreenMappingRepository.findByProjectIdAndScreenTypeAndFieldKey(projectId, screenType, fieldKey)
                : fieldScreenMappingRepository.findByProjectIdIsNullAndScreenTypeAndFieldKey(screenType, fieldKey);

        if (existing.isPresent()) {
            return;
        }

        fieldScreenMappingRepository.save(FieldScreenMapping.builder()
                .projectId(projectId)
                .screenType(screenType)
                .fieldKey(fieldKey)
                .tabName("custom_fields")
                .displayOrder(displayOrder)
                .build());
    }

    @Transactional
    public int addFieldToDefaultScreens(UUID projectId, String fieldKey) {
        int order = fieldScreenMappingRepository.findByProjectIdIsNullAndScreenType(FieldScreenType.VIEW).size();
        addFieldToScreen(projectId, fieldKey, FieldScreenType.VIEW, order);
        addFieldToScreen(projectId, fieldKey, FieldScreenType.EDIT, order);
        addFieldToScreen(null, fieldKey, FieldScreenType.VIEW, order);
        addFieldToScreen(null, fieldKey, FieldScreenType.EDIT, order);
        return 4;
    }

    private boolean contextApplies(UUID customFieldId, UUID projectId, UUID issueTypeId) {
        List<CustomFieldContext> contexts = customFieldContextRepository.findEnabledByCustomFieldId(customFieldId);
        if (contexts.isEmpty()) {
            return true;
        }
        for (CustomFieldContext ctx : contexts) {
            if (!Boolean.TRUE.equals(ctx.getEnabled())) {
                continue;
            }
            if (!projectMatches(ctx, projectId)) {
                continue;
            }
            if (!issueTypeMatches(ctx, issueTypeId)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean projectMatches(CustomFieldContext ctx, UUID projectId) {
        if (Boolean.TRUE.equals(ctx.getAllProjects())) {
            return true;
        }
        if (projectId == null) {
            return false;
        }
        UUID[] ids = ctx.getProjectIds();
        if (ids == null || ids.length == 0) {
            return false;
        }
        return Arrays.stream(ids).anyMatch(projectId::equals);
    }

    private boolean issueTypeMatches(CustomFieldContext ctx, UUID issueTypeId) {
        UUID[] types = ctx.getIssueTypeIds();
        if (types == null || types.length == 0) {
            return true;
        }
        if (issueTypeId == null) {
            return true;
        }
        return Arrays.stream(types).anyMatch(issueTypeId::equals);
    }

    private FieldConfigurationOverride resolveConfigurationOverride(
            UUID projectId, UUID issueTypeId, String fieldKey) {
        if (projectId == null) {
            return null;
        }
        if (issueTypeId != null) {
            Optional<FieldConfigurationOverride> specific =
                    fieldConfigurationOverrideRepository.findByProjectIdAndIssueTypeIdAndFieldKey(
                            projectId, issueTypeId, fieldKey);
            if (specific.isPresent()) {
                return specific.get();
            }
        }
        return fieldConfigurationOverrideRepository
                .findByProjectIdAndIssueTypeIdIsNullAndFieldKey(projectId, fieldKey)
                .orElse(null);
    }

    private boolean isHidden(FieldDefinition def, FieldConfigurationOverride override) {
        if (override != null && Boolean.TRUE.equals(override.getHidden())) {
            return true;
        }
        return Boolean.TRUE.equals(def.getHidden());
    }

    private boolean hasScreenMappings(UUID projectId, FieldScreenType screenType) {
        if (projectId != null && fieldScreenMappingRepository.existsByProjectIdAndScreenType(projectId, screenType)) {
            return true;
        }
        return fieldScreenMappingRepository.existsByProjectIdIsNullAndScreenType(screenType);
    }

    private boolean isOnScreen(String fieldKey, UUID projectId, FieldScreenType screenType) {
        if (projectId != null) {
            if (fieldScreenMappingRepository.findByProjectIdAndScreenTypeAndFieldKey(projectId, screenType, fieldKey).isPresent()) {
                return true;
            }
        }
        return fieldScreenMappingRepository.findByProjectIdIsNullAndScreenTypeAndFieldKey(screenType, fieldKey).isPresent();
    }

    private int screenDisplayOrder(String fieldKey, UUID projectId, FieldScreenType screenType) {
        if (projectId != null) {
            Optional<FieldScreenMapping> m = fieldScreenMappingRepository
                    .findByProjectIdAndScreenTypeAndFieldKey(projectId, screenType, fieldKey);
            if (m.isPresent()) {
                return m.get().getDisplayOrder() != null ? m.get().getDisplayOrder() : 0;
            }
        }
        return fieldScreenMappingRepository.findByProjectIdIsNullAndScreenTypeAndFieldKey(screenType, fieldKey)
                .map(m -> m.getDisplayOrder() != null ? m.getDisplayOrder() : 0)
                .orElse(999);
    }

    @Transactional(readOnly = true)
    public List<String> searchableFieldKeys(UUID projectId) {
        return customFieldDefinitionRepository.findAllEnabled().stream()
                .filter(cf -> Boolean.TRUE.equals(cf.getSearchable()))
                .map(CustomFieldDefinition::getFieldKey)
                .filter(key -> isFieldVisible(key, projectId, null, FieldScreenType.VIEW, null))
                .collect(Collectors.toList());
    }
}
