package com.jira.workflow.engine;

import com.jira.workflow.dto.AvailableTransitionResponse;
import com.jira.workflow.dto.TransitionScreenFieldDto;
import com.jira.workflow.entity.WorkflowScreen;
import com.jira.workflow.entity.WorkflowScreenField;
import com.jira.workflow.entity.WorkflowTransition;
import com.jira.workflow.repository.WorkflowScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        List<String> errors = new ArrayList<>();
        if (transition == null || transition.getScreenId() == null) {
            return errors;
        }
        Map<String, Object> input = screenInput != null ? screenInput : Map.of();
        for (TransitionScreenFieldDto field : getScreenFields(transition.getScreenId())) {
            if (field.isRequired()) {
                Object val = input.get(field.getFieldId());
                if (val == null) {
                    val = input.get(field.getFieldName());
                }
                if (val == null || val.toString().isBlank()) {
                    errors.add("Screen field required: " + field.getFieldName());
                }
            }
        }
        return errors;
    }

    public AvailableTransitionResponse.AvailableTransitionItem enrichTransitionItem(WorkflowTransition t) {
        List<TransitionScreenFieldDto> fields = getScreenFields(t.getScreenId());
        return AvailableTransitionResponse.AvailableTransitionItem.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .toStatusId(t.getToStatusId())
                .screenId(t.getScreenId())
                .hasScreen(t.getScreenId() != null)
                .screenFields(fields)
                .build();
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
