package com.jira.board.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderIssueRequest {
    private int index;
    private String status;
}