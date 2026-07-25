package com.jira.admin.service;

import com.jira.admin.dto.SystemConfigRequest;
import com.jira.admin.entity.SystemConfigurationEntity;
import com.jira.admin.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Service for system_configuration CRUD operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigurationRepository configRepository;
    private final MessageSource messageSource;

    @Value("${app.defaults.config-value-type:STRING}")
    private String defaultValueType;

    @Value("${app.defaults.config-category:custom}")
    private String defaultConfigCategory;

    @Transactional(readOnly = true)
    public List<SystemConfigurationEntity> getAllConfigs() {
        return configRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SystemConfigurationEntity getByKey(String configKey) {
        return configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.config.not.found", new Object[]{configKey}, Locale.ENGLISH)));
    }

    @Transactional(readOnly = true)
    public List<SystemConfigurationEntity> getByCategory(String category) {
        return configRepository.findByCategoryOrderByConfigKeyAsc(category);
    }

    @Transactional
    public SystemConfigurationEntity updateByKey(String configKey, String newValue) {
        SystemConfigurationEntity config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.config.not.found", new Object[]{configKey}, Locale.ENGLISH)));

        if (!config.getIsEditable()) {
            throw new IllegalStateException(
                    messageSource.getMessage("error.config.not.editable", new Object[]{configKey}, Locale.ENGLISH));
        }

        config.setConfigValue(newValue);
        config = configRepository.save(config);
        log.info("System configuration updated: {} = {}", configKey, newValue);
        return config;
    }

    @Transactional
    public SystemConfigurationEntity create(SystemConfigRequest request) {
        if (configRepository.existsByConfigKey(request.getConfigKey())) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.config.key.exists", new Object[]{request.getConfigKey()}, Locale.ENGLISH));
        }

        SystemConfigurationEntity config = SystemConfigurationEntity.builder()
                .configKey(request.getConfigKey())
                .configValue(request.getConfigValue())
                .valueType(request.getValueType() != null ? request.getValueType() : defaultValueType)
                .category(request.getCategory() != null ? request.getCategory() : defaultConfigCategory)
                .description(request.getDescription())
                .isEditable(request.getIsEditable() != null ? request.getIsEditable() : true)
                .build();

        config = configRepository.save(config);
        log.info("System configuration created: {}", request.getConfigKey());
        return config;
    }

    @Transactional
    public void deleteByKey(String configKey) {
        SystemConfigurationEntity config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.config.not.found", new Object[]{configKey}, Locale.ENGLISH)));
        configRepository.delete(config);
        log.info("System configuration deleted: {}", configKey);
    }

    /**
     * Convenience helper: get a config value parsed by its declared type.
     */
    public Object getTypedValue(String configKey) {
        SystemConfigurationEntity config = getByKey(configKey);
        return parseValue(config.getConfigValue(), config.getValueType());
    }

    private Object parseValue(String value, String valueType) {
        if (value == null) return null;
        return switch (valueType != null ? valueType.toUpperCase() : "STRING") {
            case "INTEGER" -> Integer.parseInt(value);
            case "BOOLEAN" -> Boolean.parseBoolean(value);
            case "LONG" -> Long.parseLong(value);
            case "DOUBLE" -> Double.parseDouble(value);
            default -> value;
        };
    }
}
