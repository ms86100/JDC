package com.jira.migration.service.field;

import com.jira.migration.dto.*;
import com.jira.migration.entity.field.*;
import com.jira.migration.entity.field.FieldScreenMapping.FieldScreenType;
import com.jira.migration.repository.field.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardCardLayoutService {

    private final BoardCardLayoutFieldRepository boardCardLayoutFieldRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final FieldVisibilityEngine fieldVisibilityEngine;
    @Transactional(readOnly = true)
    public BoardCardLayoutResponse getCardLayout(UUID boardId, UUID projectId) {
        List<BoardCardLayoutFieldEntity> selected = boardCardLayoutFieldRepository
                .findByBoardIdAndVisibleTrueOrderByDisplayOrderAsc(boardId);

        List<BoardCardLayoutResponse.EligibleFieldDto> eligible = listEligibleFields(projectId);

        List<BoardCardLayoutResponse.BoardCardFieldDto> selectedDtos = selected.stream()
                .map(s -> {
                    String displayName = fieldDefinitionRepository.findByFieldKey(s.getFieldKey())
                            .map(FieldDefinition::getDisplayName)
                            .orElse(s.getFieldKey());
                    return BoardCardLayoutResponse.BoardCardFieldDto.builder()
                            .fieldKey(s.getFieldKey())
                            .displayName(displayName)
                            .displayOrder(s.getDisplayOrder() != null ? s.getDisplayOrder() : 0)
                            .position(s.getPosition())
                            .visible(Boolean.TRUE.equals(s.getVisible()))
                            .build();
                })
                .toList();

        return BoardCardLayoutResponse.builder()
                .boardId(boardId)
                .projectId(projectId)
                .eligibleFields(eligible)
                .selectedFields(selectedDtos)
                .build();
    }

    @Transactional
    public BoardCardLayoutResponse saveCardLayout(
            UUID boardId, SaveBoardCardLayoutRequest request) {
        UUID projectId = request.getProjectId();
        boardCardLayoutFieldRepository.deleteByBoardId(boardId);

        if (request.getFields() != null) {
            int order = 0;
            for (SaveBoardCardLayoutRequest.CardFieldSelection sel : request.getFields()) {
                if (sel.getFieldKey() == null || sel.getFieldKey().isBlank()) {
                    continue;
                }
                if (!isEligible(sel.getFieldKey(), projectId)) {
                    continue;
                }
                boardCardLayoutFieldRepository.save(BoardCardLayoutFieldEntity.builder()
                        .boardId(boardId)
                        .projectId(projectId)
                        .fieldKey(sel.getFieldKey())
                        .displayOrder(sel.getDisplayOrder() != null ? sel.getDisplayOrder() : order++)
                        .position(sel.getPosition() != null ? sel.getPosition() : "BOTTOM")
                        .visible(sel.getVisible() == null || sel.getVisible())
                        .build());
            }
        }
        return getCardLayout(boardId, projectId);
    }

    @Transactional(readOnly = true)
    public IssueFieldValuesBatchResponse batchCardFieldValues(IssueFieldValuesBatchRequest request) {
        Map<UUID, List<VisibleFieldResponse>> byIssue = new LinkedHashMap<>();
        if (request.getIssueIds() == null || request.getFieldKeys() == null) {
            return IssueFieldValuesBatchResponse.fromUuidMap(byIssue);
        }

        UUID projectId = request.getProjectId();
        List<String> allowedKeys = request.getFieldKeys().stream()
                .filter(key -> isEligible(key, projectId))
                .toList();

        for (UUID issueId : request.getIssueIds()) {
            IssueVisibleFieldsResponse visible = fieldVisibilityEngine.resolveVisibleFieldsForIssue(
                    issueId, null, projectId, null, FieldScreenType.VIEW);
            List<VisibleFieldResponse> fields = visible.getFields().stream()
                    .filter(f -> allowedKeys.contains(f.getFieldKey()))
                    .filter(f -> f.getValue() != null && !String.valueOf(f.getValue()).isBlank())
                    .toList();
            byIssue.put(issueId, fields);
        }
        return IssueFieldValuesBatchResponse.fromUuidMap(byIssue);
    }

    private List<BoardCardLayoutResponse.EligibleFieldDto> listEligibleFields(UUID projectId) {
        List<BoardCardLayoutResponse.EligibleFieldDto> eligible = new ArrayList<>();
        for (CustomFieldDefinition cf : customFieldDefinitionRepository.findAllEnabled()) {
            if (!fieldVisibilityEngine.isFieldVisible(
                    cf.getFieldKey(), projectId, null, FieldScreenType.VIEW, null)) {
                continue;
            }
            String displayName = fieldDefinitionRepository.findByFieldKey(cf.getFieldKey())
                    .map(FieldDefinition::getDisplayName)
                    .orElse(cf.getName());
            String fieldType = fieldDefinitionRepository.findByFieldKey(cf.getFieldKey())
                    .map(fd -> fd.getFieldType().name())
                    .orElse(cf.getType());
            eligible.add(BoardCardLayoutResponse.EligibleFieldDto.builder()
                    .fieldKey(cf.getFieldKey())
                    .displayName(displayName)
                    .fieldType(fieldType)
                    .custom(true)
                    .build());
        }
        eligible.sort(Comparator.comparing(BoardCardLayoutResponse.EligibleFieldDto::getDisplayName,
                String.CASE_INSENSITIVE_ORDER));
        return eligible;
    }

    private boolean isEligible(String fieldKey, UUID projectId) {
        return customFieldDefinitionRepository.findByFieldKey(fieldKey)
                .filter(cf -> Boolean.TRUE.equals(cf.getEnabled()))
                .isPresent()
                && fieldVisibilityEngine.isFieldVisible(
                fieldKey, projectId, null, FieldScreenType.VIEW, null);
    }
}
