package com.avionics_systems.project.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberExportDto {

    private UUID userId;
    private String roleName;
}