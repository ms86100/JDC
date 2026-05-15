package com.jira.search.entity;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JQL Query - Represents a parsed JQL query
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JQLQuery {
    private String originalJql;
    @Builder.Default
    private List<JQLClause> clauses = new ArrayList<>();
    @Builder.Default
    private List<OrderByClause> orderByClauses = new ArrayList<>();
    private LogicalOperator defaultOperator;

    public enum LogicalOperator {
        AND, OR
    }

    public void setDefaultOperator(String op) {
        if (op == null) {
            this.defaultOperator = LogicalOperator.AND;
        } else {
            this.defaultOperator = op.equalsIgnoreCase("OR") ? LogicalOperator.OR : LogicalOperator.AND;
        }
    }
}