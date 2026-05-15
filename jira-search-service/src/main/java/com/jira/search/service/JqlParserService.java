package com.jira.search.service;

import com.jira.search.entity.JQLClause;
import com.jira.search.entity.JQLQuery;
import com.jira.search.entity.OrderByClause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JQL Parser - Supports Jira Query Language syntax
 * Based on Atlassian JQL specification
 */
@Service
@Slf4j
public class JqlParserService {

    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
            "(\\w+)\\s+(=|!=|>|<|>=|<=|~|!~|IN|NOT\\s+IN|IS|IS\\s+NOT)\\s+(.+?)(?=(?:\\s+AND|\\s+OR|\\s+ORDER|$))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "([a-zA-Z_]+)\\s*\\(([^)]*)\\)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LIST_PATTERN = Pattern.compile(
            "\\(([^)]+)\\)"
    );

    /**
     * Parse JQL string into structured query
     */
    public JQLQuery parse(String jql) {
        log.debug("Parsing JQL: {}", jql);

        JQLQuery query = JQLQuery.builder()
                .originalJql(jql)
                .clauses(new ArrayList<>())
                .orderByClauses(new ArrayList<>())
                .build();

        if (jql == null || jql.trim().isEmpty()) {
            return query;
        }

        // Split into WHERE and ORDER BY clauses
        String[] parts = jql.split("(?i)\\s+ORDER\\s+BY\\s+");
        String whereClause = parts[0].replaceFirst("(?i)^SELECT.*?FROM\\s+\\w+\\s*", "").trim();

        // Extract ORDER BY
        if (parts.length > 1) {
            parseOrderBy(parts[1], query);
        }

        // Parse WHERE clauses
        parseWhereClause(whereClause, query);

        return query;
    }

    /**
     * Convert parsed JQL to SQL query string
     */
    public String toSqlWhereClause(JQLQuery jql) {
        StringBuilder sql = new StringBuilder();

        for (int i = 0; i < jql.getClauses().size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            JQLClause clause = jql.getClauses().get(i);
            sql.append(clauseToSql(clause));
        }

        return sql.toString();
    }

    private void parseWhereClause(String where, JQLQuery query) {
        String[] tokens = where.split("(?i)\\s+(AND|OR)\\s+");

        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty()) continue;

            JQLClause clause = parseClause(token);
            if (clause != null) {
                query.getClauses().add(clause);
            }
        }
    }

    private JQLClause parseClause(String token) {
        Matcher clauseMatcher = CLAUSE_PATTERN.matcher(token);
        if (!clauseMatcher.find()) {
            log.warn("Could not parse clause: {}", token);
            return null;
        }

        String field = clauseMatcher.group(1);
        String opStr = clauseMatcher.group(2).trim();
        String value = clauseMatcher.group(3).trim();

        return JQLClause.builder()
                .field(field.toLowerCase())
                .operator(normalizeOperator(opStr))
                .value(value)
                .rawValue(value)
                .build();
    }

    private String normalizeOperator(String opStr) {
        return opStr.toUpperCase().trim();
    }

    private String parseDateExpression(String expr) {
        Pattern datePattern = Pattern.compile("(-?\\d+)d");
        Matcher m = datePattern.matcher(expr);
        if (m.find()) {
            int days = Integer.parseInt(m.group(1));
            return "now-" + Math.abs(days) + "d";
        }
        return expr;
    }

    private String cleanValue(String value) {
        if (value == null) return null;
        if ((value.startsWith("'") && value.endsWith("'")) ||
            (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        return value.trim();
    }

    private void parseOrderBy(String orderBy, JQLQuery query) {
        String[] fields = orderBy.split(",");
        for (String field : fields) {
            String[] parts = field.trim().split("\\s+");
            query.getOrderByClauses().add(OrderByClause.builder()
                    .field(field.trim())
                    .direction(parts.length > 1 ? parts[1].toUpperCase() : "ASC")
                    .build());
        }
    }

    private String clauseToSql(JQLClause clause) {
        String field = clause.getField();
        String operator = clause.getOperator();
        String value = cleanValue(clause.getValue().toString());

        if (operator.equalsIgnoreCase("IS") && value.equalsIgnoreCase("NULL")) {
            return field + " IS NULL";
        }
        if (operator.equalsIgnoreCase("IS NOT") && value.equalsIgnoreCase("NULL")) {
            return field + " IS NOT NULL";
        }

        return field + " " + operator + " '" + value + "'";
    }
}