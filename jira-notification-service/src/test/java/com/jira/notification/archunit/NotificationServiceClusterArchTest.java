package com.jira.notification.archunit;

import com.jira.cluster.archunit.ClusterSafetyArchTest;
import org.junit.jupiter.api.Test;

class NotificationServiceClusterArchTest {

    @Test
    void enforceClusterSafetyRules() {
        ClusterSafetyArchTest.verifyClusterSafety("com.jira.notification");
    }
}
