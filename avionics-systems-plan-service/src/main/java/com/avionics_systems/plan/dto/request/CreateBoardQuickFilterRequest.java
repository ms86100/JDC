package com.avionics_systems.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardQuickFilterRequest {
    private String name;
    private String filterQuery;  // JQL query
    private Integer sequence;
    private Boolean isEnabled = true;
    private String icon;
}