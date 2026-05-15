package com.jira.search.entity;

import lombok.*;

import java.util.List;

/**
 * JQL Function - Represents a function call in JQL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JQLFunction {
    private String name;
    private List<String> arguments;

    // Standard JQL functions
    public static final String EARLIEST_ISSUE_KEY = "earliestIssueKey";
    public static final String LATEST_ISSUE_KEY = "latestIssueKey";
    public static final String COMPARABLE_ISSUES = "comparableIssues";
    public static final String RELEASED_VERSIONS = "releasedVersions";
    public static final String UNRELEASED_VERSIONS = "unreleasedVersions";
    public static final String STANDARD_ERROR = "standardError";
    public static final String PROJECTS_LEAD_BY_USER = "projectsLeadByUser";
    public static final String PROJECTS_WHERE_USER_HAS_ROLE = "projectsWhereUserHasRole";
    public static final String CURRENT_USER = "currentUser";
    public static final String ISSUE_HISTORY = "issueHistory";
    public static final String PRINTED_TIMETRACKING = "printedTimeTracking";
    public static final String SUM = "sum";
    public static final String AVG = "avg";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String STDEV = "stdev";
    public static final String VARIANCE = "variance";
    public static final String START_OF_DAY = "startOfDay";
    public static final String END_OF_DAY = "endOfDay";
    public static final String START_OF_WEEK = "startOfWeek";
    public static final String END_OF_WEEK = "endOfWeek";
    public static final String START_OF_MONTH = "startOfMonth";
    public static final String END_OF_MONTH = "endOfMonth";
    public static final String START_OF_QUARTER = "startOfQuarter";
    public static final String END_OF_QUARTER = "endOfQuarter";
    public static final String START_OF_YEAR = "startOfYear";
    public static final String END_OF_YEAR = "endOfYear";
}