package com.avionics_systems.test.archunit;

import com.avionics_systems.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class TestServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.avionics_systems.test");
    }
}
