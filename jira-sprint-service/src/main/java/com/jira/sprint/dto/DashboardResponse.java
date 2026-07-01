package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private Boolean isDefault;
    private Boolean isGlobal;
    private List<GadgetResponse> gadgets;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}