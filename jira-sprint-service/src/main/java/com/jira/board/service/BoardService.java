package com.jira.board.service;

import com.jira.board.dto.*;
import com.jira.board.entity.*;
import com.jira.board.exception.ResourceNotFoundException;
import com.jira.board.repository.AgileBoardRepository;
import com.jira.board.repository.BoardColumnRepository;
import com.jira.board.repository.BoardConfigRepository;
import com.jira.board.repository.BoardSprintRepository;
import com.jira.cluster.util.StatusCategoryHelper;
import com.jira.sprint.entity.Sprint;
import com.jira.sprint.repository.SprintIssueRepository;
import com.jira.sprint.repository.SprintRepository;
import com.jira.sprint.service.IssueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final AgileBoardRepository boardRepository;
    private final BoardColumnRepository columnRepository;
    private final BoardSprintRepository boardSprintRepository;
    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;
    private final IssueServiceClient issueServiceClient;
    private final BoardConfigRepository boardConfigRepository;
    private final BoardConfigurationService boardConfigurationService;
    private final MessageSource messageSource;

    @Value("${app.defaults.board-type:SCRUM}")
    private String defaultBoardType;

    @Value("${app.defaults.card-layout:FULL}")
    private String defaultCardLayout;

    @Value("${app.defaults.days-on-board:5}")
    private int defaultDaysOnBoard;

    @Value("${app.board.kanban-columns:Backlog,Selected for Development,In Progress,Done}")
    private String kanbanColumnsStr;

    @Value("${app.board.kanban-column-categories:TODO,TODO,IN_PROGRESS,DONE}")
    private String kanbanColumnCategoriesStr;

    @Value("${app.board.kanban-column-colors:#6b778c,#0052cc,#ff8b00,#36b37e}")
    private String kanbanColumnColorsStr;

    @Value("${app.board.kanban-column-wip-limits:0,0,5,0}")
    private String kanbanColumnWipLimitsStr;

    @Value("${app.board.scrum-columns:Backlog,To Do,In Progress,In Review,Done}")
    private String scrumColumnsStr;

    @Value("${app.board.scrum-column-categories:TODO,TODO,IN_PROGRESS,IN_REVIEW,DONE}")
    private String scrumColumnCategoriesStr;

    @Value("${app.board.scrum-column-colors:#6c757d,#6c757d,#0066ff,#ff9200,#28a745}")
    private String scrumColumnColorsStr;

    @Value("${app.board.scrum-column-wip-limits:0,0,5,3,0}")
    private String scrumColumnWipLimitsStr;

    @Value("${app.board.quick-filter-ids:qf-assigned-me,qf-reporter-me,qf-recently-updated,qf-no-assignee,qf-has-due-date}")
    private String quickFilterIdsStr;

    @Value("${app.board.quick-filter-names:Assigned to Me,Reported by Me,Recently Updated,Unassigned,Has Due Date}")
    private String quickFilterNamesStr;

    @Value("${app.board.quick-filter-jqls:assignee = currentUser(),reporter = currentUser(),updated >= -1d,assignee is empty,duedate is not empty}")
    private String quickFilterJqlsStr;

    private List<BoardConfigResponse.QuickFilterConfig> getDefaultQuickFilters() {
        String[] ids = quickFilterIdsStr.split(",");
        String[] names = quickFilterNamesStr.split(",");
        String[] jqls = quickFilterJqlsStr.split(",");
        List<BoardConfigResponse.QuickFilterConfig> filters = new ArrayList<>();
        for (int i = 0; i < ids.length && i < names.length && i < jqls.length; i++) {
            filters.add(BoardConfigResponse.QuickFilterConfig.builder()
                    .id(ids[i].trim()).name(names[i].trim()).jql(jqls[i].trim()).build());
        }
        return filters;
    }

    @Transactional(readOnly = true)
    public List<AgileBoardResponse> getBoardsByProject(UUID projectId) {
        return boardRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgileBoardResponse getBoard(UUID boardId) {
        AgileBoard board = findBoardById(boardId);
        return mapToResponse(board);
    }

    @Transactional
    public AgileBoardResponse createBoard(CreateBoardRequest request) {
        AgileBoard board = AgileBoard.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .boardType(request.getBoardType() != null ? request.getBoardType() : defaultBoardType)
                .filterId(request.getFilterId())
                .jqlQuery(request.getJqlQuery())
                .isDefault(request.isDefault())
                .allowAllIssues(request.isAllowAllIssues())
                .cardLayout(request.getCardLayout() != null ? request.getCardLayout() : defaultCardLayout)
                .estimationStatistic(request.getEstimationStatistic())
                .daysOnBoard(request.getDaysOnBoard() != null ? request.getDaysOnBoard() : defaultDaysOnBoard)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        board = boardRepository.save(board);
        createDefaultColumns(board);

        log.info("Created board: {} ({})", board.getId(), board.getName());
        return mapToResponse(board);
    }

    @Transactional
    public AgileBoardResponse updateBoard(UUID boardId, UpdateBoardRequest request) {
        AgileBoard board = findBoardById(boardId);

        if (request.getName() != null) board.setName(request.getName());
        if (request.getDescription() != null) board.setDescription(request.getDescription());
        if (request.getJqlQuery() != null) board.setJqlQuery(request.getJqlQuery());
        if (request.getFilterId() != null) board.setFilterId(request.getFilterId());
        if (request.getCardLayout() != null) board.setCardLayout(request.getCardLayout());
        if (request.getEstimationStatistic() != null) board.setEstimationStatistic(request.getEstimationStatistic());
        if (request.getDaysOnBoard() != null) board.setDaysOnBoard(request.getDaysOnBoard());
        if (request.getTimezone() != null) board.setTimezone(request.getTimezone());
        if (request.getWorkingDays() != null) board.setWorkingDays(request.getWorkingDays());
        if (request.getNonWorkingDates() != null) board.setNonWorkingDates(request.getNonWorkingDates());
        if (request.getTimeTracking() != null) board.setTimeTracking(request.getTimeTracking());
        if (request.getKanbanBacklogEnabled() != null) board.setKanbanBacklogEnabled(request.getKanbanBacklogEnabled());
        if (request.getSubFilter() != null) board.setSubFilter(request.getSubFilter());
        if (request.getHideCompletedAfterDays() != null) board.setHideCompletedAfterDays(request.getHideCompletedAfterDays());
        if (request.getUseSimplifiedWorkflow() != null) board.setUseSimplifiedWorkflow(request.getUseSimplifiedWorkflow());
        board.setUpdatedAt(LocalDateTime.now());

        board = boardRepository.save(board);
        return mapToResponse(board);
    }

    @Transactional
    public void deleteBoard(UUID boardId) {
        AgileBoard board = findBoardById(boardId);
        boardRepository.delete(board);
        log.info("Deleted board: {}", boardId);
    }

    @Transactional
    public AgileBoardResponse copyBoard(UUID boardId, String newName) {
        AgileBoard source = findBoardById(boardId);

        String targetName = (newName != null && !newName.isBlank())
                ? newName
                : source.getName() + " (Copy)";

        AgileBoard copy = AgileBoard.builder()
                .name(targetName)
                .description(source.getDescription())
                .projectId(source.getProjectId())
                .boardType(source.getBoardType())
                .filterId(source.getFilterId())
                .jqlQuery(source.getJqlQuery())
                .isDefault(false)
                .allowAllIssues(source.getAllowAllIssues())
                .cardLayout(source.getCardLayout())
                .estimationStatistic(source.getEstimationStatistic())
                .daysOnBoard(source.getDaysOnBoard())
                .kanbanBacklogEnabled(source.getKanbanBacklogEnabled())
                .subFilter(source.getSubFilter())
                .hideCompletedAfterDays(source.getHideCompletedAfterDays())
                .useSimplifiedWorkflow(source.getUseSimplifiedWorkflow())
                .timeTracking(source.getTimeTracking())
                .timezone(source.getTimezone())
                .workingDays(source.getWorkingDays())
                .nonWorkingDates(source.getNonWorkingDates())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        copy = boardRepository.save(copy);

        copyColumns(source.getId(), copy.getId());
        copySwimlanes(source.getId(), copy.getId());
        copyCardColorRules(source.getId(), copy.getId());
        copyCardFields(source.getId(), copy.getId());
        copyIssueDetailFields(source.getId(), copy.getId());

        log.info("Copied board {} -> {} ({})", boardId, copy.getId(), targetName);
        return mapToResponse(copy);
    }

    private void copyColumns(UUID sourceId, UUID targetId) {
        List<BoardColumn> columns = columnRepository.findByBoardIdOrderBySequenceAsc(sourceId);
        for (BoardColumn col : columns) {
            columnRepository.save(BoardColumn.builder()
                    .boardId(targetId)
                    .name(col.getName())
                    .sequence(col.getSequence())
                    .statusCategory(col.getStatusCategory())
                    .isDone(col.getIsDone())
                    .maxIssues(col.getMaxIssues())
                    .color(col.getColor())
                    .isCollapsible(col.getIsCollapsible())
                    .isHidden(col.getIsHidden())
                    .showDaysInColumn(col.getShowDaysInColumn())
                    .build());
        }
    }

    private void copySwimlanes(UUID sourceId, UUID targetId) {
        List<BoardSwimlane> swimlanes = boardConfigurationService.getSwimlanes(sourceId);
        for (BoardSwimlane sl : swimlanes) {
            boardConfigurationService.createSwimlane(targetId, BoardSwimlane.builder()
                    .name(sl.getName())
                    .jqlQuery(sl.getJqlQuery())
                    .description(sl.getDescription())
                    .position(sl.getPosition())
                    .build());
        }
    }

    private void copyCardColorRules(UUID sourceId, UUID targetId) {
        List<BoardCardColorRule> rules = boardConfigurationService.getCardColorRules(sourceId);
        for (BoardCardColorRule rule : rules) {
            boardConfigurationService.createCardColorRule(targetId, BoardCardColorRule.builder()
                    .colorMethod(rule.getColorMethod())
                    .matchValue(rule.getMatchValue())
                    .color(rule.getColor())
                    .position(rule.getPosition())
                    .build());
        }
    }

    private void copyCardFields(UUID sourceId, UUID targetId) {
        List<BoardCardField> fields = boardConfigurationService.getCardFields(sourceId);
        for (BoardCardField field : fields) {
            boardConfigurationService.addCardField(targetId, BoardCardField.builder()
                    .fieldId(field.getFieldId())
                    .position(field.getPosition())
                    .build());
        }
    }

    private void copyIssueDetailFields(UUID sourceId, UUID targetId) {
        List<BoardIssueDetailField> fields = boardConfigurationService.getIssueDetailFields(sourceId);
        for (BoardIssueDetailField field : fields) {
            boardConfigurationService.addIssueDetailField(targetId, BoardIssueDetailField.builder()
                    .fieldId(field.getFieldId())
                    .fieldGroup(field.getFieldGroup())
                    .position(field.getPosition())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public BoardDataResponse getBoardData(UUID boardId) {
        AgileBoard board = findBoardById(boardId);
        List<BoardColumn> columns = columnRepository.findByBoardIdOrderBySequenceAsc(boardId);
        List<BoardSprint> sprints = boardSprintRepository.findByBoardIdOrderBySequenceAsc(boardId);

        // Find active sprint
        Optional<BoardSprint> activeSprintOpt = sprints.stream()
                .filter(s -> "ACTIVE".equals(s.getState()))
                .findFirst();

        BoardDataResponse.SprintInfo sprintInfo = null;
        if (activeSprintOpt.isPresent()) {
            BoardSprint active = activeSprintOpt.get();
            final UUID sprintId = active.getSprintId();
            Optional<Sprint> sprintOpt = sprintRepository.findById(sprintId);
            if (sprintOpt.isPresent()) {
                Sprint sprint = sprintOpt.get();
                sprintInfo = BoardDataResponse.SprintInfo.builder()
                        .id(sprint.getId())
                        .name(sprint.getName())
                        .startDate(sprint.getStartDate() != null ? sprint.getStartDate().atStartOfDay() : null)
                        .endDate(sprint.getEndDate() != null ? sprint.getEndDate().atStartOfDay() : null)
                        .capacity(sprint.getCapacity() != null ? sprint.getCapacity() : 0)
                        .committed(sprint.getGoal() != null ? sprint.getGoal().length() : 0)
                        .build();
            }
        }

        return BoardDataResponse.builder()
                .board(mapToResponse(board))
                .columns(columns.stream().map(this::mapColumnToResponse).collect(Collectors.toList()))
                .issues(getBoardIssues(boardId, null))
                .activeSprint(sprintInfo)
                .velocity(getVelocity(boardId))
                .build();
    }

    @Transactional(readOnly = true)
    public List<BoardIssueResponse> getBoardIssues(UUID boardId, String jql) {
        AgileBoard board = findBoardById(boardId);
        String effectiveJql = jql;
        if ((effectiveJql == null || effectiveJql.isBlank()) && board.getJqlQuery() != null && !board.getJqlQuery().isBlank()) {
            effectiveJql = board.getJqlQuery();
        }
        List<BoardIssueResponse> issues = issueServiceClient.fetchBoardIssues(board.getProjectId(), effectiveJql);
        issues.sort(Comparator.comparing(
                (BoardIssueResponse i) -> i.getRank() != null ? i.getRank() : "",
                Comparator.nullsLast(String::compareTo)));
        return issues;
    }

    @Transactional
    public List<BoardIssueResponse> applyQuickFilter(UUID boardId, String filterId) {
        AgileBoard board = findBoardById(boardId);

        // Map filter ID to JQL using configurable quick filters
        String jql = null;
        for (BoardConfigResponse.QuickFilterConfig qf : getDefaultQuickFilters()) {
            if (qf.getId().equals(filterId)) {
                jql = qf.getJql();
                break;
            }
        }

        if (jql == null) {
            return getBoardIssues(boardId, null);
        }

        return getBoardIssues(boardId, jql);
    }

    @Transactional
    public BoardIssueResponse moveIssue(UUID boardId, UUID issueId, String status, String rank) {
        AgileBoard board = findBoardById(boardId);
        log.info("Moving issue {} to status {} on board {}", issueId, status, boardId);
        return issueServiceClient.moveIssueStatus(issueId, board.getProjectId(), status, rank);
    }

    @Transactional
    public void reorderIssue(UUID boardId, UUID issueId, int index, String status) {
        AgileBoard board = findBoardById(boardId);
        List<BoardIssueResponse> all = getBoardIssues(boardId, null);
        List<BoardIssueResponse> inColumn = all.stream()
                .filter(i -> statusMatchesColumn(i, status))
                .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : ""))
                .collect(Collectors.toCollection(ArrayList::new));

        inColumn.removeIf(i -> i.getId().equals(issueId));
        BoardIssueResponse moved = all.stream().filter(i -> i.getId().equals(issueId)).findFirst().orElse(null);
        if (moved == null) {
            moved = issueServiceClient.getBoardIssue(issueId);
        }
        if (moved == null) return;

        int safeIndex = Math.max(0, Math.min(index, inColumn.size()));
        inColumn.add(safeIndex, moved);

        for (int i = 0; i < inColumn.size(); i++) {
            String rank = String.format("rank|%09d", (i + 1) * 1000);
            issueServiceClient.reorderIssueRank(inColumn.get(i).getId(), rank);
        }
    }

    private boolean statusMatchesColumn(BoardIssueResponse issue, String statusLabel) {
        if (issue.getStatus() == null || statusLabel == null) return false;
        return issue.getStatus().equalsIgnoreCase(statusLabel)
                || normalize(issue.getStatus()).contains(normalize(statusLabel))
                || normalize(statusLabel).contains(normalize(issue.getStatus()));
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replace("(legacy)", "").replace("(new)", "").replaceAll("[\\s_\\-()]+", "");
    }

    @Transactional(readOnly = true)
    public SwimlaneDataResponse getSwimlaneData(UUID boardId, String field) {
        List<BoardIssueResponse> issues = getBoardIssues(boardId, null);

        Map<String, List<BoardIssueResponse>> grouped = new LinkedHashMap<>();

        for (BoardIssueResponse issue : issues) {
            String key;
            String label;
            switch (field) {
                case "epic" -> {
                    key = issue.getEpicId() != null ? issue.getEpicId().toString() : "no-epic";
                    label = issue.getEpicName() != null ? issue.getEpicName() : "Issues without epic";
                }
                case "assignee" -> {
                    key = issue.getAssigneeId() != null ? issue.getAssigneeId().toString() : "unassigned";
                    label = issue.getAssigneeName() != null ? issue.getAssigneeName() : "Unassigned";
                }
                case "priority" -> {
                    key = issue.getPriority() != null ? issue.getPriority() : "none";
                    label = key;
                }
                case "labels" -> {
                    key = issue.getLabels() != null && !issue.getLabels().isEmpty()
                            ? issue.getLabels().get(0) : "no-labels";
                    label = key;
                }
                default -> {
                    key = "all";
                    label = "All issues";
                }
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(issue);
        }

        List<SwimlaneDataResponse.Swimlane> swimlanes = new ArrayList<>();
        for (Map.Entry<String, List<BoardIssueResponse>> e : grouped.entrySet()) {
            String label = e.getValue().isEmpty() ? e.getKey()
                    : switch (field) {
                case "epic" -> e.getValue().get(0).getEpicName() != null ? e.getValue().get(0).getEpicName() : "Issues without epic";
                case "assignee" -> e.getValue().get(0).getAssigneeName() != null ? e.getValue().get(0).getAssigneeName() : "Unassigned";
                default -> e.getKey();
            };
            swimlanes.add(SwimlaneDataResponse.Swimlane.builder()
                    .key(e.getKey())
                    .label(label)
                    .issues(e.getValue())
                    .build());
        }

        return SwimlaneDataResponse.builder()
                .swimlanes(swimlanes)
                .build();
    }

    @Transactional(readOnly = true)
    public VelocityResponse getVelocity(UUID boardId) {
        List<BoardSprint> completedSprints = boardSprintRepository.findByBoardIdOrderBySequenceAsc(boardId).stream()
                .filter(s -> "COMPLETED".equals(s.getState()))
                .collect(Collectors.toList());

        List<VelocityResponse.VelocityPoint> points = new ArrayList<>();

        for (BoardSprint sprint : completedSprints) {
            sprintRepository.findById(sprint.getSprintId()).ifPresent(sprintEntity -> {
                // Get real story points from issue service
                List<IssueServiceClient.IssueData> issues = issueServiceClient.getIssues(
                        getSprintIssueIds(sprintEntity.getId()));

                int completed = issues.stream()
                        .filter(i -> isCompletedStatus(i.getStatusName()))
                        .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                        .sum();

                int planned = issues.stream()
                        .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                        .sum();

                points.add(VelocityResponse.VelocityPoint.builder()
                        .sprintName(sprintEntity.getName())
                        .completed(completed)
                        .planned(planned)
                        .build());
            });
        }

        double avg = points.stream()
                .mapToInt(VelocityResponse.VelocityPoint::getCompleted)
                .average()
                .orElse(0.0);

        return VelocityResponse.builder()
                .averageVelocity(avg)
                .velocityPoints(points)
                .build();
    }

    private boolean isCompletedStatus(String status) {
        return StatusCategoryHelper.isCompleted(status);
    }

    private List<UUID> getSprintIssueIds(UUID sprintId) {
        try {
            return sprintIssueRepository.findBySprintId(sprintId).stream()
                    .map(si -> si.getIssueId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get sprint issue IDs for {}: {}", sprintId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public CapacityResponse getSprintCapacity(UUID boardId, UUID sprintId) {
        AgileBoard board = findBoardById(boardId);
        List<UUID> issueIds = getSprintIssueIds(sprintId);

        int committed = 0;
        int completed = 0;
        for (UUID issueId : issueIds) {
            try {
                var issue = issueServiceClient.getIssue(issueId);
                int points = issue.getStoryPoints() != null ? issue.getStoryPoints() : 0;
                committed += points;
                String status = issue.getStatusName();
                if (StatusCategoryHelper.isCompleted(status)) {
                    completed += points;
                }
            } catch (Exception e) {
                log.debug("Failed to get issue data for capacity: {}", e.getMessage());
            }
        }

        int capacity = board.getDaysOnBoard() * 8;
        int remaining = committed - completed;

        return CapacityResponse.builder()
                .capacity(capacity)
                .committed(committed)
                .completed(completed)
                .remaining(Math.max(0, remaining))
                .build();
    }

    @Transactional(readOnly = true)
    public BoardConfigResponse getBoardConfig(UUID boardId) {
        findBoardById(boardId);
        BoardConfig config = boardConfigRepository.findByBoardIdAndUserIdIsNull(boardId)
                .orElse(null);

        return BoardConfigResponse.builder()
                .boardId(boardId)
                .quickFilters(getDefaultQuickFilters())
                .swimlane(BoardConfigResponse.SwimlaneConfigResponse.builder()
                        .enabled(config != null && !"none".equals(config.getSwimlaneField()))
                        .field(config != null ? config.getSwimlaneField() : "none")
                        .collapsedSwimlanes(config != null && config.getCollapsedSwimlanes() != null
                                ? Arrays.asList(config.getCollapsedSwimlanes())
                                : Collections.emptyList())
                        .build())
                .showWorkVsCapacity(config == null || Boolean.TRUE.equals(config.getShowWorkVsCapacity()))
                .cardColors(BoardConfigResponse.CardColorConfig.builder()
                        .enabled(true)
                        .field(config != null ? config.getCardColorField() : "priority")
                        .build())
                .build();
    }

    @Transactional
    public BoardConfigResponse updateBoardConfig(UUID boardId, UpdateBoardConfigRequest request) {
        AgileBoard board = findBoardById(boardId);
        BoardConfig config = boardConfigRepository.findByBoardIdAndUserIdIsNull(boardId)
                .orElse(BoardConfig.builder().boardId(boardId).build());

        if (request.getSwimlane() != null) {
            config.setSwimlaneField(request.getSwimlane().getField() != null
                    ? request.getSwimlane().getField() : "none");
            if (request.getSwimlane().getCollapsedSwimlanes() != null) {
                config.setCollapsedSwimlanes(
                        request.getSwimlane().getCollapsedSwimlanes().toArray(new String[0]));
            }
        }
        if (request.getShowWorkVsCapacity() != null) {
            config.setShowWorkVsCapacity(request.getShowWorkVsCapacity());
        }
        if (request.getCardColors() != null && request.getCardColors().getField() != null) {
            config.setCardColorField(request.getCardColors().getField());
        }

        boardConfigRepository.save(config);
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);

        return getBoardConfig(boardId);
    }

    @Transactional
    public BoardColumnResponse updateColumn(UUID boardId, UUID columnId, BoardColumnResponse updates) {
        findBoardById(boardId);
        BoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.column.not.found", new Object[]{columnId}, Locale.ENGLISH)));
        if (!column.getBoardId().equals(boardId)) {
            throw new ResourceNotFoundException(messageSource.getMessage("error.column.not.on.board", null, Locale.ENGLISH));
        }
        if (updates.getName() != null) column.setName(updates.getName());
        if (updates.getMaxIssues() != null) column.setMaxIssues(updates.getMaxIssues());
        if (updates.getColor() != null) column.setColor(updates.getColor());
        column.setIsHidden(updates.isHidden());
        column = columnRepository.save(column);
        return mapColumnToResponse(column);
    }

    private void createDefaultColumns(AgileBoard board) {
        List<BoardColumn> defaultColumns;

        if ("KANBAN".equals(board.getBoardType())) {
            defaultColumns = buildColumnsFromConfig(board.getId(),
                    kanbanColumnsStr, kanbanColumnCategoriesStr, kanbanColumnColorsStr, kanbanColumnWipLimitsStr);
        } else {
            // SCRUM default columns
            defaultColumns = buildColumnsFromConfig(board.getId(),
                    scrumColumnsStr, scrumColumnCategoriesStr, scrumColumnColorsStr, scrumColumnWipLimitsStr);
        }

        columnRepository.saveAll(defaultColumns);
    }

    private List<BoardColumn> buildColumnsFromConfig(UUID boardId, String namesStr, String categoriesStr, String colorsStr, String wipLimitsStr) {
        String[] names = namesStr.split(",");
        String[] categories = categoriesStr.split(",");
        String[] colors = colorsStr.split(",");
        String[] wipLimits = wipLimitsStr.split(",");
        List<BoardColumn> columns = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String cat = i < categories.length ? categories[i].trim() : "TODO";
            String color = i < colors.length ? colors[i].trim() : "#6c757d";
            int wip = 0;
            if (i < wipLimits.length) {
                try { wip = Integer.parseInt(wipLimits[i].trim()); } catch (NumberFormatException ignored) {}
            }
            columns.add(createColumn(boardId, names[i].trim(), i, cat, color, wip > 0 ? wip : null));
        }
        return columns;
    }

    private BoardColumn createColumn(UUID boardId, String name, int sequence, String category, String color) {
        return createColumn(boardId, name, sequence, category, color, null);
    }

    private BoardColumn createColumn(UUID boardId, String name, int sequence, String category, String color, Integer maxIssues) {
        return BoardColumn.builder()
                .boardId(boardId)
                .name(name)
                .sequence(sequence)
                .statusCategory(category)
                .isDone("DONE".equals(category))
                .color(color)
                .maxIssues(maxIssues)
                .isCollapsible(true)
                .isHidden(false)
                .build();
    }

    private AgileBoard findBoardById(UUID boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.board.not.found", new Object[]{boardId}, Locale.ENGLISH)));
    }

    private AgileBoardResponse mapToResponse(AgileBoard board) {
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
                .cardLayout(board.getCardLayout())
                .estimationStatistic(board.getEstimationStatistic())
                .daysOnBoard(board.getDaysOnBoard())
                .timezone(board.getTimezone())
                .workingDays(board.getWorkingDays())
                .nonWorkingDates(board.getNonWorkingDates())
                .timeTracking(board.getTimeTracking())
                .kanbanBacklogEnabled(board.getKanbanBacklogEnabled())
                .subFilter(board.getSubFilter())
                .hideCompletedAfterDays(board.getHideCompletedAfterDays())
                .useSimplifiedWorkflow(board.getUseSimplifiedWorkflow())
                .lastViewed(board.getLastViewed())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    private BoardColumnResponse mapColumnToResponse(BoardColumn column) {
        int count = 0;
        try {
            List<BoardIssueResponse> issues = getBoardIssues(column.getBoardId(), null);
            count = (int) issues.stream().filter(i -> columnMatchesIssue(column, i)).count();
        } catch (Exception ignored) {
            /* count stays 0 */
        }
        return BoardColumnResponse.builder()
                .id(column.getId())
                .boardId(column.getBoardId())
                .name(column.getName())
                .sequence(column.getSequence())
                .statusCategory(column.getStatusCategory())
                .isDone(column.getIsDone())
                .maxIssues(column.getMaxIssues())
                .currentIssues(count)
                .color(column.getColor())
                .isCollapsible(column.getIsCollapsible())
                .isHidden(column.getIsHidden())
                .build();
    }

    private boolean columnMatchesIssue(BoardColumn column, BoardIssueResponse issue) {
        if (issue.getStatus() == null) return false;
        String status = normalize(issue.getStatus());
        String name = normalize(column.getName());
        if ("DONE".equals(column.getStatusCategory()) || Boolean.TRUE.equals(column.getIsDone())) {
            return StatusCategoryHelper.isCompleted(issue.getStatus());
        }
        if ("IN_PROGRESS".equals(column.getStatusCategory())) {
            return StatusCategoryHelper.isInProgress(issue.getStatus());
        }
        if (name.contains("backlog")) return status.contains("backlog") || status.equals("open") || status.equals("new");
        if (name.contains("selected")) return status.contains("todo") || status.contains("selected");
        return "TODO".equals(column.getStatusCategory())
                && (status.contains("todo") || status.contains("backlog"));
    }
}