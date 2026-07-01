package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderAccessEntry {
    private UUID id;
    private String name;
    private String email;
    private String type;
    private String permissionLevel;
    private LocalDateTime grantedAt;
    private UUID grantedBy;
}
