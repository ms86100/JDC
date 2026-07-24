package com.jira.search.service;

import com.jira.search.entity.JQLClause;
import com.jira.search.entity.JQLQuery;
import com.jira.search.entity.OrderByClause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JQL (Jira Query Language) Parser - Matches Jira DC functionality
 *
 * Parses JQL strings into structured query components that can be converted
 * to SQL/JPQL queries for the database.
 */
@Service
@Slf4j
public class JQLParser {

    // Standard JQL operators
    private static final Set<String> BINARY_OPERATORS = Set.of(
            "=", "!=", ">", "<", ">=", "<=", "~", "!~", "IN", "NOT IN", "IS", "IS NOT", "WAS", "WAS NOT",
            "WAS IN", "WAS NOT IN", "CHANGED"
    );

    // Fields that support text searches
    private static final Set<String> TEXT_FIELDS = Set.of(
            "summary", "description", "comment", "environment"
    );

    // Fields that support number comparisons
    private static final Set<String> NUMBER_FIELDS = Set.of(
            "priority", "votes", "watchers", "attachments", "comment", "subtasks", "flagged"
    );

    // Date fields
    private static final Set<String> DATE_FIELDS = Set.of(
            "created", "updated", "due", "resolved", "resolutiondate",
            "lastviewed", "createddate", "updateddate"
    );

    // User fields
    private static final Set<String> USER_FIELDS = Set.of(
            "assignee", "reporter", "creator", "updatedBy"
    );

    // List of issue fields
    private static final Set<String> ISSUE_FIELDS = Set.of(
            "issue", "issuekey", "key", "summary", "description", "environment",
            "type", "issuetype", "status", "priority", "resolution", "assignee",
            "reporter", "creator", "created", "updated", "duedate",
            "resolved", "resolutiondate", "votes", "watcher", "watchers", "comment",
            "attachment", "subtask", "parent", "project", "sprint", "epic", "epiclink",
            "epic name", "labels", "component", "fixversion", "affectsversion",
            "linkedissue", "subtasks", "text",
            "cf"
    );

    private static final Pattern OPERATOR_PATTERN = Pattern.compile(
            "\\s*([\\w\\.]+)\\s*(=|!=|>|<|>=|<=|~>|!~>|<|>=|<=| IN | NOT IN | IS | IS NOT | WAS | WAS NOT | WAS IN | WAS NOT IN )\\s*",
            Pattern.CASE_INSENSITIVE
    );

    public JQLQuery parse(String jql) {
        log.debug("Parsing JQL: {}", jql);

        if (jql == null || jql.trim().isEmpty()) {
            return JQLQuery.builder().build();
        }

        JQLQuery query = JQLQuery.builder()
                .originalJql(jql)
                .clauses(new ArrayList<>())
                .orderByClauses(new ArrayList<>())
                .build();

        String processedJql = preprocessJql(jql);

        // Parse ORDER BY first
        processedJql = parseOrderBy(processedJql, query);

        // Split by AND/OR while respecting parentheses
        List<String> clauseStrings = splitByLogicalOperators(processedJql);

        for (String clauseStr : clauseStrings) {
            clauseStr = clauseStr.trim();
            if (clauseStr.isEmpty()) continue;

            JQLClause clause = parseClause(clauseStr);
            if (clause != null) {
                query.getClauses().add(clause);
            }
        }

        return query;
    }

    private String preprocessJql(String jql) {
        // Handle special functions
        jql = jql.replace("updatedBy=", "updatedBy =");
        jql = jql.replace("linkedissue=", "linkedissue =");
        jql = jql.replace("parent=", "parent =");
        // Add more preprocessing as needed
        return jql;
    }

    private String parseOrderBy(String jql, JQLQuery query) {
        int orderByIndex = jql.toUpperCase().lastIndexOf("ORDER BY");
        if (orderByIndex == -1) {
            return jql;
        }

        String orderByClause = jql.substring(orderByIndex + 9).trim();
        jql = jql.substring(0, orderByIndex).trim();

        String[] orderParts = orderByClause.split(",");
        for (String part : orderParts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            String[] fieldAndDirection = part.split("\\s+");
            String field = fieldAndDirection[0].trim();
            String direction = "ASC";

            if (fieldAndDirection.length > 1) {
                String dir = fieldAndDirection[1].trim().toUpperCase();
                if (dir.equals("DESC")) {
                    direction = "DESC";
                }
            }

            // Normalize field names
            field = normalizeFieldName(field);

            query.getOrderByClauses().add(OrderByClause.builder()
                    .field(field)
                    .direction(direction)
                    .build());
        }

        return jql;
    }

