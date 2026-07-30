package com.avionics_systems.migration.service.field;

import com.avionics_systems.migration.entity.field.CustomFieldContext;
import com.avionics_systems.migration.entity.field.CustomFieldDefinition;
import com.avionics_systems.migration.entity.field.FieldDefinition;
import com.avionics_systems.migration.repository.field.CustomFieldContextRepository;
import com.avionics_systems.migration.repository.field.CustomFieldDefinitionRepository;
import com.avionics_systems.migration.repository.field.FieldDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Aligns provisioned custom fields with issue screen layout (Legacy DC field configuration scheme behavior).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldScreenConfigurationService {

    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CustomFieldContextRepository customFieldContextRepository;
    private final FieldVisibilityEngine fieldVisibilityEngine;

    /**
     * Registers field on VIEW/EDIT screens (Legacy DC screen scheme) without forcing global visibility on all surfaces.
     */
    @Transactional
    public int ensureFieldVisibleOnScreen(String fieldKey, UUID projectId) {
        Optional<FieldDefinition> opt = fieldDefinitionRepository.findByFieldKey(fieldKey);
        if (opt.isEmpty()) {
            return 0;
        }
        FieldDefinition def = opt.get();
        boolean changed = false;
        if (def.getScreenRegion() == null) {
            def.setScreenRegion(FieldDefinition.ScreenRegion.SIDEBAR_DETAILS);
            changed = true;
        }
        if (Boolean.TRUE.equals(def.getDeprecated())) {
            def.setDeprecated(false);
            changed = true;
        }
        if (changed) {
            fieldDefinitionRepository.save(def);
        }
        ensureCustomFieldContext(def.getFieldKey(), projectId);
        fieldVisibilityEngine.addFieldToDefaultScreens(projectId, fieldKey);
        return 1;
    }

    @Transactional
    public int ensureProjectFieldsVisible(UUID projectId, Collection<String> fieldKeys) {
        if (fieldKeys == null || fieldKeys.isEmpty()) {
            return ensureAllCustomFieldsOnScreen(projectId);
        }
        int count = 0;
        for (String key : fieldKeys) {
            count += ensureFieldVisibleOnScreen(key, projectId);
        }
        return count;
    }

    @Transactional
    public int ensureAllCustomFieldsOnScreen(UUID projectId) {
        List<CustomFieldDefinition> customFields = customFieldDefinitionRepository.findAllEnabled();
        int count = 0;
        for (CustomFieldDefinition cf : customFields) {
            count += ensureFieldVisibleOnScreen(cf.getFieldKey(), projectId);
        }
        return count;
    }

    private void ensureCustomFieldContext(String fieldKey, UUID projectId) {
        customFieldDefinitionRepository.findByFieldKey(fieldKey).ifPresent(cf -> {
            List<CustomFieldContext> contexts = customFieldContextRepository.findEnabledByCustomFieldId(cf.getId());
            if (contexts.isEmpty()) {
                CustomFieldContext ctx = CustomFieldContext.builder()
                        .customFieldId(cf.getId())
                        .name(projectId != null ? "Project " + projectId : "Default Context")
                        .allProjects(projectId == null)
                        .projectIds(projectId != null ? new UUID[]{projectId} : null)
                        .enabled(true)
                        .displayOrder(0)
                        .build();
                customFieldContextRepository.save(ctx);
                return;
            }
            if (projectId != null) {
                for (CustomFieldContext ctx : contexts) {
                    if (Boolean.TRUE.equals(ctx.getAllProjects())) {
                        continue;
                    }
                    UUID[] ids = ctx.getProjectIds();
                    if (ids == null) {
                        ctx.setProjectIds(new UUID[]{projectId});
                        customFieldContextRepository.save(ctx);
                    } else if (Arrays.stream(ids).noneMatch(projectId::equals)) {
                        UUID[] merged = Arrays.copyOf(ids, ids.length + 1);
                        merged[ids.length] = projectId;
                        ctx.setProjectIds(merged);
                        customFieldContextRepository.save(ctx);
                    }
                }
            }
        });
    }
}
