package com.avionics_systems.plan.specification;

import com.avionics_systems.plan.entity.SprintIssue;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SprintIssueSpecification {

    public static Specification<SprintIssue> buildFromJql(UUID sprintId, String jql) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("sprint").get("id"), sprintId));
            predicates.add(cb.notEqual(root.get("completionStatus"), "DROPPED"));

            if (jql != null && !jql.isBlank()) {
                String[] clauses = jql.split("\\s+AND\\s+", -1);
                for (String clause : clauses) {
                    clause = clause.trim();
                    String[] parts = clause.split("\\s*=\\s*", 2);
                    if (parts.length == 2) {
                        String field = parts[0].trim().toLowerCase();
                        String value = parts[1].trim().replaceAll("^[\"']|[\"']$", "");

                        switch (field) {
                            case "completionstatus":
                                predicates.add(cb.equal(root.get("completionStatus"), value));
                                break;
                            case "issuetype":
                                predicates.add(cb.equal(root.get("planItem").get("issueType"), value));
                                break;
                            case "assigneeid":
                                predicates.add(cb.equal(root.get("planItem").get("assigneeId"), UUID.fromString(value)));
                                break;
                            case "statuscategory":
                                predicates.add(cb.equal(root.get("planItem").get("statusCategory"), value));
                                break;
                            case "flagged":
                                predicates.add(cb.equal(root.get("flagged"), Boolean.parseBoolean(value)));
                                break;
                        }
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
