package com.jira.project.service;

import com.jira.project.dto.FieldConfigurationRuleResponse;
import com.jira.project.dto.ValidateCreateIssueFieldsRequest;
import com.jira.project.entity.FieldConfigurationEntry;
import com.jira.project.entity.ProjectScheme;
import com.jira.project.repository.FieldConfigurationEntryRepository;
import com.jira.project.repository.FieldConfigurationSchemeRepository;
import com.jira.project.repository.ProjectSchemeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FieldConfigurationService {

    private final ProjectSchemeRepository projectSchemeRepository;
    private final FieldConfigurationSchemeRepository fieldConfigurationSchemeRepository;
    private final FieldConfigurationEntryRepository fieldConfigurationEntryRepository;

    @Value("${app.defaults.field-aliases:summary=title|summary,issuetype=issueTypeId|issuetype,priority=priorityId|priority,assignee=assigneeId|assignee,duedate=dueDate|duedate,components=componentIds|components,labels=labels,fixversions=fixVersions,affectsversions=affectsVersions,description=description}")
    private String fieldAliasesStr;

    private Map<String, List<String>> fieldAliasMap;

    @PostConstruct
    void initFieldAliases() {
        fieldAliasMap = new LinkedHashMap<>();
        for (String entry : fieldAliasesStr.split(",(?=[a-z])")) {
            String[] kv = entry.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                List<String> aliases = Arrays.asList(kv[1].trim().split("\\|"));
                fieldAliasMap.put(key, aliases);
            }
        }
    }

    public List<FieldConfigurationRuleResponse> resolveForProject(UUID projectId, UUID issueTypeId) {
        UUID schemeId = projectSchemeRepository.findByProjectId(projectId)
                .map(ProjectScheme::getFieldConfigurationScheme)
                .filter(Objects::nonNull)
                .map(s -> s.getId())
                .orElseGet(() -> fieldConfigurationSchemeRepository.findByIsDefaultTrue()
                        .map(s -> s.getId())
                        .orElse(null));

        if (schemeId == null) {
            return List.of();
        }

        List<FieldConfigurationEntry> global =
                fieldConfigurationEntryRepository.findBySchemeIdAndIssueTypeIdIsNull(schemeId);
        List<FieldConfigurationEntry> specific = issueTypeId != null
                ? fieldConfigurationEntryRepository.findBySchemeIdAndIssueTypeId(schemeId, issueTypeId)
                : List.of();

        Map<String, FieldConfigurationEntry> merged = new LinkedHashMap<>();
        for (FieldConfigurationEntry entry : global) {
            merged.put(normalizeKey(entry.getFieldKey()), entry);
        }
        for (FieldConfigurationEntry entry : specific) {
            merged.put(normalizeKey(entry.getFieldKey()), entry);
        }

        return merged.values().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getHidden()))
                .map(this::toRule)
                .collect(Collectors.toList());
    }

    public List<String> validateCreateFields(UUID projectId, ValidateCreateIssueFieldsRequest request) {
        UUID issueTypeId = request.getIssueTypeId();
        Map<String, Object> fields = request.getFields() != null ? request.getFields() : Map.of();
        List<String> errors = new ArrayList<>();

        for (FieldConfigurationRuleResponse rule : resolveForProject(projectId, issueTypeId)) {
            if (!rule.isRequired()) {
                continue;
            }
            if (!isPresent(rule.getFieldKey(), fields)) {
                errors.add("Field '" + rule.getFieldKey() + "' is required");
            }
        }
        return errors;
    }

    private boolean isPresent(String fieldKey, Map<String, Object> fields) {
        String key = normalizeKey(fieldKey);
        if (fields.containsKey(key) && hasValue(fields.get(key))) {
            return true;
        }
        List<String> aliases = fieldAliasMap.get(key);
        if (aliases != null) {
            return aliases.stream().anyMatch(alias -> hasValue(fields.get(alias)));
        }
        return false;
    }

    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return !s.isBlank();
        }
        if (value instanceof Object[] arr) {
            return arr.length > 0;
        }
        if (value instanceof Collection<?> c) {
            return !c.isEmpty();
        }
        return true;
    }

    private FieldConfigurationRuleResponse toRule(FieldConfigurationEntry entry) {
        return FieldConfigurationRuleResponse.builder()
                .fieldKey(normalizeKey(entry.getFieldKey()))
                .issueTypeId(entry.getIssueTypeId())
                .required(Boolean.TRUE.equals(entry.getRequired()))
                .visible(!Boolean.TRUE.equals(entry.getHidden()))
                .hidden(Boolean.TRUE.equals(entry.getHidden()))
                .build();
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}
