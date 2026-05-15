package com.jira.board.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityResponse {
    private int capacity;
    private int committed;
    private int completed;
    private int remaining;
}