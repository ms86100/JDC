package com.jira.migration.persister;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Board Persister Handler
 * Handles Scrum/Kanban board creation and configuration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BoardPersisterHandler {

    @Transactional(rollbackFor = Exception.class)
    public BoardPersistResult persistBoard(Map<String, Object> boardData, UUID jobId) {
        BoardPersistResult result = new BoardPersistResult();

        try {
            String name = (String) boardData.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Board name is required");
            }

            String boardType = (String) boardData.getOrDefault("boardType", "SCRUM"); // SCRUM, KANBAN
            String projectKey = (String) boardData.get("projectKey");
            if (projectKey == null) {
                throw new IllegalArgumentException("Project key is required for board");
            }

            BoardEntity board = BoardEntity.builder()
                    .projectKey(projectKey)
                    .name(name)
                    .boardType(boardType)
                    .columnConfig((Map<String, Object>) boardData.get("columnConfig"))
                    .rankingConfig((Map<String, Object>) boardData.get("rankingConfig"))
                    .build();

            UUID boardId = persistToDatabase(board);

            // Create default columns for Kanban
            if ("KANBAN".equals(boardType)) {
                createDefaultKanbanColumns(boardId);
            }

            result.setSuccess(true);
            result.setBoardId(boardId);
            result.setBoardName(name);

            log.info("Persisted board: {} ({}) for project {}", name, boardType, projectKey);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private UUID persistToDatabase(BoardEntity board) {
        log.debug("Persisting board: {}", board.getName());
        return UUID.randomUUID();
    }

    private void createDefaultKanbanColumns(UUID boardId) {
        String[] defaultColumns = {"To Do", "In Progress", "Done"};
        for (int i = 0; i < defaultColumns.length; i++) {
            persistBoardColumn(boardId, defaultColumns[i], i, i == 2); // last column is "done"
        }
    }

    private void persistBoardColumn(UUID boardId, String name, int sequence, boolean isDone) {
        log.debug("Creating board column: {} (seq={})", name, sequence);
        // In production: Persist to board_columns table
    }

    /**
     * Configure board columns
     */
    @Transactional(rollbackFor = Exception.class)
    public void configureColumns(UUID boardId, List<Map<String, Object>> columns) {
        for (Map<String, Object> column : columns) {
            String name = (String) column.get("name");
            int sequence = (Integer) column.get("sequence");
            Boolean isDone = (Boolean) column.get("isDone");
            Integer maxIssues = (Integer) column.get("maxIssues");

            persistBoardColumn(boardId, name, sequence, isDone != null && isDone);
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class BoardEntity {
        private UUID id;
        private String projectKey;
        private String name;
        private String boardType; // SCRUM, KANBAN
        private Map<String, Object> columnConfig;
        private Map<String, Object> rankingConfig;
    }

    public static class BoardPersistResult {
        private boolean success;
        private UUID boardId;
        private String boardName;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getBoardId() { return boardId; }
        public void setBoardId(UUID boardId) { this.boardId = boardId; }
        public String getBoardName() { return boardName; }
        public void setBoardName(String boardName) { this.boardName = boardName; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}