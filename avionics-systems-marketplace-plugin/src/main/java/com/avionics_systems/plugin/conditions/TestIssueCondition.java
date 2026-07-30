package com.avionics_systems.plugin.conditions;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.WorkflowException;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.avionics_systems.plugin.config.PluginConfig;
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
        PluginConfig config = PluginConfig.getInstance();

        if (config != null) {
            setReturnValue(args, config.getManagedIssueTypes().contains(issueType));
        } else {
            setReturnValue(args, false);
        }
    }
}
