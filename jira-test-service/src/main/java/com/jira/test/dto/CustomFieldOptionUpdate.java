package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldOptionUpdate {

    private String value;
    private String label;
    private Integer position;
}