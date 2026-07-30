package com.avionics_systems.issue.customfield;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for custom field type handlers.
 * F7-US001: Custom Field Types
 *
 * Provides centralized access to all registered custom field type handlers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomFieldTypeRegistry {

    private final List<CustomFieldTypeHandler> handlers;
    private final Map<String, CustomFieldTypeHandler> handlerMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing CustomFieldTypeRegistry with {} handlers", handlers.size());

        for (CustomFieldTypeHandler handler : handlers) {
            registerHandler(handler);
        }

        log.info("CustomFieldTypeRegistry initialized with types: {}", getRegisteredTypes());
    }

    /**
     * Register a custom field type handler
     */
    public void registerHandler(CustomFieldTypeHandler handler) {
        if (handler == null || handler.getType() == null) {
            log.warn("Attempted to register invalid handler");
            return;
        }

        String type = handler.getType().toLowerCase();
        handlerMap.put(type, handler);
        log.debug("Registered custom field type handler: {} -> {}", type, handler.getDisplayName());
    }

    /**
     * Get handler for a specific field type
     */
    public Optional<CustomFieldTypeHandler> getHandler(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlerMap.get(type.toLowerCase()));
    }

    /**
     * Get handler for a specific field type, throwing if not found
     */
    public CustomFieldTypeHandler getHandlerOrThrow(String type) {
        return getHandler(type)
                .orElseThrow(() -> new IllegalArgumentException("No handler registered for field type: " + type));
    }

    /**
     * Check if a field type is supported
     */
    public boolean isSupported(String type) {
        return type != null && handlerMap.containsKey(type.toLowerCase());
    }

    /**
     * Get all registered field types
     */
    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }

    /**
     * Get all registered handlers
     */
    public Collection<CustomFieldTypeHandler> getAllHandlers() {
        return Collections.unmodifiableCollection(handlerMap.values());
    }

    /**
     * Get handler metadata for all registered types
     */
    public List<Map<String, Object>> getHandlerMetadata() {
        List<Map<String, Object>> metadata = new ArrayList<>();
        for (CustomFieldTypeHandler handler : handlerMap.values()) {
            metadata.add(Map.of(
                    "type", handler.getType(),
                    "displayName", handler.getDisplayName()
            ));
        }
        return metadata;
    }

    /**
     * Process a custom field value through its handler
     */
    public Object processFieldValue(String fieldType, Object value, Map<String, Object> config, ProcessingMode mode) {
        CustomFieldTypeHandler handler = getHandlerOrThrow(fieldType);

        return switch (mode) {
            case VALIDATE -> {
                CustomFieldTypeHandler.ValidationResult result = handler.validate(value, config);
                if (!result.valid()) {
                    throw new IllegalArgumentException(result.message());
                }
                yield result.sanitizedValue();
            }
            case RENDER_DISPLAY -> handler.renderForDisplay(value, config);
            case RENDER_EDIT -> handler.renderForEdit(value, config);
            case PARSE_INPUT -> handler.parseInput(value, config);
            case TO_JSON -> handler.toJsonValue(value, config);
            case FROM_JSON -> handler.fromJsonValue(value, config);
            case TO_SEARCHABLE -> handler.toSearchableText(value, config);
        };
    }

    /**
     * Processing modes for custom field values
     */
    public enum ProcessingMode {
        VALIDATE,
        RENDER_DISPLAY,
        RENDER_EDIT,
        PARSE_INPUT,
        TO_JSON,
        FROM_JSON,
        TO_SEARCHABLE
    }
}