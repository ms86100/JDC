package com.avionics_systems.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveFolderRequest {

    private UUID newParentId;

    private Integer sortOrder;
}