package com.jira.plugin.conditions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.WorkflowException;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.opensymphony.workflow.WorkflowContext;

import java.util.Map;

public class TestExecutionCondition extends AbstractJiraCondition {

    @Override
    public void passesCondition(Map<String, Object> args, Issue issue, WorkflowContext workflowContext)
            throws WorkflowException {

        if (issue == null) {
            return;
        }

        String issueType = issue.getIssueTypeObject().getName();

        if ("Test".equals(issueType)) {
            String status = issue.getStatus().getName();
            setReturnValue(args, "Ready".equals(status) || "In Progress".equals(status));
        }
    }
}