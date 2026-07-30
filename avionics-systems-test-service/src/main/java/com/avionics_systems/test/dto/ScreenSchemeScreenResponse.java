package com.avionics_systems.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenSchemeScreenResponse {

    private UUID id;
    private UUID screenId;
    private String screenName;
    private String screenType;
}