    private List<String> splitByLogicalOperators(String jql) {
        List<String> clauses = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;

        for (int i = 0; i < jql.length(); i++) {
            char c = jql.charAt(i);

            if (c == '(') {
                parenDepth++;
                current.append(c);
            } else if (c == ')') {
                parenDepth--;
                current.append(c);
            } else if (parenDepth == 0) {
                String remaining = jql.substring(i);
                if (remaining.toUpperCase().startsWith("AND ")) {
                    clauses.add(current.toString());
                    current = new StringBuilder();
                    i += 3; // Skip "AND "
                } else if (remaining.toUpperCase().startsWith("OR ")) {
                    clauses.add(current.toString());
                    current = new StringBuilder();
                    i += 2; // Skip "OR "
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            clauses.add(current.toString());
        }

        return clauses;
    }

    private JQLClause parseClause(String clauseStr) {
        clauseStr = clauseStr.trim();

        // Handle parentheses
        if (clauseStr.startsWith("(") && clauseStr.endsWith(")")) {
            clauseStr = clauseStr.substring(1, clauseStr.length() - 1);
            return parseClause(clauseStr);
        }

        Matcher matcher = OPERATOR_PATTERN.matcher(clauseStr);
        if (!matcher.find()) {
            // Try fuzzy matching
            return parseSimpleClause(clauseStr);
        }

        String field = matcher.group(1).toLowerCase();
        String operator = matcher.group(2).trim();
        String value = clauseStr.substring(matcher.end()).trim();

        field = normalizeFieldName(field);

        // Remove quotes from value if present
        value = unquoteValue(value);

        return JQLClause.builder()
                .field(field)
                .operator(normalizeOperator(operator))
                .value(value)
                .rawValue(value)
                .build();
    }

    private JQLClause parseSimpleClause(String clauseStr) {
        // Handle format: field=value or field=value without explicit operator
        Pattern simplePattern = Pattern.compile("^([\\w\\.]+)=(.+)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = simplePattern.matcher(clauseStr);

        if (matcher.matches()) {
            String field = normalizeFieldName(matcher.group(1));
            String value = unquoteValue(matcher.group(2));
            return JQLClause.builder()
                    .field(field)
                    .operator("=")
                    .value(value)
                    .build();
        }

        return null;
    }

    private String normalizeFieldName(String field) {
        field = field.trim();

        // Handle cf[fieldId] or customfield_xxx syntax
        if (field.toLowerCase().startsWith("cf[") && field.endsWith("]")) {
            return "cf:" + field.substring(3, field.length() - 1);
        }
        if (field.toLowerCase().startsWith("customfield_")) {
            return "cf:" + field.substring("customfield_".length());
        }

        field = field.toLowerCase();

        return switch (field) {
            case "issuekey", "issue", "key" -> "issue_key";
            case "issuetype", "type" -> "issue_type";
            case "created", "createddate" -> "created_at";
            case "updated", "updateddate" -> "updated_at";
            case "duedate", "due" -> "due_date";
            case "resolved", "resolutiondate" -> "resolution_date";
            case "watchers" -> "watcher_count";
            case "text" -> "summary";
            default -> field;
        };
    }

    private String normalizeOperator(String operator) {
        operator = operator.toUpperCase().trim();
        return switch (operator) {
            case "=" -> "=";
            case "!=" -> "!=";
            case ">" -> ">";
            case "<" -> "<";
            case ">=" -> ">=";
            case "<=" -> "<=";
            case "~" -> "~";
            case "!~" -> "!~";
            case "IN" -> "IN";
            case "NOT IN" -> "NOT IN";
            case "IS" -> "IS";
            case "IS NOT" -> "IS NOT";
            default -> operator;
        };
    }

    private String unquoteValue(String value) {
        if (value == null) return null;

        value = value.trim();

        // Remove surrounding quotes
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        return value.trim();
    }

    public String toSql(JQLQuery query) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM jira_issue.issues WHERE 1=1");

        for (JQLClause clause : query.getClauses()) {
            sql.append(" AND ");
            sql.append(clauseToSql(clause));
        }

        if (!query.getOrderByClauses().isEmpty()) {
            sql.append(" ORDER BY ");
            List<String> orderParts = new ArrayList<>();
            for (OrderByClause ob : query.getOrderByClauses()) {
                orderParts.add(ob.getField() + " " + ob.getDirection());
            }
            sql.append(String.join(", ", orderParts));
        }

        return sql.toString();
    }

    public String toSqlWhereClause(JQLQuery query) {
        StringBuilder sql = new StringBuilder();

        for (int i = 0; i < query.getClauses().size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(clauseToSql(query.getClauses().get(i)));
        }

        return sql.toString();
    }

    private String clauseToSql(JQLClause clause) {
        String field = clause.getField();
        String operator = clause.getOperator();
        String value = clause.getValue() != null ? clause.getValue().toString() : null;

        // Handle special functions
        if (value != null && value.toLowerCase().startsWith("issuekeyin(")) {
            return handleIssueKeyIn(field, value);
        }

        if (value != null && value.toLowerCase().startsWith("issuefunction(")) {
            return handleIssueFunction(field, value);
        }

        if (field.startsWith("cf:")) {
            return handleCustomField(field, operator, value);
        }

        return switch (field) {
            case "issue_key" -> handleIssueKey(field, operator, value);
            case "project" -> handleProject(field, operator, value);
            case "status" -> handleStatus(field, operator, value);
            case "issuetype" -> handleIssueType(field, operator, value);
            case "priority" -> handlePriority(field, operator, value);
            case "assignee", "reporter", "creator" -> handleUser(field, operator, value);
            case "created_at", "updated_at", "due_date", "resolution_date" -> handleDate(field, operator, value);
            case "summary", "description" -> handleText(field, operator, value);
            case "labels" -> handleLabels(field, operator, value);
            case "watcher_count", "votes" -> handleNumber(field, operator, value);
            case "sprint" -> handleSprint(field, operator, value);
            case "epic" -> handleEpic(field, operator, value);
            default -> handleGeneric(field, operator, value);
        };
    }

    private String escapeSqlValue(String value) {
        if (value == null) return "";
        return value.replace("'", "''").replace("\\", "\\\\");
    }

    private String handleIssueKey(String field, String operator, String value) {
        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> keys = parseInList(value);
            String keyList = String.join(",", keys.stream().map(k -> "'" + escapeSqlValue(k) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return field + " " + not + "IN (" + keyList + ")";
        }
        return field + " " + operator + " '" + escapeSqlValue(value) + "'";
    }

    private String handleIssueKeyIn(String field, String value) {
        // Handle ISSUEKEYIN function
        return handleGeneric(field, "IN", value);
    }

    private String handleIssueFunction(String field, String value) {
        // Handle issue functions like issuefunction in releasedVersions()
        return "1=1"; // Placeholder - implement based on specific function
    }

    private String handleProject(String field, String operator, String value) {
        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> projects = parseInList(value);
            String projectList = String.join(",", projects.stream().map(p -> "'" + escapeSqlValue(p) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return "project_id " + not + "IN (SELECT id FROM jira_project.projects WHERE project_key IN (" + projectList + "))";
        }
        return "project_id IN (SELECT id FROM jira_project.projects WHERE project_key = '" + escapeSqlValue(value) + "')";
    }

    private String handleStatus(String field, String operator, String value) {
        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> statuses = parseInList(value);
            String statusList = String.join(",", statuses.stream().map(s -> "'" + escapeSqlValue(s) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return "status_id " + not + "IN (SELECT id FROM jira_issue.issue_status WHERE name IN (" + statusList + "))";
        }
        return "status_id IN (SELECT id FROM jira_issue.issue_status WHERE name = '" + escapeSqlValue(value) + "')";
    }

    private String handleIssueType(String field, String operator, String value) {
        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> types = parseInList(value);
            String typeList = String.join(",", types.stream().map(t -> "'" + escapeSqlValue(t) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return "issue_type_id " + not + "IN (SELECT id FROM jira_issue.issue_type WHERE name IN (" + typeList + "))";
        }
        return "issue_type_id IN (SELECT id FROM jira_issue.issue_type WHERE name = '" + escapeSqlValue(value) + "')";
    }

    private String handlePriority(String field, String operator, String value) {
        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> priorities = parseInList(value);
            String priorityList = String.join(",", priorities.stream().map(p -> "'" + escapeSqlValue(p) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return "priority_id " + not + "IN (SELECT id FROM jira_issue.issue_priority WHERE name IN (" + priorityList + "))";
        }
        return "priority_id IN (SELECT id FROM jira_issue.issue_priority WHERE name = '" + escapeSqlValue(value) + "')";
    }

    private String handleUser(String field, String operator, String value) {
        String column = switch (field) {
            case "assignee" -> "assignee_id";
            case "reporter" -> "reporter_id";
            default -> field + "_id";
        };

        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> users = parseInList(value);
            String userList = String.join(",", users.stream().map(u -> "'" + escapeSqlValue(u) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return column + " " + not + "IN (SELECT id FROM jira_user.users WHERE username IN (" + userList + "))";
        }

        if (operator.equalsIgnoreCase("IS")) {
            if (value.equalsIgnoreCase("NULL")) {
                return column + " IS NULL";
            }
        }

        if (operator.equalsIgnoreCase("IS NOT")) {
            if (value.equalsIgnoreCase("NULL")) {
                return column + " IS NOT NULL";
            }
        }

        return column + " IN (SELECT id FROM jira_user.users WHERE username = '" + escapeSqlValue(value) + "')";
    }

    private String handleDate(String field, String operator, String value) {
        String dateValue = parseDateValue(value);
        return field + " " + operator + " " + dateValue;
    }

    private String parseDateValue(String value) {
        // Handle relative dates
        value = value.toLowerCase().trim();

        if (value.startsWith("-")) {
            // Relative date like -1d, -2w, -3m, -1y
            return parseRelativeDate(value);
        }

        if (value.startsWith("+")) {
            return parseRelativeDate(value);
        }

        if (value.equals("now")) {
            return "CURRENT_TIMESTAMP";
        }

        // Handle "startOfDay", "endOfDay" etc.
        if (value.startsWith("startof")) {
            return parseStartOf(value);
        }

        if (value.startsWith("endof")) {
            return parseEndOf(value);
        }

        // Try to parse as date literal
        try {
            LocalDate date = LocalDate.parse(value.replace("\"", ""));
            return "'" + date.toString() + "'";
        } catch (Exception e) {
            return "'" + escapeSqlValue(value) + "'";
        }
    }

    private String parseRelativeDate(String value) {
        Pattern pattern = Pattern.compile("(-?\\d+)([dwmqy])");
        Matcher matcher = pattern.matcher(value);

        if (matcher.matches()) {
            int amount = Integer.parseInt(matcher.group(1));
            char unit = matcher.group(2).charAt(0);

            LocalDateTime result = LocalDateTime.now();
            result = switch (unit) {
                case 'd' -> result.minus(amount, ChronoUnit.DAYS);
                case 'w' -> result.minus(amount * 7L, ChronoUnit.DAYS);
                case 'm' -> result.minus(amount, ChronoUnit.MONTHS);
                case 'q' -> result.minus(amount * 3L, ChronoUnit.MONTHS);
                case 'y' -> result.minus(amount, ChronoUnit.YEARS);
                default -> result;
            };

            return "TIMESTAMP '" + result.toString() + "'";
        }

        return "CURRENT_TIMESTAMP";
    }

    private String parseStartOf(String value) {
        return "'" + LocalDate.now().toString() + "'"; // Simplified
    }

    private String parseEndOf(String value) {
        return "'" + LocalDate.now().toString() + "'"; // Simplified
    }

    private String handleText(String field, String operator, String value) {
        String escaped = escapeSqlValue(value);
        if (operator.equals("~")) {
            return field + " ILIKE '%" + escaped + "%'";
        }
        if (operator.equals("!~")) {
            return field + " NOT ILIKE '%" + escaped + "%'";
        }
        if (operator.equals("=")) {
            return field + " = '" + escaped + "'";
        }
        return field + " ILIKE '%" + escaped + "%'";
    }

    private String handleLabels(String field, String operator, String value) {
        // Simplified label handling
        return "id IN (SELECT issue_id FROM jira_issue.label WHERE name = '" + escapeSqlValue(value) + "')";
    }

    private String handleNumber(String field, String operator, String value) {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + value);
        }
        return field + " " + operator + " " + value;
    }

    private String handleSprint(String field, String operator, String value) {
        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> sprints = parseInList(value);
            String sprintList = String.join(",", sprints.stream().map(s -> "'" + escapeSqlValue(s) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return "sprint_id " + not + "IN (SELECT id FROM jira_sprint.sprints WHERE name IN (" + sprintList + "))";
        }
        return "sprint_id IN (SELECT id FROM jira_sprint.sprints WHERE name = '" + escapeSqlValue(value) + "')";
    }

    private String handleEpic(String field, String operator, String value) {
        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> epics = parseInList(value);
            String epicList = String.join(",", epics.stream().map(e -> "'" + escapeSqlValue(e) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return "epic_id " + not + "IN (SELECT id FROM jira_issue.issues WHERE issue_key IN (" + epicList + "))";
        }
        return "epic_id IN (SELECT id FROM jira_issue.issues WHERE issue_key = '" + escapeSqlValue(value) + "')";
    }

    private String handleCustomField(String field, String operator, String value) {
        String fieldId = escapeSqlValue(field.substring(3));
        String escaped = escapeSqlValue(value);

        if (operator.equalsIgnoreCase("IS")) {
            if ("EMPTY".equalsIgnoreCase(value) || "NULL".equalsIgnoreCase(value)) {
                return "i.id NOT IN (SELECT cfv.issue_id FROM jira_issue.custom_field_values cfv WHERE cfv.field_id = '" + fieldId + "')";
            }
            if ("NOT EMPTY".equalsIgnoreCase(value)) {
                return "i.id IN (SELECT cfv.issue_id FROM jira_issue.custom_field_values cfv WHERE cfv.field_id = '" + fieldId + "')";
            }
        }

        if (operator.equalsIgnoreCase("IS NOT")) {
            if ("EMPTY".equalsIgnoreCase(value) || "NULL".equalsIgnoreCase(value)) {
                return "i.id IN (SELECT cfv.issue_id FROM jira_issue.custom_field_values cfv WHERE cfv.field_id = '" + fieldId + "')";
            }
        }

        if ("~".equals(operator) || "CONTAINS".equalsIgnoreCase(operator)) {
            return "i.id IN (SELECT cfv.issue_id FROM jira_issue.custom_field_values cfv WHERE cfv.field_id = '" + fieldId
                    + "' AND CAST(cfv.value AS TEXT) ILIKE '%" + escaped + "%')";
        }

        if ("!~".equals(operator)) {
            return "i.id NOT IN (SELECT cfv.issue_id FROM jira_issue.custom_field_values cfv WHERE cfv.field_id = '" + fieldId
                    + "' AND CAST(cfv.value AS TEXT) ILIKE '%" + escaped + "%')";
        }

        if ("IN".equalsIgnoreCase(operator) || "NOT IN".equalsIgnoreCase(operator)) {
            List<String> values = parseInList(value);
            String valueList = String.join(",", values.stream().map(v -> "'" + escapeSqlValue(v) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return "i.id " + not + "IN (SELECT cfv.issue_id FROM jira_issue.custom_field_values cfv WHERE cfv.field_id = '" + fieldId
                    + "' AND cfv.value @> ANY(ARRAY[" + valueList + "]::jsonb[]))";
        }

        String sqlOp = switch (operator) {
            case "=" -> "=";
            case "!=" -> "!=";
            default -> "=";
        };

        return "i.id IN (SELECT cfv.issue_id FROM jira_issue.custom_field_values cfv WHERE cfv.field_id = '" + fieldId
                + "' AND CAST(cfv.value AS TEXT) " + sqlOp + " '\"" + escaped + "\"')";
    }

    private String handleGeneric(String field, String operator, String value) {
        if (operator.equalsIgnoreCase("IN") || operator.equalsIgnoreCase("NOT IN")) {
            List<String> values = parseInList(value);
            String valueList = String.join(",", values.stream().map(v -> "'" + escapeSqlValue(v) + "'").toList());
            String not = operator.equalsIgnoreCase("NOT IN") ? "NOT " : "";
            return field + " " + not + "IN (" + valueList + ")";
        }

        if (operator.equalsIgnoreCase("IS")) {
            if (value.equalsIgnoreCase("NULL")) {
                return field + " IS NULL";
            }
            if (value.equalsIgnoreCase("EMPTY") || value.equalsIgnoreCase("NULL")) {
                return "(" + field + " IS NULL OR " + field + " = '')";
            }
        }

        if (operator.equalsIgnoreCase("IS NOT")) {
            if (value.equalsIgnoreCase("NULL")) {
                return field + " IS NOT NULL";
            }
            if (value.equalsIgnoreCase("EMPTY")) {
                return "(" + field + " IS NOT NULL AND " + field + " != '')";
            }
        }

        return field + " " + operator + " '" + escapeSqlValue(value) + "'";
    }

    private List<String> parseInList(String value) {
        List<String> items = new ArrayList<>();

        // Remove parentheses
        value = value.trim();
        if (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1);
        }

        // Split by comma, respecting quoted strings
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = '"';

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if ((c == '"' || c == '\'') && !inQuote) {
                inQuote = true;
                quoteChar = c;
                current.append(c);
            } else if (c == quoteChar && inQuote) {
                inQuote = false;
                current.append(c);
            } else if (c == ',' && !inQuote) {
                String item = current.toString().trim();
                if (!item.isEmpty()) {
                    items.add(unquoteValue(item));
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String lastItem = current.toString().trim();
        if (!lastItem.isEmpty()) {
            items.add(unquoteValue(lastItem));
        }

        return items;
    }
}