package com.jira.test.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenConfigurationPreview {

    private UUID screenId;
    private String screenName;
    private String screenType;
    private int fieldCount;
    @Builder.Default
    private List<ScreenFieldPreview> fields = new ArrayList<>();
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}