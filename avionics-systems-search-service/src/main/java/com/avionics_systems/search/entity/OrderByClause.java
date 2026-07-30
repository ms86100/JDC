package com.avionics_systems.search.entity;

import lombok.*;

/**
 * Order By Clause - Represents an ORDER BY component in JQL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderByClause {
    private String field;
    @Builder.Default
    private String direction = "ASC";
    private boolean ascending() { return "ASC".equalsIgnoreCase(direction); }
}