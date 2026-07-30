package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardQuickFilterResponse {
    private UUID id;
    private String name;
    private String filterQuery;
    private Integer sequence;
    private Boolean isEnabled;
    private String icon;
}