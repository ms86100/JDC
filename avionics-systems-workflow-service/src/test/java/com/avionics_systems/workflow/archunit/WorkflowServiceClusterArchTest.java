package com.avionics_systems.workflow.archunit;

import com.avionics_systems.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class WorkflowServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.avionics_systems.workflow");
    }
}
