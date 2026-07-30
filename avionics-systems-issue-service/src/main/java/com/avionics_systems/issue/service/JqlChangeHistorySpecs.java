package com.avionics_systems.issue.service;

import com.avionics_systems.issue.entity.ChangeGroup;
import com.avionics_systems.issue.entity.ChangeItem;
import com.avionics_systems.issue.entity.Issue;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * JQL WAS / CHANGED predicates backed by {@code change_groups} + {@code change_items}.
 */
public final class JqlChangeHistorySpecs {

    private JqlChangeHistorySpecs() {}

    public static Specification<Issue> fieldWas(String field, List<String> values, boolean negate, boolean notIn) {
        String historyField = mapHistoryField(field);
        List<String> normalized = values.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList();
        return (root, query, cb) -> {
            Subquery<UUID> sq = query.subquery(UUID.class);
            Root<ChangeGroup> cg = sq.from(ChangeGroup.class);
            Root<ChangeItem> ci = sq.from(ChangeItem.class);

            Predicate link = cb.equal(ci.get("changeGroupId"), cg.get("id"));
            Predicate issue = cb.equal(cg.get("issueId"), root.get("id"));
            Predicate fieldMatch = cb.equal(cb.lower(ci.get("field")), historyField);

            List<Predicate> valueMatches = new ArrayList<>();
            for (String v : normalized) {
                valueMatches.add(valuePredicate(cb, ci, v));
            }
            Predicate valueMatch = cb.or(valueMatches.toArray(new Predicate[0]));

            sq.select(cg.get("id")).where(cb.and(link, issue, fieldMatch, valueMatch));
            Predicate exists = cb.exists(sq);

            if (notIn) {
                return negate ? exists : cb.not(exists);
            }
            return negate ? cb.not(exists) : exists;
        };
    }

    public static Specification<Issue> fieldChanged(
            String field,
            UUID changedByUserId,
            LocalDateTime after,
            LocalDateTime before,
            String fromValue,
            String toValue) {
        String historyField = mapHistoryField(field);
        return (root, query, cb) -> {
            Subquery<UUID> sq = query.subquery(UUID.class);
            Root<ChangeGroup> cg = sq.from(ChangeGroup.class);
            Root<ChangeItem> ci = sq.from(ChangeItem.class);

            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(ci.get("changeGroupId"), cg.get("id")));
            preds.add(cb.equal(cg.get("issueId"), root.get("id")));
            preds.add(cb.equal(cb.lower(ci.get("field")), historyField));

            if (changedByUserId != null) {
                preds.add(cb.equal(cg.get("authorId"), changedByUserId));
            }
            if (after != null) {
                preds.add(cb.greaterThanOrEqualTo(cg.get("createdAt"), after));
            }
            if (before != null) {
                preds.add(cb.lessThanOrEqualTo(cg.get("createdAt"), before));
            }
            if (fromValue != null && !fromValue.isBlank()) {
                String fv = fromValue.toLowerCase(Locale.ROOT);
                preds.add(cb.or(
                        cb.equal(cb.lower(ci.get("oldString")), fv),
                        cb.equal(cb.lower(ci.get("oldValue")), fv)));
            }
            if (toValue != null && !toValue.isBlank()) {
                String tv = toValue.toLowerCase(Locale.ROOT);
                preds.add(cb.or(
                        cb.equal(cb.lower(ci.get("newString")), tv),
                        cb.equal(cb.lower(ci.get("newValue")), tv)));
            }

            sq.select(cg.get("id")).where(cb.and(preds.toArray(new Predicate[0])));
            return cb.exists(sq);
        };
    }

    private static Predicate valuePredicate(CriteriaBuilder cb, Root<ChangeItem> ci, String valueLower) {
        return cb.or(
                cb.equal(cb.lower(ci.get("oldString")), valueLower),
                cb.equal(cb.lower(ci.get("newString")), valueLower),
                cb.equal(cb.lower(ci.get("oldValue")), valueLower),
                cb.equal(cb.lower(ci.get("newValue")), valueLower));
    }

    public static String mapHistoryField(String jqlField) {
        return switch (jqlField.toLowerCase(Locale.ROOT)) {
            case "summary", "title" -> "summary";
            case "issuetype", "type" -> "issuetype";
            case "assignee" -> "assignee";
            case "reporter" -> "reporter";
            case "priority" -> "priority";
            case "status" -> "status";
            case "resolution" -> "resolution";
            case "duedate", "due" -> "duedate";
            default -> jqlField.toLowerCase(Locale.ROOT);
        };
    }
}
