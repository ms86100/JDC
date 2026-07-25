package com.jira.cluster.messaging;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String ISSUE_EVENTS = "jira.issue.events";
    public static final String WORKFLOW_EVENTS = "jira.workflow.events";
    public static final String NOTIFICATION_EVENTS = "jira.notification.events";
    public static final String AUDIT_EVENTS = "jira.audit.events";
    public static final String SEARCH_INDEX_EVENTS = "jira.search.index";
    public static final String USER_EVENTS = "jira.user.events";
    public static final String PROJECT_EVENTS = "jira.project.events";
}
