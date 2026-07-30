package com.avionics_systems.issue.service;

import com.avionics_systems.issue.entity.Issue;
import com.avionics_systems.issue.repository.ProjectRepository;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Avionics Systems DC-style JQL → JPA {@link Specification} with AND, OR, parentheses, and common functions.
 */
@Component
@RequiredArgsConstructor
public class JqlExpressionParser {

    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
            "^\\s*([\\w.]+)\\s*(=|!=|>|<|>=|<=|~|!~|IN|NOT\\s+IN|IS|IS\\s+NOT)\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ProjectRepository projectRepository;
    private final JqlGroupResolver jqlGroupResolver;

    private static final Pattern DURING_PATTERN = Pattern.compile(
            "^\\s*([\\w.]+)\\s+DURING\\s*\\(\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MEMBERS_OF_PATTERN = Pattern.compile(
            "membersOf\\s*\\(\\s*\"([^\"]*)\"\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    public record JqlParseResult(Specification<Issue> spec, Sort sort) {}

    public JqlParseResult parse(String jql, UUID currentUserId) {
        if (jql == null || jql.isBlank()) {
            return new JqlParseResult((r, q, cb) -> cb.conjunction(), defaultSort());
        }

        String body = jql.trim();
        Sort sort = defaultSort();

        Matcher orderMatcher = Pattern.compile("\\s+ORDER\\s+BY\\s+(.+)$", Pattern.CASE_INSENSITIVE).matcher(body);
        if (orderMatcher.find()) {
            sort = parseSort(orderMatcher.group(1).trim());
            body = body.substring(0, orderMatcher.start()).trim();
        }

        if (body.isBlank() || "1=1".equalsIgnoreCase(body)) {
            return new JqlParseResult((r, q, cb) -> cb.conjunction(), sort);
        }

        Specification<Issue> spec = parseOrGroup(body, currentUserId);
        if (spec == null) {
            spec = (r, q, cb) -> cb.conjunction();
        }
        return new JqlParseResult(spec, sort);
    }

    private Specification<Issue> parseOrGroup(String expr, UUID currentUserId) {
        List<String> parts = splitTopLevel(expr, "OR");
        if (parts.size() == 1) {
            return parseAndGroup(parts.get(0), currentUserId);
        }
        Specification<Issue> combined = null;
        for (String part : parts) {
            Specification<Issue> s = parseAndGroup(part.trim(), currentUserId);
            if (s == null) continue;
            combined = combined == null ? s : combined.or(s);
        }
        return combined;
    }

    private Specification<Issue> parseAndGroup(String expr, UUID currentUserId) {
        expr = unwrapParens(expr.trim());
        List<String> parts = splitTopLevel(expr, "AND");
        if (parts.size() == 1) {
            return parseAtom(parts.get(0), currentUserId);
        }
        Specification<Issue> combined = null;
        for (String part : parts) {
            Specification<Issue> s = parseAtom(part.trim(), currentUserId);
            if (s == null) continue;
            combined = combined == null ? s : combined.and(s);
        }
        return combined;
    }

    private Specification<Issue> parseAtom(String expr, UUID currentUserId) {
        expr = unwrapParens(expr.trim());
        if (expr.isBlank()) return null;

        if (expr.startsWith("(")) {
            return parseOrGroup(expr, currentUserId);
        }

        List<String> orParts = splitTopLevel(expr, "OR");
        if (orParts.size() > 1) {
            return parseOrGroup(expr, currentUserId);
        }

        Specification<Issue> history = tryParseHistoryClause(expr, currentUserId);
        if (history != null) {
            return history;
        }

        Specification<Issue> during = tryParseDuringClause(expr);
        if (during != null) {
            return during;
        }

        Matcher m = CLAUSE_PATTERN.matcher(expr);
        if (!m.matches()) {
            return null;
        }

        String field = normalizeField(m.group(1).trim());
        String op = m.group(2).trim().toUpperCase().replaceAll("\\s+", " ");
        String valueRaw = m.group(3).trim();

        return buildClause(field, op, valueRaw, currentUserId);
    }

    private Specification<Issue> buildClause(String field, String op, String valueRaw, UUID currentUserId) {
        return switch (field) {
            case "project" -> projectSpec(op, valueRaw);
            case "status" -> statusSpec(op, valueRaw);
            case "type", "issuetype" -> issueTypeSpec(op, valueRaw);
            case "assignee" -> userSpec("assigneeId", op, valueRaw, currentUserId);
            case "reporter" -> userSpec("reporterId", op, valueRaw, currentUserId);
            case "creator" -> userSpec("reporterId", op, valueRaw, currentUserId);
            case "priority" -> prioritySpec(op, valueRaw);
            case "text", "summary" -> textSpec(op, valueRaw);
            case "description" -> descriptionSpec(op, valueRaw);
            case "key", "issuekey", "issue" -> issueKeySpec(op, valueRaw);
            case "resolution" -> resolutionSpec(op, valueRaw);
            case "created", "createddate" -> dateSpec("createdAt", op, valueRaw);
            case "updated", "updateddate" -> dateSpec("updatedAt", op, valueRaw);
            case "duedate", "due" -> localDateSpec("dueDate", op, valueRaw);
            case "labels", "label" -> labelsSpec(op, valueRaw);
            default -> null;
        };
    }

    private Specification<Issue> projectSpec(String op, String valueRaw) {
        if ("IN".equals(op) || "NOT IN".equals(op)) {
            List<String> keys = parseInList(valueRaw);
            List<UUID> ids = keys.stream()
                    .map(k -> projectRepository.findByProjectKey(k.toUpperCase()).map(p -> p.getId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            if (ids.isEmpty()) {
                return (r, q, cb) -> cb.disjunction();
            }
            boolean not = "NOT IN".equals(op);
            return (r, q, cb) -> not ? cb.not(r.get("projectId").in(ids)) : r.get("projectId").in(ids);
        }
        String key = unquote(valueRaw);
        Optional<UUID> projectId = projectRepository.findByProjectKey(key.toUpperCase()).map(p -> p.getId());
        if (projectId.isEmpty()) {
            return (r, q, cb) -> cb.disjunction();
        }
        UUID id = projectId.get();
        if ("!=".equals(op)) {
            return (r, q, cb) -> cb.notEqual(r.get("projectId"), id);
        }
        return (r, q, cb) -> cb.equal(r.get("projectId"), id);
    }

    private Specification<Issue> statusSpec(String op, String valueRaw) {
        if ("IS".equals(op) && isEmpty(valueRaw)) {
            return (r, q, cb) -> cb.isNull(r.get("status"));
        }
        if ("NOT IN".equals(op)) {
            List<String> names = parseInList(valueRaw);
            return (r, q, cb) -> {
                Join<?, ?> status = r.join("status", JoinType.INNER);
                return cb.not(status.get("name").in(names));
            };
        }
        if ("IN".equals(op)) {
            List<String> names = parseInList(valueRaw);
            return (r, q, cb) -> {
                Join<?, ?> status = r.join("status", JoinType.INNER);
                return status.get("name").in(names);
            };
        }
        String name = unquote(valueRaw);
        return (r, q, cb) -> {
            Join<?, ?> status = r.join("status", JoinType.INNER);
            Predicate eq = cb.equal(cb.lower(status.get("name")), name.toLowerCase());
            return "!=".equals(op) ? cb.not(eq) : eq;
        };
    }

    private Specification<Issue> issueTypeSpec(String op, String valueRaw) {
        if ("IN".equals(op) || "NOT IN".equals(op)) {
            List<String> names = parseInList(valueRaw);
            boolean not = "NOT IN".equals(op);
            return (r, q, cb) -> {
                Join<?, ?> type = r.join("issueType", JoinType.INNER);
                return not ? cb.not(type.get("name").in(names)) : type.get("name").in(names);
            };
        }
        String name = unquote(valueRaw);
        return (r, q, cb) -> {
            Join<?, ?> type = r.join("issueType", JoinType.INNER);
            Predicate eq = cb.equal(cb.lower(type.get("name")), name.toLowerCase());
            return "!=".equals(op) ? cb.not(eq) : eq;
        };
    }

    private Specification<Issue> prioritySpec(String op, String valueRaw) {
        if ("IN".equals(op) || "NOT IN".equals(op)) {
            List<String> names = parseInList(valueRaw);
            boolean not = "NOT IN".equals(op);
            return (r, q, cb) -> {
                Join<?, ?> p = r.join("priority", JoinType.LEFT);
                return not ? cb.not(p.get("name").in(names)) : p.get("name").in(names);
            };
        }
        String name = unquote(valueRaw);
        return (r, q, cb) -> {
            Join<?, ?> p = r.join("priority", JoinType.LEFT);
            Predicate eq = cb.equal(cb.lower(p.get("name")), name.toLowerCase());
            return "!=".equals(op) ? cb.not(eq) : eq;
        };
    }

    private Specification<Issue> userSpec(String field, String op, String valueRaw, UUID currentUserId) {
        if ("IS".equals(op) && isEmpty(valueRaw)) {
            return (r, q, cb) -> cb.isNull(r.get(field));
        }
        if ("IS NOT".equals(op) && isEmpty(valueRaw)) {
            return (r, q, cb) -> cb.isNotNull(r.get(field));
        }
        if ("IN".equals(op) || "NOT IN".equals(op)) {
            Matcher membersOf = MEMBERS_OF_PATTERN.matcher(valueRaw);
            if (membersOf.find()) {
                List<UUID> userIds = jqlGroupResolver.resolveMembersOf(membersOf.group(1));
                if (userIds.isEmpty()) {
                    return (r, q, cb) -> cb.disjunction();
                }
                boolean not = "NOT IN".equals(op);
                return (r, q, cb) -> not
                        ? cb.or(cb.isNull(r.get(field)), cb.not(r.get(field).in(userIds)))
                        : r.get(field).in(userIds);
            }
        }
        String lower = valueRaw.toLowerCase();
        if (lower.contains("currentuser()")) {
            if (currentUserId == null) {
                return (r, q, cb) -> cb.disjunction();
            }
            if ("!=".equals(op)) {
                return (r, q, cb) -> cb.or(
                        cb.isNull(r.get(field)),
                        cb.notEqual(r.get(field), currentUserId));
            }
            return (r, q, cb) -> cb.equal(r.get(field), currentUserId);
        }
        try {
            UUID uid = UUID.fromString(unquote(valueRaw));
            if ("!=".equals(op)) {
                return (r, q, cb) -> cb.notEqual(r.get(field), uid);
            }
            return (r, q, cb) -> cb.equal(r.get(field), uid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Specification<Issue> textSpec(String op, String valueRaw) {
        String text = unquote(valueRaw).toLowerCase();
        String pattern = "%" + text + "%";
        return (r, q, cb) -> {
            Predicate like = cb.like(cb.lower(r.get("title")), pattern);
            if ("!~".equals(op)) return cb.not(like);
            if ("~".equals(op) || "=".equals(op)) return like;
            return like;
        };
    }

    private Specification<Issue> descriptionSpec(String op, String valueRaw) {
        String text = unquote(valueRaw).toLowerCase();
        String pattern = "%" + text + "%";
        return (r, q, cb) -> {
            Predicate like = cb.like(cb.lower(r.get("description")), pattern);
            return "!~".equals(op) ? cb.not(like) : like;
        };
    }

    private Specification<Issue> issueKeySpec(String op, String valueRaw) {
        if ("IN".equals(op) || "NOT IN".equals(op)) {
            List<String> keys = parseInList(valueRaw);
            boolean not = "NOT IN".equals(op);
            return (r, q, cb) -> not
                    ? cb.not(r.get("issueKey").in(keys))
                    : r.get("issueKey").in(keys);
        }
        String key = unquote(valueRaw).toUpperCase();
        if ("~".equals(op) || "!~".equals(op)) {
            String pattern = "%" + key.toLowerCase() + "%";
            return (r, q, cb) -> {
                Predicate p = cb.like(cb.lower(r.get("issueKey")), pattern);
                return "!~".equals(op) ? cb.not(p) : p;
            };
        }
        return (r, q, cb) -> {
            Predicate eq = cb.equal(cb.upper(r.get("issueKey")), key);
            return "!=".equals(op) ? cb.not(eq) : eq;
        };
    }

    private Specification<Issue> resolutionSpec(String op, String valueRaw) {
        String val = unquote(valueRaw);
        if ("IS".equals(op) && isEmpty(valueRaw)) {
            return (r, q, cb) -> cb.isNull(r.get("resolutionId"));
        }
        if ("IS NOT".equals(op) && isEmpty(valueRaw)) {
            return (r, q, cb) -> cb.isNotNull(r.get("resolutionId"));
        }
        if ("UNRESOLVED".equalsIgnoreCase(val) || "EMPTY".equalsIgnoreCase(val)) {
            return (r, q, cb) -> cb.isNull(r.get("resolutionId"));
        }
        return null;
    }

    private Specification<Issue> dateSpec(String field, String op, String valueRaw) {
        LocalDateTime dt = parseDateTime(valueRaw);
        if (dt == null) return null;
        return (r, q, cb) -> switch (op) {
            case ">" -> cb.greaterThan(r.get(field), dt);
            case ">=" -> cb.greaterThanOrEqualTo(r.get(field), dt);
            case "<" -> cb.lessThan(r.get(field), dt);
            case "<=" -> cb.lessThanOrEqualTo(r.get(field), dt);
            case "!=" -> cb.notEqual(r.get(field), dt);
            default -> cb.equal(r.get(field), dt);
        };
    }

    private Specification<Issue> localDateSpec(String field, String op, String valueRaw) {
        LocalDate d = parseDate(valueRaw);
        if (d == null) return null;
        return (r, q, cb) -> switch (op) {
            case ">" -> cb.greaterThan(r.get(field), d);
            case ">=" -> cb.greaterThanOrEqualTo(r.get(field), d);
            case "<" -> cb.lessThan(r.get(field), d);
            case "<=" -> cb.lessThanOrEqualTo(r.get(field), d);
            case "!=" -> cb.notEqual(r.get(field), d);
            default -> cb.equal(r.get(field), d);
        };
    }

    private Specification<Issue> labelsSpec(String op, String valueRaw) {
        String label = unquote(valueRaw);
        if ("IS".equals(op) && isEmpty(valueRaw)) {
            return (r, q, cb) -> cb.isNull(r.get("labels"));
        }
        String pattern = "%" + label + "%";
        return (r, q, cb) -> {
            Expression<String> joined = cb.function(
                    "array_to_string", String.class, r.get("labels"), cb.literal(","));
            Predicate like = cb.like(joined, pattern);
            return "!=".equals(op) ? cb.not(like) : like;
        };
    }

    private List<String> splitTopLevel(String expr, String operator) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        String upper = expr.toUpperCase();
        String token = " " + operator + " ";
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth == 0 && upper.startsWith(token, i)) {
                parts.add(cur.toString());
                cur = new StringBuilder();
                i += token.length() - 1;
                continue;
            }
            cur.append(c);
        }
        if (cur.length() > 0) parts.add(cur.toString());
        return parts.isEmpty() ? List.of(expr) : parts;
    }

    private String unwrapParens(String expr) {
        while (expr.startsWith("(") && expr.endsWith(")")) {
            int depth = 0;
            boolean full = true;
            for (int i = 0; i < expr.length(); i++) {
                if (expr.charAt(i) == '(') depth++;
                else if (expr.charAt(i) == ')') depth--;
                if (depth == 0 && i < expr.length() - 1) {
                    full = false;
                    break;
                }
            }
            if (!full) break;
            expr = expr.substring(1, expr.length() - 1).trim();
        }
        return expr;
    }

    private Sort parseSort(String orderPart) {
        String[] tokens = orderPart.split("\\s+");
        String field = tokens[0].toLowerCase();
        Sort.Direction dir = tokens.length > 1 && tokens[1].equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String entityField = switch (field) {
            case "priority" -> "priority.sequence";
            case "created", "createddate" -> "createdAt";
            case "updated", "updateddate" -> "updatedAt";
            case "duedate", "due" -> "dueDate";
            case "key", "issuekey" -> "issueKey";
            default -> "updatedAt";
        };
        if ("priority.sequence".equals(entityField)) {
            return Sort.by(dir, "priority").and(Sort.by(dir, "updatedAt"));
        }
        return Sort.by(dir, entityField);
    }

    private Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "updatedAt");
    }

    private String normalizeField(String field) {
        return switch (field.toLowerCase()) {
            case "issue_type" -> "issuetype";
            case "issuekey", "issue_key" -> "key";
            default -> field.toLowerCase();
        };
    }

    private boolean isEmpty(String valueRaw) {
        return "EMPTY".equalsIgnoreCase(valueRaw.trim()) || "NULL".equalsIgnoreCase(valueRaw.trim());
    }

    private String unquote(String v) {
        v = v.trim();
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private List<String> parseInList(String raw) {
        raw = raw.trim();
        if (raw.startsWith("(") && raw.endsWith(")")) {
            raw = raw.substring(1, raw.length() - 1);
        }
        return Arrays.stream(raw.split(","))
                .map(this::unquote)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private LocalDateTime parseDateTime(String raw) {
        String v = unquote(raw);
        if (v.startsWith("-")) {
            int days = parseRelativeDays(v);
            if (days > 0) {
                return LocalDateTime.now().minusDays(days);
            }
        }
        try {
            return LocalDateTime.parse(v, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(v).atStartOfDay();
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private LocalDate parseDate(String raw) {
        String v = unquote(raw);
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            LocalDateTime dt = parseDateTime(raw);
            return dt != null ? dt.toLocalDate() : null;
        }
    }

    private int parseRelativeDays(String v) {
        v = v.toLowerCase().trim();
        if (v.endsWith("d")) {
            try {
                return Integer.parseInt(v.substring(0, v.length() - 1).replace("-", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private Specification<Issue> tryParseDuringClause(String expr) {
        Matcher m = DURING_PATTERN.matcher(expr.trim());
        if (!m.matches()) {
            return null;
        }
        String field = normalizeField(m.group(1).trim());
        LocalDateTime start = parseDateTime(m.group(2));
        LocalDateTime end = parseDateTime(m.group(3));
        if (start == null || end == null) {
            return null;
        }
        String entityField = switch (field) {
            case "created", "createddate" -> "createdAt";
            case "updated", "updateddate" -> "updatedAt";
            case "duedate", "due" -> "dueDate";
            default -> null;
        };
        if (entityField == null) {
            return null;
        }
        if ("dueDate".equals(entityField)) {
            LocalDate startDate = start.toLocalDate();
            LocalDate endDate = end.toLocalDate();
            return (r, q, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(r.get(entityField), startDate),
                    cb.lessThanOrEqualTo(r.get(entityField), endDate));
        }
        return (r, q, cb) -> cb.and(
                cb.greaterThanOrEqualTo(r.get(entityField), start),
                cb.lessThanOrEqualTo(r.get(entityField), end));
    }

    /**
     * History operators: {@code status WAS "Done"}, {@code assignee CHANGED}, {@code priority CHANGED AFTER -7d BY currentUser()}.
     */
    private Specification<Issue> tryParseHistoryClause(String expr, UUID currentUserId) {
        Matcher was = Pattern.compile(
                "^\\s*([\\w.]+)\\s+WAS\\s+(NOT\\s+)?(IN\\s*\\([^)]+\\)|.+)$",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(expr);
        if (was.matches()) {
            String field = normalizeField(was.group(1).trim());
            boolean hadNot = was.group(2) != null;
            String rest = was.group(3).trim();
            String restUpper = rest.toUpperCase(Locale.ROOT);
            boolean negate = false;
            boolean notIn = false;
            if (restUpper.startsWith("IN")) {
                notIn = hadNot;
            } else if (hadNot) {
                negate = true;
            }
            List<String> values;
            if (restUpper.startsWith("IN")) {
                values = parseInList(rest.substring(2).trim());
                return JqlChangeHistorySpecs.fieldWas(field, values, negate, notIn);
            }
            values = List.of(unquote(rest));
            return JqlChangeHistorySpecs.fieldWas(field, values, negate, false);
        }

        Matcher changed = Pattern.compile(
                "^\\s*([\\w.]+)\\s+CHANGED\\b(.*)$",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(expr);
        if (changed.matches()) {
            String field = normalizeField(changed.group(1).trim());
            ChangedModifiers mods = parseChangedModifiers(changed.group(2).trim(), currentUserId);
            return JqlChangeHistorySpecs.fieldChanged(
                    field, mods.changedBy, mods.after, mods.before, mods.fromValue, mods.toValue);
        }
        return null;
    }

    private ChangedModifiers parseChangedModifiers(String tail, UUID currentUserId) {
        ChangedModifiers mods = new ChangedModifiers();
        if (tail == null || tail.isBlank()) {
            return mods;
        }
        Matcher m = Pattern.compile(
                "(AFTER|BEFORE|FROM|TO|BY)\\s+(-?\\d+d|currentUser\\(\\)|\"[^\"]*\"|'[^']*'|\\S+)",
                Pattern.CASE_INSENSITIVE).matcher(tail);
        while (m.find()) {
            String kw = m.group(1).toUpperCase(Locale.ROOT);
            String val = unquote(m.group(2).trim());
            switch (kw) {
                case "AFTER" -> mods.after = parseDateTime(val);
                case "BEFORE" -> mods.before = parseDateTime(val);
                case "FROM" -> mods.fromValue = val;
                case "TO" -> mods.toValue = val;
                case "BY" -> {
                    if (val.toLowerCase(Locale.ROOT).contains("currentuser()")) {
                        mods.changedBy = currentUserId;
                    } else {
                        try {
                            mods.changedBy = UUID.fromString(val);
                        } catch (IllegalArgumentException ignored) {
                            mods.changedBy = null;
                        }
                    }
                }
                default -> { }
            }
        }
        return mods;
    }

    private static final class ChangedModifiers {
        UUID changedBy;
        LocalDateTime after;
        LocalDateTime before;
        String fromValue;
        String toValue;
    }
}
