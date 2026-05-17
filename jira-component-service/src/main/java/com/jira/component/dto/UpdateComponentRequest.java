package com.jira.component.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateComponentRequest {
    private String name;
    private String description;
    private UUID leadUserId;
    private String assigneeType;
    private UUID defaultAssignee;
    private String color;
    private String icon;
    private Integer sequence;
}