package com.jira.plan.dto.request;

import com.jira.plan.entity.BoardCardColor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardCardColorRequest {
    private String name;
    private String color;  // Hex color
    private List<BoardCardColor.CardColorCondition> conditions;
    private Integer sequence;
    private Boolean enabled = true;
}