package com.jira.test.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldOptionsResponse {

    private UUID fieldId;
    private String fieldType;
    @Builder.Default
    private List<CustomFieldOption> options = new ArrayList<>();
}