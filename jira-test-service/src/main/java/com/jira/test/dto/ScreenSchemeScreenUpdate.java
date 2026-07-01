package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenSchemeScreenUpdate {

    private String screenType;
    private UUID screenId;
}