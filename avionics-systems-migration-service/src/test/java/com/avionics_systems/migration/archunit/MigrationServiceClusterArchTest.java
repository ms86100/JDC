package com.avionics_systems.migration.archunit;

import com.avionics_systems.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class MigrationServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.avionics_systems.migration");
    }
}
