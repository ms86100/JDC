package com.jira.plan.service;

import com.jira.plan.dto.request.CreateBoardConfigRequest;
import com.jira.plan.dto.request.CreateBoardColumnRequest;
import com.jira.plan.dto.request.CreateBoardQuickFilterRequest;
import com.jira.plan.dto.request.CreateBoardSwimlaneRequest;
import com.jira.plan.dto.request.CreateBoardCardColorRequest;
import com.jira.plan.dto.request.CreateBoardDetailFieldRequest;
import com.jira.plan.dto.response.BoardConfigResponse;
import com.jira.plan.dto.response.BoardColumnResponse;
import com.jira.plan.dto.response.BoardQuickFilterResponse;
import com.jira.plan.dto.response.BoardSwimlaneResponse;
import com.jira.plan.dto.response.BoardCardColorResponse;
import com.jira.plan.dto.response.BoardDetailFieldResponse;
import com.jira.plan.entity.*;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Board Configuration service for managing Scrum/Kanban boards.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoardConfigService {

    private final BoardConfigRepository boardConfigRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardQuickFilterRepository boardQuickFilterRepository;
    private final BoardSwimlaneRepository boardSwimlaneRepository;
    private final BoardCardColorRepository boardCardColorRepository;
    private final BoardDetailFieldRepository boardDetailFieldRepository;
    private final BoardCardLayoutFieldRepository boardCardLayoutFieldRepository;
    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<BoardConfigResponse> getBoardsByPlanId(UUID planId) {
        return boardConfigRepository.findByPlanId(planId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BoardConfigResponse getBoardById(UUID id) {
        BoardConfig board = boardConfigRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", id));
        return toResponse(board);
    }

    @Transactional
    public BoardConfigResponse createBoard(UUID planId, CreateBoardConfigRequest request) {
        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));

        BoardConfig board = BoardConfig.builder()
            .plan(plan)
            .name(request.getName())
            .boardType(request.getBoardType() != null ? request.getBoardType() : "SCRUM")
            .columnConfigMode(request.getColumnConfigMode() != null ? request.getColumnConfigMode() : "DEFAULT")
            .constraintSource(request.getConstraintSource())
            .isEnabled(true)
            .cardLayoutMode(request.getCardLayoutMode() != null ? request.getCardLayoutMode() : "COMPACT")
            .defaultSwimlane(request.getDefaultSwimlane() != null ? request.getDefaultSwimlane() : "NONE")
            .build();

        board = boardConfigRepository.save(board);

        // Create default columns based on board type
        createDefaultColumns(board, request.getBoardType());

        return toResponse(board);
    }

    @Transactional
    public BoardConfigResponse updateBoard(UUID id, CreateBoardConfigRequest request) {
        BoardConfig board = boardConfigRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", id));

        if (request.getName() != null) board.setName(request.getName());
        if (request.getBoardType() != null) board.setBoardType(request.getBoardType());
        if (request.getColumnConfigMode() != null) board.setColumnConfigMode(request.getColumnConfigMode());
        if (request.getConstraintSource() != null) board.setConstraintSource(request.getConstraintSource());
        if (request.getCardLayoutMode() != null) board.setCardLayoutMode(request.getCardLayoutMode());
        if (request.getDefaultSwimlane() != null) board.setDefaultSwimlane(request.getDefaultSwimlane());
        if (request.getIsEnabled() != null) board.setIsEnabled(request.getIsEnabled());

        board = boardConfigRepository.save(board);
        return toResponse(board);
    }

    @Transactional
    public void deleteBoard(UUID id) {
        BoardConfig board = boardConfigRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", id));
        boardConfigRepository.delete(board);
    }

    // Column management

    @Transactional
    public BoardColumnResponse addColumn(UUID boardId, CreateBoardColumnRequest request) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        int maxSequence = board.getColumns().stream()
            .mapToInt(BoardColumn::getSequence)
            .max()
            .orElse(-1);

        BoardColumn column = BoardColumn.builder()
            .boardConfig(board)
            .name(request.getName())
            .sequence(request.getSequence() != null ? request.getSequence() : maxSequence + 1)
            .statusMapping(request.getStatusMapping() != null ? request.getStatusMapping() : new ArrayList<>())
            .labelValues(request.getLabelValues() != null ? request.getLabelValues() : new ArrayList<>())
            .minWidth(request.getMinWidth() != null ? request.getMinWidth() : 100)
            .maxWidth(request.getMaxWidth() != null ? request.getMaxWidth() : 600)
            .color(request.getColor())
            .maxIssues(request.getMaxIssues())
            .constraintStatus(request.getConstraintStatus())
            .build();

        column = boardColumnRepository.save(column);
        return toColumnResponse(column);
    }

    @Transactional
    public BoardColumnResponse updateColumn(UUID columnId, CreateBoardColumnRequest request) {
        BoardColumn column = boardColumnRepository.findById(columnId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", "id", columnId));

        if (request.getName() != null) column.setName(request.getName());
        if (request.getSequence() != null) column.setSequence(request.getSequence());
        if (request.getStatusMapping() != null) column.setStatusMapping(request.getStatusMapping());
        if (request.getLabelValues() != null) column.setLabelValues(request.getLabelValues());
        if (request.getMinWidth() != null) column.setMinWidth(request.getMinWidth());
        if (request.getMaxWidth() != null) column.setMaxWidth(request.getMaxWidth());
        if (request.getColor() != null) column.setColor(request.getColor());
        if (request.getMaxIssues() != null) column.setMaxIssues(request.getMaxIssues());
        if (request.getConstraintStatus() != null) column.setConstraintStatus(request.getConstraintStatus());

        column = boardColumnRepository.save(column);
        return toColumnResponse(column);
    }

    @Transactional
    public void deleteColumn(UUID columnId) {
        BoardColumn column = boardColumnRepository.findById(columnId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", "id", columnId));
        boardColumnRepository.delete(column);
    }

    @Transactional
    public void updateColumnsOrder(UUID boardId, List<UUID> columnIds) {
        for (int i = 0; i < columnIds.size(); i++) {
            UUID columnId = columnIds.get(i);
            int seq = i;
            BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", "id", columnId));
            column.setSequence(seq);
            boardColumnRepository.save(column);
        }
    }

    // Quick Filter management

    @Transactional
    public BoardQuickFilterResponse addQuickFilter(UUID boardId, CreateBoardQuickFilterRequest request) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        int maxSequence = board.getQuickFilters().stream()
            .mapToInt(BoardQuickFilter::getSequence)
            .max()
            .orElse(-1);

        BoardQuickFilter filter = BoardQuickFilter.builder()
            .boardConfig(board)
            .name(request.getName())
            .filterQuery(request.getFilterQuery())
            .sequence(request.getSequence() != null ? request.getSequence() : maxSequence + 1)
            .isEnabled(true)
            .icon(request.getIcon())
            .build();

        filter = boardQuickFilterRepository.save(filter);
        return toQuickFilterResponse(filter);
    }

    @Transactional
    public void deleteQuickFilter(UUID filterId) {
        boardQuickFilterRepository.deleteById(filterId);
    }

    // Swimlane management

    @Transactional
    public BoardSwimlaneResponse addSwimlane(UUID boardId, CreateBoardSwimlaneRequest request) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        int maxSequence = board.getSwimlanes().stream()
            .mapToInt(BoardSwimlane::getSequence)
            .max()
            .orElse(-1);

        BoardSwimlane swimlane = BoardSwimlane.builder()
            .boardConfig(board)
            .name(request.getName())
            .groupingField(request.getGroupingField())
            .enabled(true)
            .collapsedByDefault(request.getCollapsedByDefault() != null ? request.getCollapsedByDefault() : false)
            .sequence(request.getSequence() != null ? request.getSequence() : maxSequence + 1)
            .build();

        swimlane = boardSwimlaneRepository.save(swimlane);
        return toSwimlaneResponse(swimlane);
    }

    @Transactional
    public void deleteSwimlane(UUID swimlaneId) {
        boardSwimlaneRepository.deleteById(swimlaneId);
    }

    // Card Color management

    @Transactional
    public BoardCardColorResponse addCardColor(UUID boardId, CreateBoardCardColorRequest request) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        int maxSequence = board.getCardColors().stream()
            .mapToInt(BoardCardColor::getSequence)
            .max()
            .orElse(-1);

        BoardCardColor cardColor = BoardCardColor.builder()
            .boardConfig(board)
            .name(request.getName())
            .color(request.getColor())
            .conditions(request.getConditions() != null ? request.getConditions() : new ArrayList<>())
            .sequence(request.getSequence() != null ? request.getSequence() : maxSequence + 1)
            .enabled(true)
            .build();

        cardColor = boardCardColorRepository.save(cardColor);
        return toCardColorResponse(cardColor);
    }

    @Transactional
    public void deleteCardColor(UUID colorId) {
        boardCardColorRepository.deleteById(colorId);
    }

    // Detail Field management

    @Transactional
    public BoardDetailFieldResponse addDetailField(UUID boardId, CreateBoardDetailFieldRequest request) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        int maxSequence = board.getDetailFields().stream()
            .mapToInt(BoardDetailField::getSequence)
            .max()
            .orElse(-1);

        BoardDetailField field = BoardDetailField.builder()
            .boardConfig(board)
            .fieldKey(request.getFieldKey())
            .fieldLabel(request.getFieldLabel())
            .sequence(request.getSequence() != null ? request.getSequence() : maxSequence + 1)
            .isVisible(true)
            .fieldType(request.getFieldType() != null ? request.getFieldType() : "STANDARD")
            .build();

        field = boardDetailFieldRepository.save(field);
        return toDetailFieldResponse(field);
    }

    @Transactional
    public void deleteDetailField(UUID fieldId) {
        boardDetailFieldRepository.deleteById(fieldId);
    }

    // Helper methods

    private void createDefaultColumns(BoardConfig board, String boardType) {
        List<BoardColumn> defaultColumns;

        if ("KANBAN".equalsIgnoreCase(boardType)) {
            defaultColumns = Arrays.asList(
                createColumn(board, "To Do", 0, Arrays.asList("OPEN", "REOPENED", "ASSIGNED")),
                createColumn(board, "In Progress", 1, Arrays.asList("IN_PROGRESS")),
                createColumn(board, "Done", 2, Arrays.asList("RESOLVED", "CLOSED"))
            );
        } else {
            // SCRUM board
            defaultColumns = Arrays.asList(
                createColumn(board, "Backlog", 0, Arrays.asList("OPEN")),
                createColumn(board, "To Do", 1, Arrays.asList("ASSIGNED")),
                createColumn(board, "In Progress", 2, Arrays.asList("IN_PROGRESS")),
                createColumn(board, "In Review", 3, Arrays.asList("IN_REVIEW")),
                createColumn(board, "Done", 4, Arrays.asList("RESOLVED", "CLOSED"))
            );
        }

        for (BoardColumn column : defaultColumns) {
            boardColumnRepository.save(column);
        }
    }

    private BoardColumn createColumn(BoardConfig board, String name, int sequence, List<String> statuses) {
        return BoardColumn.builder()
            .boardConfig(board)
            .name(name)
            .sequence(sequence)
            .statusMapping(new ArrayList<>(statuses))
            .labelValues(new ArrayList<>())
            .minWidth(100)
            .maxWidth(600)
            .build();
    }

    private BoardConfigResponse toResponse(BoardConfig board) {
        return BoardConfigResponse.builder()
            .id(board.getId())
            .planId(board.getPlan() != null ? board.getPlan().getId() : null)
            .name(board.getName())
            .boardType(board.getBoardType())
            .columnConfigMode(board.getColumnConfigMode())
            .constraintSource(board.getConstraintSource())
            .isEnabled(board.getIsEnabled())
            .cardLayoutMode(board.getCardLayoutMode())
            .defaultSwimlane(board.getDefaultSwimlane())
            .columns(board.getColumns().stream().map(this::toColumnResponse).collect(Collectors.toList()))
            .quickFilters(board.getQuickFilters().stream().map(this::toQuickFilterResponse).collect(Collectors.toList()))
            .swimlanes(board.getSwimlanes().stream().map(this::toSwimlaneResponse).collect(Collectors.toList()))
            .cardColors(board.getCardColors().stream().map(this::toCardColorResponse).collect(Collectors.toList()))
            .detailFields(board.getDetailFields().stream().map(this::toDetailFieldResponse).collect(Collectors.toList()))
            .createdAt(board.getCreatedAt())
            .updatedAt(board.getUpdatedAt())
            .build();
    }

    private BoardColumnResponse toColumnResponse(BoardColumn column) {
        return BoardColumnResponse.builder()
            .id(column.getId())
            .name(column.getName())
            .sequence(column.getSequence())
            .statusMapping(column.getStatusMapping())
            .labelValues(column.getLabelValues())
            .minWidth(column.getMinWidth())
            .maxWidth(column.getMaxWidth())
            .color(column.getColor())
            .maxIssues(column.getMaxIssues())
            .constraintStatus(column.getConstraintStatus())
            .build();
    }

    private BoardQuickFilterResponse toQuickFilterResponse(BoardQuickFilter filter) {
        return BoardQuickFilterResponse.builder()
            .id(filter.getId())
            .name(filter.getName())
            .filterQuery(filter.getFilterQuery())
            .sequence(filter.getSequence())
            .isEnabled(filter.getIsEnabled())
            .icon(filter.getIcon())
            .build();
    }

    private BoardSwimlaneResponse toSwimlaneResponse(BoardSwimlane swimlane) {
        return BoardSwimlaneResponse.builder()
            .id(swimlane.getId())
            .name(swimlane.getName())
            .groupingField(swimlane.getGroupingField())
            .enabled(swimlane.getEnabled())
            .collapsedByDefault(swimlane.getCollapsedByDefault())
            .sequence(swimlane.getSequence())
            .build();
    }

    private BoardCardColorResponse toCardColorResponse(BoardCardColor cardColor) {
        return BoardCardColorResponse.builder()
            .id(cardColor.getId())
            .name(cardColor.getName())
            .color(cardColor.getColor())
            .conditions(cardColor.getConditions())
            .sequence(cardColor.getSequence())
            .enabled(cardColor.getEnabled())
            .build();
    }

    private BoardDetailFieldResponse toDetailFieldResponse(BoardDetailField field) {
        return BoardDetailFieldResponse.builder()
            .id(field.getId())
            .fieldKey(field.getFieldKey())
            .fieldLabel(field.getFieldLabel())
            .sequence(field.getSequence())
            .isVisible(field.getIsVisible())
            .fieldType(field.getFieldType())
            .build();
    }
}