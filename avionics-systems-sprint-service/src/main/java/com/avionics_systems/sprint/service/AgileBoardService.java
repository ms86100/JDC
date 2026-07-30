package com.avionics_systems.sprint.service;

import com.avionics_systems.sprint.dto.*;
import com.avionics_systems.sprint.entity.*;
import com.avionics_systems.sprint.exception.ResourceNotFoundException;
import com.avionics_systems.sprint.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agile Board Service - Enhanced with Avionics Systems DC features
 *
 * Supports Scrum, Kanban, and Badge boards with full configuration options
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgileBoardService {

    private final AgileBoardRepository agileBoardRepository;
    private final BoardSprintRepository boardSprintRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;
    private final MessageSource messageSource;

    @Value("${app.defaults.days-on-board:5}")
    private int defaultDaysOnBoard;

    @Value("${app.board.kanban-default-columns:To Do,In Progress,Done}")
    private String kanbanDefaultColumnsStr;

    @Value("${app.board.kanban-default-categories:TODO,IN_PROGRESS,DONE}")
    private String kanbanDefaultCategoriesStr;

    @Value("${app.board.scrum-default-columns:To Do,In Progress,In Review,Done}")
    private String scrumDefaultColumnsStr;

    @Value("${app.board.scrum-default-categories:TODO,IN_PROGRESS,IN_PROGRESS,DONE}")
    private String scrumDefaultCategoriesStr;

    @Value("${app.board.badge-default-columns:Backlog,Active,Complete}")
    private String badgeDefaultColumnsStr;

    @Value("${app.board.badge-default-categories:TODO,IN_PROGRESS,DONE}")
    private String badgeDefaultCategoriesStr;

    @Transactional
    public AgileBoardResponse createBoard(CreateBoardRequest request, UUID createdBy) {
        log.info("Creating agile board: {} for project: {}", request.getName(), request.getProjectId());

        AgileBoard board = AgileBoard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .boardType(request.getBoardType() != null ? request.getBoardType() : AgileBoard.TYPE_SCRUM)
                .filterId(request.getFilterId())
                .jqlQuery(request.getJqlQuery())
                .isDefault(request.isDefault())
                .allowAllIssues(request.getAllowAllIssues() != null ? request.getAllowAllIssues() : true)
                .isCommunity(request.isCommunity())
                .location(request.getLocation())
                .canManage(request.getCanManage() != null ? request.getCanManage() : true)
                .columnConfig(request.getColumnConfig())
                .rankingConfig(request.getRankingConfig())
                .cardLayout(request.getCardLayout() != null ? request.getCardLayout() : AgileBoard.LAYOUT_FULL)
                .estimationStatistic(request.getEstimationStatistic())
                .daysOnBoard(request.getDaysOnBoard() != null ? request.getDaysOnBoard() : defaultDaysOnBoard)
                .backlogColumn(request.getBacklogColumn())
                .createdBy(createdBy)
                .build();

        board = agileBoardRepository.save(board);

        // Create default columns based on board type
        createDefaultColumns(board.getId(), request.getBoardType());

        log.info("Created agile board: {}", board.getId());
        return mapToAgileBoardResponse(board);
    }

    private void createDefaultColumns(UUID boardId, String boardType) {
        String columnsStr;
        String categoriesStr;

        if (AgileBoard.TYPE_KANBAN.equals(boardType)) {
            columnsStr = kanbanDefaultColumnsStr;
            categoriesStr = kanbanDefaultCategoriesStr;
        } else if (AgileBoard.TYPE_SCRUM.equals(boardType)) {
            columnsStr = scrumDefaultColumnsStr;
            categoriesStr = scrumDefaultCategoriesStr;
        } else {
            columnsStr = badgeDefaultColumnsStr;
            categoriesStr = badgeDefaultCategoriesStr;
        }

        String[] names = columnsStr.split(",");
        String[] categories = categoriesStr.split(",");
        List<BoardColumn> defaultColumns = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String cat = i < categories.length ? categories[i].trim() : "TODO";
            boolean isDone = "DONE".equals(cat);
            defaultColumns.add(createColumn(boardId, names[i].trim(), i, cat, isDone));
        }

        boardColumnRepository.saveAll(defaultColumns);
    }

    private BoardColumn createColumn(UUID boardId, String name, int sequence, String category, boolean isDone) {
        return BoardColumn.builder()
                .boardId(boardId)
                .name(name)
                .sequence(sequence)
                .statusCategory(category)
                .isDone(isDone)
                .isCollapsible(true)
                .isHidden(false)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AgileBoardResponse> getBoardsByProject(UUID projectId) {
        return agileBoardRepository.findByProjectId(projectId).stream()
                .map(this::mapToAgileBoardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgileBoardResponse getBoard(UUID boardId) {
        AgileBoard board = findBoardById(boardId);
        return mapToAgileBoardResponse(board);
    }

    @Transactional
    public AgileBoardResponse updateBoard(UUID boardId, UpdateBoardRequest request) {
        AgileBoard board = findBoardById(boardId);

        if (request.getName() != null) board.setName(request.getName());
        if (request.getDescription() != null) board.setDescription(request.getDescription());
        if (request.getJqlQuery() != null) board.setJqlQuery(request.getJqlQuery());
        if (request.getFilterId() != null) board.setFilterId(request.getFilterId());
        if (request.getColumnConfig() != null) board.setColumnConfig(request.getColumnConfig());
        if (request.getCardLayout() != null) board.setCardLayout(request.getCardLayout());
        if (request.getEstimationStatistic() != null) board.setEstimationStatistic(request.getEstimationStatistic());
        if (request.getDaysOnBoard() != null) board.setDaysOnBoard(request.getDaysOnBoard());
        if (request.getBacklogColumn() != null) board.setBacklogColumn(request.getBacklogColumn());

        board = agileBoardRepository.save(board);
        log.info("Updated agile board: {}", boardId);

        return mapToAgileBoardResponse(board);
    }

    @Transactional
    public void deleteBoard(UUID boardId) {
        AgileBoard board = findBoardById(boardId);
        agileBoardRepository.delete(board);
        log.info("Deleted agile board: {}", boardId);
    }

    // Sprint Management
    @Transactional
    public BoardSprintResponse addSprintToBoard(UUID boardId, UUID sprintId) {
        AgileBoard board = findBoardById(boardId);

        // Get max sequence
        int maxSequence = boardSprintRepository.findByBoardIdOrderBySequenceAsc(boardId)
                .stream()
                .mapToInt(BoardSprint::getSequence)
                .max()
                .orElse(-1);

        BoardSprint boardSprint = BoardSprint.builder()
                .boardId(boardId)
                .sprintId(sprintId)
                .sequence(maxSequence + 1)
                .state(BoardSprint.STATE_FUTURE)
                .build();

        boardSprint = boardSprintRepository.save(boardSprint);
        log.info("Added sprint {} to board {}", sprintId, boardId);

        return mapToBoardSprintResponse(boardSprint);
    }

    @Transactional
    public BoardSprintResponse updateSprintState(UUID boardId, UUID sprintId, String newState) {
        BoardSprint boardSprint = boardSprintRepository.findByBoardIdAndSprintId(boardId, sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.board.sprint.not.found", null, Locale.ENGLISH)));

        boardSprint.setState(newState);

        if (BoardSprint.STATE_ACTIVE.equals(newState)) {
            boardSprint.setStartDate(LocalDateTime.now());
        } else if (BoardSprint.STATE_COMPLETED.equals(newState)) {
            boardSprint.setCompleteDate(LocalDateTime.now());
        }

        boardSprint = boardSprintRepository.save(boardSprint);
        return mapToBoardSprintResponse(boardSprint);
    }

    // Column Management
    @Transactional
    public BoardColumnResponse addColumn(UUID boardId, CreateColumnRequest request) {
        int maxSequence = boardColumnRepository.findByBoardIdOrderBySequenceAsc(boardId)
                .stream()
                .mapToInt(BoardColumn::getSequence)
                .max()
                .orElse(-1);

        BoardColumn column = BoardColumn.builder()
                .boardId(boardId)
                .name(request.getName())
                .sequence(maxSequence + 1)
                .statusIds(request.getStatusIds())
                .statusCategory(request.getStatusCategory())
                .isDone(request.getIsDone() != null ? request.getIsDone() : false)
                .maxIssues(request.getMaxIssues())
                .color(request.getColor())
                .isCollapsible(request.getIsCollapsible() != null ? request.getIsCollapsible() : true)
                .isHidden(request.getIsHidden() != null ? request.getIsHidden() : false)
                .build();

        column = boardColumnRepository.save(column);
        return mapToBoardColumnResponse(column);
    }

    @Transactional
    public BoardColumnResponse updateColumn(UUID columnId, UpdateColumnRequest request) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.column.not.found", new Object[]{columnId}, Locale.ENGLISH)));

        if (request.getName() != null) column.setName(request.getName());
        if (request.getStatusIds() != null) column.setStatusIds(request.getStatusIds());
        if (request.getStatusCategory() != null) column.setStatusCategory(request.getStatusCategory());
        if (request.getIsDone() != null) column.setIsDone(request.getIsDone());
        if (request.getMaxIssues() != null) column.setMaxIssues(request.getMaxIssues());
        if (request.getColor() != null) column.setColor(request.getColor());
        if (request.getIsCollapsible() != null) column.setIsCollapsible(request.getIsCollapsible());
        if (request.getIsHidden() != null) column.setIsHidden(request.getIsHidden());

        column = boardColumnRepository.save(column);
        return mapToBoardColumnResponse(column);
    }

    @Transactional
    public void deleteColumn(UUID columnId) {
        boardColumnRepository.deleteById(columnId);
    }

    @Transactional
    public void reorderColumns(UUID boardId, List<UUID> columnIds) {
        for (int i = 0; i < columnIds.size(); i++) {
            final int sequence = i;
            boardColumnRepository.findById(columnIds.get(i)).ifPresent(col -> {
                col.setSequence(sequence);
                boardColumnRepository.save(col);
            });
        }
    }

    // Board Data (for rendering)
    @Transactional(readOnly = true)
    public BoardDataResponse getBoardData(UUID boardId) {
        AgileBoard board = findBoardById(boardId);

        // Update last viewed
        board.setLastViewed(LocalDateTime.now());
        agileBoardRepository.save(board);

        // Get columns
        List<BoardColumn> columns = boardColumnRepository.findByBoardIdOrderBySequenceAsc(boardId);

        // Get sprints for Scrum boards
        List<BoardSprint> sprints = boardSprintRepository.findByBoardIdOrderBySequenceAsc(boardId);

        return BoardDataResponse.builder()
                .board(mapToAgileBoardResponse(board))
                .columns(columns.stream().map(this::mapToBoardColumnResponse).collect(Collectors.toList()))
                .sprints(sprints.stream().map(this::mapToBoardSprintResponse).collect(Collectors.toList()))
                .build();
    }

    // Velocity and Statistics
    @Transactional(readOnly = true)
    public BoardVelocityResponse getVelocity(UUID boardId) {
        List<BoardSprint> completedSprints = boardSprintRepository.findByBoardIdOrderBySequenceAsc(boardId)
                .stream()
                .filter(s -> BoardSprint.STATE_COMPLETED.equals(s.getState()))
                .collect(Collectors.toList());

        List<BoardVelocityResponse.VelocityPoint> velocityPoints = new ArrayList<>();

        for (BoardSprint sprint : completedSprints) {
            Sprint fullSprint = sprintRepository.findById(sprint.getSprintId()).orElse(null);
            if (fullSprint != null) {
                int issueCount = sprintIssueRepository.countBySprintId(sprint.getSprintId());
                velocityPoints.add(BoardVelocityResponse.VelocityPoint.builder()
                        .sprintId(sprint.getSprintId())
                        .sprintName(fullSprint.getName())
                        .completedIssues(issueCount)
                        .plannedIssues(issueCount)  // Would need actual planned count
                        .build());
            }
        }

        double averageVelocity = velocityPoints.stream()
                .mapToInt(BoardVelocityResponse.VelocityPoint::getCompletedIssues)
                .average()
                .orElse(0.0);

        return BoardVelocityResponse.builder()
                .velocityPoints(velocityPoints)
                .averageVelocity(averageVelocity)
                .build();
    }

    // Helper methods
    private AgileBoard findBoardById(UUID boardId) {
        return agileBoardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.board.not.found", new Object[]{boardId}, Locale.ENGLISH)));
    }

    private AgileBoardResponse mapToAgileBoardResponse(AgileBoard board) {
        return AgileBoardResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .projectId(board.getProjectId())
                .boardType(board.getBoardType())
                .filterId(board.getFilterId())
                .jqlQuery(board.getJqlQuery())
                .isDefault(board.getIsDefault())
                .allowAllIssues(board.getAllowAllIssues())
                .isCommunity(board.getIsCommunity())
                .location(board.getLocation())
                .canManage(board.getCanManage())
                .columnConfig(board.getColumnConfig())
                .cardLayout(board.getCardLayout())
                .estimationStatistic(board.getEstimationStatistic())
                .daysOnBoard(board.getDaysOnBoard())
                .lastViewed(board.getLastViewed())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    private BoardSprintResponse mapToBoardSprintResponse(BoardSprint boardSprint) {
        Sprint sprint = sprintRepository.findById(boardSprint.getSprintId()).orElse(null);
        return BoardSprintResponse.builder()
                .id(boardSprint.getId())
                .boardId(boardSprint.getBoardId())
                .sprintId(boardSprint.getSprintId())
                .sprintName(sprint != null ? sprint.getName() : null)
                .sequence(boardSprint.getSequence())
                .state(boardSprint.getState())
                .startDate(boardSprint.getStartDate())
                .endDate(boardSprint.getEndDate())
                .completeDate(boardSprint.getCompleteDate())
                .build();
    }

    private BoardColumnResponse mapToBoardColumnResponse(BoardColumn column) {
        return BoardColumnResponse.builder()
                .id(column.getId())
                .boardId(column.getBoardId())
                .name(column.getName())
                .sequence(column.getSequence())
                .statusIds(column.getStatusIds())
                .statusCategory(column.getStatusCategory())
                .isDone(column.getIsDone())
                .maxIssues(column.getMaxIssues())
                .color(column.getColor())
                .isCollapsible(column.getIsCollapsible())
                .isHidden(column.getIsHidden())
                .build();
    }
}