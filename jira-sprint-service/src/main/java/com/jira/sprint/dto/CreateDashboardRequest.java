package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDashboardRequest {
    private String name;
    private String description;
    private Boolean isDefault;
    private Boolean isGlobal;
}