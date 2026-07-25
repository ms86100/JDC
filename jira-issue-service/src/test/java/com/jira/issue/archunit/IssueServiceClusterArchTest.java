package com.jira.issue.archunit;

import com.jira.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class IssueServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.jira.issue");
    }
}
