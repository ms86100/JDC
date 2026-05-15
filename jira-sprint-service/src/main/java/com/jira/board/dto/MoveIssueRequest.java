package com.jira.board.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveIssueRequest {
    private String status;
    private String rank;
}