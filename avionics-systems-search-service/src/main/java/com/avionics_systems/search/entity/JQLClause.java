package com.avionics_systems.search.entity;

import lombok.*;

/**
 * JQL Clause - Represents a single condition in JQL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JQLClause {
    private String field;
    private String operator;
    private String value;
    private String rawValue;  // Original value before processing
    private JQLFunction function;  // If this uses a JQL function
}