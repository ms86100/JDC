package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldOption {

    private String value;
    private String label;
    private Integer position;
}