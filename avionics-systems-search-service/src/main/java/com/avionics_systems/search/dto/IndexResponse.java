package com.avionics_systems.search.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexResponse {

    private UUID id;
    private String entityType;
    private UUID entityId;
    private String message;
}