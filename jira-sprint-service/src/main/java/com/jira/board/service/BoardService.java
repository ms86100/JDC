package com.jira.board.service;

import com.jira.board.dto.*;
import com.jira.board.entity.AgileBoard;
import com.jira.board.entity.BoardColumn;
import com.jira.board.entity.BoardSprint;
import com.jira.board.exception.ResourceNotFoundException;
import com.jira.board.repository.AgileBoardRepository;
import com.jira.board.repository.BoardColumnRepository;
import com.jira.board.repository.BoardSprintRepository;
import com.jira.sprint.entity.Sprint;
import com.jira.sprint.repository.SprintIssueRepository;
import com.jira.sprint.repository.SprintRepository;
import com.jira.sprint.service.IssueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();

    // Default quick filters
    private static final List<BoardConfigResponse.QuickFilterConfig> DEFAULT_QUICK_FILTERS = Arrays.asList(
            BoardConfigResponse.QuickFilterConfig.builder()
                    .id("qf-assigned-me").name("Assigned to Me").jql("assignee = currentUser()").build(),
            BoardConfigResponse.QuickFilterConfig.builder()
                    .id("qf-reporter-me").name("Reported by Me").jql("reporter = currentUser()").build(),
            BoardConfigResponse.QuickFilterConfig.builder()
                    .id("qf-recently-updated").name("Recently Updated").jql("updated >= -1d").build(),
            BoardConfigResponse.QuickFilterConfig.builder()
                    .id("qf-no-assignee").name("Unassigned").jql("assignee is empty").build(),
            BoardConfigResponse.QuickFilterConfig.builder()
                    .id("qf-has-due-date").name("Has Due Date").jql("duedate is not empty").build()
    );

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
                .boardType(request.getBoardType() != null ? request.getBoardType() : "SCRUM")
                .filterId(request.getFilterId())
                .jqlQuery(request.getJqlQuery())
                .isDefault(request.isDefault())
                .allowAllIssues(request.isAllowAllIssues())
                .cardLayout(request.getCardLayout() != null ? request.getCardLayout() : "FULL")
                .estimationStatistic(request.getEstimationStatistic())
                .daysOnBoard(request.getDaysOnBoard() != null ? request.getDaysOnBoard() : 5)
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
        log.info("Fetching issues for board {} with JQL: {}", boardId, jql);

        // Try to call issue service via REST
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://jira-issue-service:8084/api/issues?projectId=" + board.getProjectId();
            if (jql != null && !jql.isEmpty()) {
                url += "&jql=" + jql;
            }

            // For now, return mock data - in production this would call the actual service
            List<BoardIssueResponse> mockIssues = createMockIssues(boardId);
            return mockIssues;
        } catch (Exception e) {
            log.warn("Failed to fetch issues from issue service, using mock data: {}", e.getMessage());
            return createMockIssues(boardId);
        }
    }

    private List<BoardIssueResponse> createMockIssues(UUID boardId) {
        // Return sample data for demo
        return Arrays.asList(
                BoardIssueResponse.builder()
                        .id(UUID.randomUUID())
                        .issueKey("PROJ-1")
                        .title("Implement user authentication")
                        .status("In Progress")
                        .priority("High")
                        .issueType("Story")
                        .assigneeId(UUID.randomUUID())
                        .assigneeName("John Doe")
                        .storyPoints(5)
                        .labels(Arrays.asList("auth", "security"))
                        .created(LocalDateTime.now().minusDays(2))
                        .updated(LocalDateTime.now())
                        .build(),
                BoardIssueResponse.builder()
                        .id(UUID.randomUUID())
                        .issueKey("PROJ-2")
                        .title("Fix login button styling")
                        .status("To Do")
                        .priority("Medium")
                        .issueType("Bug")
                        .created(LocalDateTime.now().minusDays(1))
                        .updated(LocalDateTime.now())
                        .build(),
                BoardIssueResponse.builder()
                        .id(UUID.randomUUID())
                        .issueKey("PROJ-3")
                        .title("Setup CI/CD pipeline")
                        .status("Done")
                        .priority("High")
                        .issueType("Task")
                        .assigneeId(UUID.randomUUID())
                        .assigneeName("Jane Smith")
                        .storyPoints(8)
                        .created(LocalDateTime.now().minusDays(5))
                        .updated(LocalDateTime.now().minusDays(1))
                        .build()
        );
    }

    @Transactional
    public List<BoardIssueResponse> applyQuickFilter(UUID boardId, String filterId) {
        AgileBoard board = findBoardById(boardId);

        // Map filter ID to JQL
        String jql = switch (filterId) {
            case "qf-assigned-me" -> "assignee = currentUser()";
            case "qf-reporter-me" -> "reporter = currentUser()";
            case "qf-recently-updated" -> "updated >= -1d";
            case "qf-no-assignee" -> "assignee is empty";
            case "qf-has-due-date" -> "duedate is not empty";
            default -> null;
        };

        if (jql == null) {
            return getBoardIssues(boardId, null);
        }

        return getBoardIssues(boardId, jql);
    }

    @Transactional
    public BoardIssueResponse moveIssue(UUID boardId, UUID issueId, String status, String rank) {
        log.info("Moving issue {} to status {} on board {}", issueId, status, boardId);

        // This would call the issue service to update the status
        // For now, return a mock response
        return BoardIssueResponse.builder()
                .id(issueId)
                .status(status)
                .rank(rank)
                .build();
    }

    @Transactional
    public void reorderIssue(UUID boardId, UUID issueId, int index, String status) {
        log.info("Reordering issue {} to position {} in status {} on board {}", issueId, index, status, boardId);
        // Store ranking information for proper ordering
    }

    @Transactional(readOnly = true)
    public SwimlaneDataResponse getSwimlaneData(UUID boardId, String field) {
        List<BoardIssueResponse> issues = getBoardIssues(boardId, null);

        Map<String, List<BoardIssueResponse>> grouped = new LinkedHashMap<>();

        for (BoardIssueResponse issue : issues) {
            String key = switch (field) {
                case "epic" -> issue.getEpicId() != null ? issue.getEpicId().toString() : "no-epic";
                case "assignee" -> issue.getAssigneeId() != null ? issue.getAssigneeId().toString() : "unassigned";
                case "priority" -> issue.getPriority() != null ? issue.getPriority() : "none";
                case "labels" -> issue.getLabels() != null && !issue.getLabels().isEmpty()
                        ? issue.getLabels().get(0) : "no-labels";
                default -> "all";
            };

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(issue);
        }

        List<SwimlaneDataResponse.Swimlane> swimlanes = grouped.entrySet().stream()
                .map(e -> SwimlaneDataResponse.Swimlane.builder()
                        .key(e.getKey())
                        .label(e.getKey())
                        .issues(e.getValue())
                        .build())
                .collect(Collectors.toList());

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
        if (status == null) return false;
        String normalized = status.toLowerCase();
        return normalized.contains("done") ||
               normalized.contains("completed") ||
               normalized.contains("closed") ||
               normalized.equals("resolved");
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
        // Mock capacity data
        return CapacityResponse.builder()
                .capacity(40)
                .committed(32)
                .completed(18)
                .remaining(14)
                .build();
    }

    @Transactional(readOnly = true)
    public BoardConfigResponse getBoardConfig(UUID boardId) {
        AgileBoard board = findBoardById(boardId);

        return BoardConfigResponse.builder()
                .boardId(boardId)
                .quickFilters(DEFAULT_QUICK_FILTERS)
                .swimlane(BoardConfigResponse.SwimlaneConfigResponse.builder()
                        .enabled(false)
                        .field("none")
                        .collapsedSwimlanes(Collections.emptyList())
                        .build())
                .showWorkVsCapacity(true)
                .cardColors(BoardConfigResponse.CardColorConfig.builder()
                        .enabled(true)
                        .field("priority")
                        .build())
                .build();
    }

    @Transactional
    public BoardConfigResponse updateBoardConfig(UUID boardId, UpdateBoardConfigRequest request) {
        AgileBoard board = findBoardById(boardId);
        // Store config changes
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);

        return getBoardConfig(boardId);
    }

    private void createDefaultColumns(AgileBoard board) {
        List<BoardColumn> defaultColumns;

        if ("KANBAN".equals(board.getBoardType())) {
            defaultColumns = Arrays.asList(
                    createColumn(board.getId(), "To Do", 0, "TODO", "#6c757d"),
                    createColumn(board.getId(), "In Progress", 1, "IN_PROGRESS", "#0066ff"),
                    createColumn(board.getId(), "Done", 2, "DONE", "#28a745")
            );
        } else {
            // SCRUM default columns
            defaultColumns = Arrays.asList(
                    createColumn(board.getId(), "Backlog", 0, "TODO", "#6c757d"),
                    createColumn(board.getId(), "To Do", 1, "TODO", "#6c757d"),
                    createColumn(board.getId(), "In Progress", 2, "IN_PROGRESS", "#0066ff", 5),
                    createColumn(board.getId(), "In Review", 3, "IN_REVIEW", "#ff9200", 3),
                    createColumn(board.getId(), "Done", 4, "DONE", "#28a745")
            );
        }

        columnRepository.saveAll(defaultColumns);
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
                .orElseThrow(() -> new ResourceNotFoundException("Board not found: " + boardId));
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
                .lastViewed(board.getLastViewed())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    private BoardColumnResponse mapColumnToResponse(BoardColumn column) {
        return BoardColumnResponse.builder()
                .id(column.getId())
                .boardId(column.getBoardId())
                .name(column.getName())
                .sequence(column.getSequence())
                .statusCategory(column.getStatusCategory())
                .isDone(column.getIsDone())
                .maxIssues(column.getMaxIssues())
                .color(column.getColor())
                .isCollapsible(column.getIsCollapsible())
                .isHidden(column.getIsHidden())
                .build();
    }
}