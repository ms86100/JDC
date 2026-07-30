package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenResponse {

    private UUID id;
    private String name;
    private String screenType;
    private Integer position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}