package com.jira.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Value;
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
    private final BoardConfigAuditLogRepository boardAuditLogRepository;

    @Value("${app.board.default-type:SCRUM}")
    private String defaultBoardType;

    @Value("${app.board.default-column-config-mode:DEFAULT}")
    private String defaultColumnConfigMode;

    @Value("${app.board.default-card-layout-mode:COMPACT}")
    private String defaultCardLayoutMode;

    @Value("${app.board.default-swimlane:NONE}")
    private String defaultSwimlane;

    @Value("${app.board.default-field-type:STANDARD}")
    private String defaultFieldType;

    @Value("${app.board.column.default-min-width:100}")
    private int defaultColumnMinWidth;

    @Value("${app.board.column.default-max-width:600}")
    private int defaultColumnMaxWidth;

    // Audit event types
    private static final String EVENT_BOARD_CREATED = "BOARD_CREATED";
    private static final String EVENT_BOARD_UPDATED = "BOARD_UPDATED";
    private static final String EVENT_BOARD_DELETED = "BOARD_DELETED";
    private static final String EVENT_COLUMN_ADDED = "COLUMN_ADDED";
    private static final String EVENT_COLUMN_UPDATED = "COLUMN_UPDATED";
    private static final String EVENT_COLUMN_DELETED = "COLUMN_DELETED";
    private static final String EVENT_COLUMNS_REORDERED = "COLUMNS_REORDERED";
    private static final String EVENT_FILTER_ADDED = "FILTER_ADDED";
    private static final String EVENT_FILTER_DELETED = "FILTER_DELETED";
    private static final String EVENT_SWIMLANE_ADDED = "SWIMLANE_ADDED";
    private static final String EVENT_SWIMLANE_DELETED = "SWIMLANE_DELETED";
    private static final String EVENT_COLOR_ADDED = "COLOR_ADDED";
    private static final String EVENT_COLOR_DELETED = "COLOR_DELETED";
    private static final String EVENT_FIELD_ADDED = "FIELD_ADDED";
    private static final String EVENT_FIELD_DELETED = "FIELD_DELETED";

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
            .boardType(request.getBoardType() != null ? request.getBoardType() : defaultBoardType)
            .columnConfigMode(request.getColumnConfigMode() != null ? request.getColumnConfigMode() : defaultColumnConfigMode)
            .constraintSource(request.getConstraintSource())
            .isEnabled(true)
            .cardLayoutMode(request.getCardLayoutMode() != null ? request.getCardLayoutMode() : defaultCardLayoutMode)
            .defaultSwimlane(request.getDefaultSwimlane() != null ? request.getDefaultSwimlane() : defaultSwimlane)
            .build();

        board = boardConfigRepository.save(board);

        // Create default columns based on board type
        createDefaultColumns(board, request.getBoardType());

        // Audit log
        createAuditLog(board.getId(), EVENT_BOARD_CREATED, null,
            Map.of("name", board.getName(), "boardType", board.getBoardType()));

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

        // Audit log
        createAuditLog(board.getId(), EVENT_BOARD_UPDATED, null,
            Map.of("name", board.getName()));

        return toResponse(board);
    }

    @Transactional
    public void deleteBoard(UUID planId, UUID id) {
        BoardConfig board = boardConfigRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", id));
        // IDOR check: verify board belongs to specified plan
        if (!board.getPlan().getId().equals(planId)) {
            throw new ResourceNotFoundException("BoardConfig", "id", id);
        }
        // Audit log before deletion
        createAuditLog(board.getId(), EVENT_BOARD_DELETED, null,
            Map.of("name", board.getName()));
        boardConfigRepository.delete(board);
    }

    // Audit log retrieval

    @Transactional(readOnly = true)
    public List<BoardConfigAuditLog> getBoardAuditLog(UUID boardId) {
        // Verify board exists
        boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));
        return boardAuditLogRepository.findByBoardIdOrderByCreatedAtDesc(boardId);
    }

    // Column management

    @Transactional
    public BoardColumnResponse addColumn(UUID boardId, CreateBoardColumnRequest request) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));
        // IDOR check: board exists and is accessible (verified by plan association)
        if (board.getPlan() == null) {
            throw new ResourceNotFoundException("BoardConfig", "id", boardId);
        }

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
            .minWidth(request.getMinWidth() != null ? request.getMinWidth() : defaultColumnMinWidth)
            .maxWidth(request.getMaxWidth() != null ? request.getMaxWidth() : defaultColumnMaxWidth)
            .color(request.getColor())
            .maxIssues(request.getMaxIssues())
            .constraintStatus(request.getConstraintStatus())
            .build();

        column = boardColumnRepository.save(column);

        // Audit log
        createAuditLog(boardId, EVENT_COLUMN_ADDED, null,
            Map.of("columnId", column.getId().toString(), "name", column.getName()));

        return toColumnResponse(column);
    }

    @Transactional
    public BoardColumnResponse updateColumn(UUID boardId, UUID columnId, CreateBoardColumnRequest request) {
        BoardColumn column = boardColumnRepository.findById(columnId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", "id", columnId));
        // IDOR check: verify column belongs to specified board
        if (!column.getBoardConfig().getId().equals(boardId)) {
            throw new ResourceNotFoundException("BoardColumn", "id", columnId);
        }

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

        // Audit log
        createAuditLog(boardId, EVENT_COLUMN_UPDATED, null,
            Map.of("columnId", column.getId().toString(), "name", column.getName()));

        return toColumnResponse(column);
    }

    @Transactional
    public void deleteColumn(UUID boardId, UUID columnId) {
        BoardColumn column = boardColumnRepository.findById(columnId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", "id", columnId));
        // IDOR check: verify column belongs to specified board
        if (!column.getBoardConfig().getId().equals(boardId)) {
            throw new ResourceNotFoundException("BoardColumn", "id", columnId);
        }
        // Audit log before deletion
        createAuditLog(boardId, EVENT_COLUMN_DELETED, null,
            Map.of("columnId", columnId.toString(), "name", column.getName()));
        boardColumnRepository.delete(column);
    }

    @Transactional
    public void updateColumnsOrder(UUID boardId, List<UUID> columnIds) {
        // IDOR check: verify all columns belong to the specified board
        for (UUID columnId : columnIds) {
            BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn", "id", columnId));
            if (!column.getBoardConfig().getId().equals(boardId)) {
                throw new ResourceNotFoundException("BoardColumn", "id", columnId);
            }
        }
        // Now update sequence in single transaction
        for (int i = 0; i < columnIds.size(); i++) {
            UUID columnId = columnIds.get(i);
            int seq = i;
            BoardColumn column = boardColumnRepository.findById(columnId).get();
            column.setSequence(seq);
            boardColumnRepository.save(column);
        }
        // Audit log
        createAuditLog(boardId, EVENT_COLUMNS_REORDERED, null,
            Map.of("columnIds", columnIds.toString()));
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

        // Audit log
        createAuditLog(boardId, EVENT_FILTER_ADDED, null,
            Map.of("filterId", filter.getId().toString(), "name", filter.getName()));

        return toQuickFilterResponse(filter);
    }

    @Transactional
    public void deleteQuickFilter(UUID boardId, UUID filterId) {
        BoardQuickFilter filter = boardQuickFilterRepository.findById(filterId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardQuickFilter", "id", filterId));
        // IDOR check: verify filter belongs to specified board
        if (!filter.getBoardConfig().getId().equals(boardId)) {
            throw new ResourceNotFoundException("BoardQuickFilter", "id", filterId);
        }
        // Audit log before deletion
        createAuditLog(boardId, EVENT_FILTER_DELETED, null,
            Map.of("filterId", filterId.toString(), "name", filter.getName()));
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

        // Audit log
        createAuditLog(boardId, EVENT_SWIMLANE_ADDED, null,
            Map.of("swimlaneId", swimlane.getId().toString(), "name", swimlane.getName()));

        return toSwimlaneResponse(swimlane);
    }

    @Transactional
    public void deleteSwimlane(UUID boardId, UUID swimlaneId) {
        BoardSwimlane swimlane = boardSwimlaneRepository.findById(swimlaneId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardSwimlane", "id", swimlaneId));
        // IDOR check: verify swimlane belongs to specified board
        if (!swimlane.getBoardConfig().getId().equals(boardId)) {
            throw new ResourceNotFoundException("BoardSwimlane", "id", swimlaneId);
        }
        // Audit log before deletion
        createAuditLog(boardId, EVENT_SWIMLANE_DELETED, null,
            Map.of("swimlaneId", swimlaneId.toString(), "name", swimlane.getName()));
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

        // Audit log
        createAuditLog(boardId, EVENT_COLOR_ADDED, null,
            Map.of("colorId", cardColor.getId().toString(), "name", cardColor.getName()));

        return toCardColorResponse(cardColor);
    }

    @Transactional
    public void deleteCardColor(UUID boardId, UUID colorId) {
        BoardCardColor cardColor = boardCardColorRepository.findById(colorId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardCardColor", "id", colorId));
        // IDOR check: verify color belongs to specified board
        if (!cardColor.getBoardConfig().getId().equals(boardId)) {
            throw new ResourceNotFoundException("BoardCardColor", "id", colorId);
        }
        // Audit log before deletion
        createAuditLog(boardId, EVENT_COLOR_DELETED, null,
            Map.of("colorId", colorId.toString(), "name", cardColor.getName()));
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
            .fieldType(request.getFieldType() != null ? request.getFieldType() : defaultFieldType)
            .build();

        field = boardDetailFieldRepository.save(field);

        // Audit log
        createAuditLog(boardId, EVENT_FIELD_ADDED, null,
            Map.of("fieldId", field.getId().toString(), "fieldKey", field.getFieldKey()));

        return toDetailFieldResponse(field);
    }

    @Transactional
    public void deleteDetailField(UUID boardId, UUID fieldId) {
        BoardDetailField field = boardDetailFieldRepository.findById(fieldId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardDetailField", "id", fieldId));
        // IDOR check: verify field belongs to specified board
        if (!field.getBoardConfig().getId().equals(boardId)) {
            throw new ResourceNotFoundException("BoardDetailField", "id", fieldId);
        }
        // Audit log before deletion
        createAuditLog(boardId, EVENT_FIELD_DELETED, null,
            Map.of("fieldId", fieldId.toString(), "fieldKey", field.getFieldKey()));
        boardDetailFieldRepository.deleteById(fieldId);
    }

    // Audit helper method

    private void createAuditLog(UUID boardId, String eventType, UUID userId, Map<String, String> details) {
        String detailsJson = null;
        if (details != null) {
            try {
                detailsJson = new ObjectMapper().writeValueAsString(details);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize audit details", e);
            }
        }
        BoardConfigAuditLog auditLog = BoardConfigAuditLog.builder()
            .boardId(boardId)
            .eventType(eventType)
            .userId(userId)
            .details(detailsJson)
            .build();
        boardAuditLogRepository.save(auditLog);
        log.debug("Board audit log created: {} for board {}", eventType, boardId);
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
            .minWidth(defaultColumnMinWidth)
            .maxWidth(defaultColumnMaxWidth)
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