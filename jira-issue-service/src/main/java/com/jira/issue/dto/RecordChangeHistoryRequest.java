package com.jira.issue.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RecordChangeHistoryRequest {
    private UUID authorId;
    private String authorName;
    private List<ChangeItemInput> changes;

    @Data
    public static class ChangeItemInput {
        private String fieldType;
        private String field;
        private String oldValue;
        private String oldString;
        private String newValue;
        private String newString;
    }
}
