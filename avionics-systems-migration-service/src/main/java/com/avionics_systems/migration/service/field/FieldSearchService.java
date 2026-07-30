package com.avionics_systems.migration.service.field;

import com.avionics_systems.migration.entity.field.FieldDefinition;
import com.avionics_systems.migration.entity.field.FieldScreenMapping;
import com.avionics_systems.migration.entity.field.IssueFieldValue;
import com.avionics_systems.migration.repository.field.FieldDefinitionRepository;
import com.avionics_systems.migration.repository.field.IssueFieldValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom field search / JQL helper (Phase 6).
 */
@Service
@RequiredArgsConstructor
public class FieldSearchService {

    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final IssueFieldValueRepository issueFieldValueRepository;
    private final FieldVisibilityEngine fieldVisibilityEngine;

    @Transactional(readOnly = true)
    public List<UUID> searchIssuesByCustomField(UUID projectId, String fieldKey, String query) {
        FieldDefinition def = fieldDefinitionRepository.findByFieldKey(fieldKey).orElse(null);
        if (def == null || !Boolean.TRUE.equals(def.getSearchable())) {
            return List.of();
        }
        if (!fieldVisibilityEngine.isFieldVisible(fieldKey, projectId, null,
                FieldScreenMapping.FieldScreenType.VIEW, null)) {
            return List.of();
        }

        String needle = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        if (needle.isEmpty()) {
            return List.of();
        }

        List<IssueFieldValue> values = issueFieldValueRepository.findByFieldDefinitionId(def.getId());
        return values.stream()
                .filter(fv -> matchesSearch(fv, needle))
                .map(IssueFieldValue::getIssueId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> autocompleteFieldKeys(UUID projectId, String prefix) {
        String p = prefix != null ? prefix.toLowerCase(Locale.ROOT) : "";
        return fieldVisibilityEngine.searchableFieldKeys(projectId).stream()
                .filter(key -> p.isEmpty() || key.toLowerCase(Locale.ROOT).contains(p))
                .map(key -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("fieldKey", key);
                    fieldDefinitionRepository.findByFieldKey(key).ifPresent(def ->
                            row.put("displayName", def.getDisplayName()));
                    return row;
                })
                .limit(50)
                .toList();
    }

    private boolean matchesSearch(IssueFieldValue fv, String needle) {
        if (fv.getSearchableText() != null && fv.getSearchableText().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (fv.getStringValue() != null && fv.getStringValue().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        if (fv.getFormattedValue() != null && fv.getFormattedValue().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        return false;
    }
}
