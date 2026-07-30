package com.avionics_systems.plan.archunit;

import com.avionics_systems.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class PlanServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.avionics_systems.plan");
    }
}
