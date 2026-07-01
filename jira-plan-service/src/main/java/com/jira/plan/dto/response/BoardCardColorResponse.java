package com.jira.plan.dto.response;

import com.jira.plan.entity.BoardCardColor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCardColorResponse {
    private UUID id;
    private String name;
    private String color;
    private List<BoardCardColor.CardColorCondition> conditions;
    private Integer sequence;
    private Boolean enabled;
}