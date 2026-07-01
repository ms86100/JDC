package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScreenDto {
    private String id;
    private String name;
    private String description;
    private String screenType;
    private List<ScreenTabDto> tabs;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScreenTabDto {
        private String id;
        private String tabName;
        private List<String> fieldIds;
        private Integer sequence;
    }
}