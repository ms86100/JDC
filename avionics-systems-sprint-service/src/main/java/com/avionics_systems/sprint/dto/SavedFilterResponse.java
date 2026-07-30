package com.avionics_systems.sprint.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedFilterResponse {
    private UUID id;
    private String name;
    private String jql;
    private String owner;
    private Boolean isShared;
    private Boolean favorite;
    private String shareType;
    private Boolean isSystem;
    private Integer usageCount;
    private LocalDateTime lastUsed;
    private LocalDateTime createdAt;
}