package com.avionics_systems.workflow.engine;

import com.avionics_systems.workflow.dto.AvailableTransitionResponse;
import com.avionics_systems.workflow.dto.TransitionScreenFieldDto;
import com.avionics_systems.workflow.entity.WorkflowScreen;
import com.avionics_systems.workflow.entity.WorkflowScreenField;
import com.avionics_systems.workflow.entity.WorkflowTransition;
import com.avionics_systems.workflow.repository.WorkflowScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransitionScreenService {

    private final WorkflowScreenRepository workflowScreenRepository;

    public List<TransitionScreenFieldDto> getScreenFields(UUID screenId) {
        if (screenId == null) {
            return List.of();
        }
        return workflowScreenRepository.findById(screenId)
                .map(this::mapFields)
                .orElse(List.of());
    }

    public List<String> validateScreenInput(WorkflowTransition transition, Map<String, Object> screenInput, Map<String, Object> issueData) {
        return new ArrayList<>(validateScreenInputFields(transition, screenInput, issueData).values());
    }

    public Map<String, String> validateScreenInputFields(
            WorkflowTransition transition,
            Map<String, Object> screenInput,
            Map<String, Object> issueData) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (transition == null || transition.getScreenId() == null) {
            return fieldErrors;
        }
        Map<String, Object> input = screenInput != null ? screenInput : Map.of();
        for (TransitionScreenFieldDto field : getScreenFields(transition.getScreenId())) {
            if (field.isRequired()) {
                Object val = input.get(field.getFieldId());
                if (val == null) {
                    val = input.get(field.getFieldName());
                }
                if (val == null || val.toString().isBlank()) {
                    String key = field.getFieldName() != null ? field.getFieldName() : field.getFieldId();
                    fieldErrors.put(key, key + " is required");
                }
            }
        }
        return fieldErrors;
    }

    public AvailableTransitionResponse.AvailableTransitionItem enrichTransitionItem(
            WorkflowTransition t,
            String requiredPermission) {
        List<TransitionScreenFieldDto> fields = getScreenFields(t.getScreenId());
        return AvailableTransitionResponse.AvailableTransitionItem.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .toStatusId(t.getToStatusId())
                .screenId(t.getScreenId())
                .hasScreen(t.getScreenId() != null)
                .screenFields(fields)
                .requiredPermission(requiredPermission)
                .build();
    }

    public AvailableTransitionResponse.AvailableTransitionItem enrichTransitionItem(WorkflowTransition t) {
        return enrichTransitionItem(t, t.getPermissionCheck());
    }

    private List<TransitionScreenFieldDto> mapFields(WorkflowScreen screen) {
        List<TransitionScreenFieldDto> fields = new ArrayList<>();
        if (screen.getTabs() == null) {
            return fields;
        }
        screen.getTabs().forEach(tab -> {
            if (tab.getFields() == null) {
                return;
            }
            for (WorkflowScreenField f : tab.getFields()) {
                fields.add(TransitionScreenFieldDto.builder()
                        .fieldId(f.getId() != null ? f.getId().toString() : f.getFieldId())
                        .fieldName(f.getFieldId())
                        .fieldType(f.getFieldType())
                        .required(Boolean.TRUE.equals(f.getRequired()))
                        .defaultValue(null)
                        .build());
            }
        });
        return fields;
    }
}
