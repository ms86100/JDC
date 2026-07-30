package com.avionics_systems.issue.service;

import com.avionics_systems.issue.entity.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Facade for JQL parsing — delegates to {@link JqlExpressionParser}.
 */
@Component
@RequiredArgsConstructor
public class JqlSpecificationBuilder {

    private final JqlExpressionParser jqlExpressionParser;

    public record JqlParseResult(Specification<Issue> spec, Sort sort) {}

    public JqlParseResult parse(String jql, UUID currentUserId) {
        JqlExpressionParser.JqlParseResult parsed = jqlExpressionParser.parse(jql, currentUserId);
        return new JqlParseResult(parsed.spec(), parsed.sort());
    }
}
