package com.avionics_systems.notification.archunit;

import com.avionics_systems.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class NotificationServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.avionics_systems.notification");
    }
}
