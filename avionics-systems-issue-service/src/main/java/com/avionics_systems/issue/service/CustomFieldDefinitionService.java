package com.avionics_systems.issue.service;

import com.avionics_systems.issue.customfield.CustomFieldTypeRegistry;
import com.avionics_systems.issue.entity.CustomFieldDefinition;
import com.avionics_systems.issue.entity.CustomFieldOption;
import com.avionics_systems.issue.exception.ResourceNotFoundException;
import com.avionics_systems.issue.repository.CustomFieldDefinitionRepository;
import com.avionics_systems.issue.repository.CustomFieldOptionRepository;
import com.avionics_systems.issue.repository.CustomFieldValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomFieldDefinitionService {

    private final CustomFieldDefinitionRepository definitionRepository;
    private final CustomFieldOptionRepository optionRepository;
    private final CustomFieldValueRepository valueRepository;
    private final CustomFieldTypeRegistry typeRegistry;

    @Transactional(readOnly = true)
    public List<CustomFieldDefinition> listAll() {
        return definitionRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public CustomFieldDefinition getById(UUID id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomFieldDefinition", "id", id));
    }

    @Transactional(readOnly = true)
    public CustomFieldDefinition getByKey(String fieldKey) {
        return definitionRepository.findByFieldKey(fieldKey)
                .orElseThrow(() -> new ResourceNotFoundException("CustomFieldDefinition", "fieldKey", fieldKey));
    }

    @Transactional(readOnly = true)
    public List<CustomFieldDefinition> listByType(String fieldType) {
        return definitionRepository.findByFieldType(fieldType);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldDefinition> search(String name) {
        return definitionRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public CustomFieldDefinition create(CustomFieldDefinition definition) {
        if (definitionRepository.existsByFieldKey(definition.getFieldKey())) {
            throw new IllegalArgumentException("Custom field with key '" + definition.getFieldKey() + "' already exists");
        }
        if (!typeRegistry.isSupported(definition.getFieldType())) {
            log.warn("Field type '{}' has no registered handler — field will be stored but not validated", definition.getFieldType());
        }
        return definitionRepository.save(definition);
    }

    @Transactional
    public CustomFieldDefinition update(UUID id, CustomFieldDefinition update) {
        CustomFieldDefinition existing = getById(id);
        if (update.getName() != null) existing.setName(update.getName());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getDefaultValue() != null) existing.setDefaultValue(update.getDefaultValue());
        if (update.getIsRequired() != null) existing.setIsRequired(update.getIsRequired());
        if (update.getIsSearchable() != null) existing.setIsSearchable(update.getIsSearchable());
        if (update.getIsSortable() != null) existing.setIsSortable(update.getIsSortable());
        if (update.getScreenRegion() != null) existing.setScreenRegion(update.getScreenRegion());
        if (update.getRenderer() != null) existing.setRenderer(update.getRenderer());
        if (update.getOptions() != null) existing.setOptions(update.getOptions());
        return definitionRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!definitionRepository.existsById(id)) {
            throw new ResourceNotFoundException("CustomFieldDefinition", "id", id);
        }
        optionRepository.deleteAllByFieldId(id);
        definitionRepository.deleteById(id);
        log.info("Deleted custom field definition {}", id);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldOption> getOptions(UUID fieldId) {
        getById(fieldId);
        return optionRepository.findByFieldIdOrderByPositionAsc(fieldId);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldOption> getEnabledOptions(UUID fieldId) {
        return optionRepository.findByFieldIdAndDisabledFalseOrderByPositionAsc(fieldId);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldOption> getParentOptions(UUID fieldId) {
        return optionRepository.findByFieldIdAndParentOptionIdIsNullOrderByPositionAsc(fieldId);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldOption> getChildOptions(UUID parentOptionId) {
        return optionRepository.findByParentOptionIdOrderByPositionAsc(parentOptionId);
    }

    @Transactional
    public CustomFieldOption addOption(UUID fieldId, CustomFieldOption option) {
        getById(fieldId);
        option.setFieldId(fieldId);
        if (option.getPosition() == null || option.getPosition() == 0) {
            option.setPosition(optionRepository.findMaxPositionByFieldId(fieldId) + 1);
        }
        return optionRepository.save(option);
    }

    @Transactional
    public CustomFieldOption updateOption(UUID optionId, CustomFieldOption update) {
        CustomFieldOption existing = optionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomFieldOption", "id", optionId));
        if (update.getValue() != null) existing.setValue(update.getValue());
        if (update.getLabel() != null) existing.setLabel(update.getLabel());
        if (update.getPosition() != null) existing.setPosition(update.getPosition());
        if (update.getDisabled() != null) existing.setDisabled(update.getDisabled());
        return optionRepository.save(existing);
    }

    @Transactional
    public void deleteOption(UUID optionId) {
        if (!optionRepository.existsById(optionId)) {
            throw new ResourceNotFoundException("CustomFieldOption", "id", optionId);
        }
        optionRepository.deleteById(optionId);
    }

    @Transactional
    public void reorderOptions(UUID fieldId, List<UUID> orderedOptionIds) {
        for (int i = 0; i < orderedOptionIds.size(); i++) {
            optionRepository.findById(orderedOptionIds.get(i)).ifPresent(opt -> {
                opt.setPosition(orderedOptionIds.indexOf(opt.getId()));
                optionRepository.save(opt);
            });
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRegisteredFieldTypes() {
        return typeRegistry.getHandlerMetadata();
    }
}
