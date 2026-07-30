package com.avionics_systems.board.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderIssueRequest {
    private int index;
    private String status;
}