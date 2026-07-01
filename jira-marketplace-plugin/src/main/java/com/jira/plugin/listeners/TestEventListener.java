package com.jira.plugin.listeners;

import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.ModificationItem;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class TestEventListener {

    @EventListener
    public void onIssueCreated(IssueEvent event) {
        Issue issue = event.getIssue();

        if ("Test".equals(issue.getIssueTypeObject().getName())) {
            handleTestCreated(issue);
        }
    }

    @EventListener
    public void onIssueUpdated(IssueEvent event) {
        Issue issue = event.getIssue();
        List<ModificationItem> modifications = event.getModificationLog().getModificationItems();

        if ("Test".equals(issue.getIssueTypeObject().getName())) {
            handleTestUpdated(issue, modifications);
        }
    }

    @EventListener
    public void onIssueDeleted(IssueEvent event) {
        Issue issue = event.getIssue();

        if ("Test".equals(issue.getIssueTypeObject().getName())) {
            handleTestDeleted(issue);
        }
    }

    private void handleTestCreated(Issue issue) {
        // Create audit log entry
    }

    private void handleTestUpdated(Issue issue, List<ModificationItem> modifications) {
        // Track status changes, field updates
    }

    private void handleTestDeleted(Issue issue) {
        // Clean up related data
    }
}