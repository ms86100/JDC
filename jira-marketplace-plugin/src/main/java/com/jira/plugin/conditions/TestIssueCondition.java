package com.jira.plugin.conditions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.WorkflowException;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.opensymphony.workflow.WorkflowContext;

import java.util.Map;

public class TestIssueCondition extends AbstractJiraCondition {

    @Override
    public void passesCondition(Map<String, Object> args, Issue issue, WorkflowContext workflowContext)
            throws WorkflowException {

        if (issue == null) {
            setReturnValue(args, false);
            return;
        }

        String issueType = issue.getIssueTypeObject().getName();
        setReturnValue(args, "Test".equals(issueType)
                           || "Test Set".equals(issueType)
                           || "Test Plan".equals(issueType));
    }
}