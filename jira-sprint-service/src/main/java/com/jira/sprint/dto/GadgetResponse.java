package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GadgetResponse {
    private UUID id;
    private String gadgetType;
    private String title;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private Map<String, Object> preferences;
}