package com.jira.migration.archunit;

import com.jira.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class MigrationServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.jira.migration");
    }
}
