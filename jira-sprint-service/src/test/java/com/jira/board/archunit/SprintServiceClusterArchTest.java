package com.jira.board.archunit;

import com.jira.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class SprintServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.jira.board");
    }
}
