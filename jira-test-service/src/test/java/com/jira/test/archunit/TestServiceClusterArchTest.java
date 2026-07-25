package com.jira.test.archunit;

import com.jira.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class TestServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.jira.test");
    }
}
