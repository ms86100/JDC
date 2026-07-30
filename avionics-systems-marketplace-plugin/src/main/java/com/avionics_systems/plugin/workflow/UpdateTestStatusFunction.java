package com.avionics_systems.plugin.workflow;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.WorkflowException;
import com.atlassian.jira.workflow.postfunction.AbstractIssueFunction;
import com.avionics_systems.plugin.config.PluginConfig;
import com.opensymphony.workflow.WorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UpdateTestStatusFunction extends AbstractIssueFunction {

    private static final Logger log = LoggerFactory.getLogger(UpdateTestStatusFunction.class);

    @Override
    public void execute(Map<String, Object> args, Issue issue, WorkflowContext workflowContext)
            throws WorkflowException {

        if (issue == null) {
            return;
        }

        String issueType = issue.getIssueTypeObject().getName();
        PluginConfig config = PluginConfig.getInstance();
        String testIssueType = config != null ? config.getTestIssueType() : "Test";

        if (!testIssueType.equals(issueType)) {
            return;
        }

        String destinationStatus = getParameter(args, "destinationStatus", String.class);

        log.info("Updating test status for issue {} to {}", issue.getKey(), destinationStatus);
    }
}
