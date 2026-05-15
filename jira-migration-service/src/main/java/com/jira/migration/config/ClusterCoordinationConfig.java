package com.jira.migration.config;

import com.jira.migration.cluster.ClusterNodeRegistry;
import com.jira.migration.cluster.DistributedLockService;
import com.jira.migration.cluster.LeaderElectionServiceImpl;
import com.jira.migration.cluster.ClusterHealthMonitor;
import com.jira.migration.cluster.ShutdownHookService;
import com.jira.migration.repository.DistributedLockRepository;
import com.jira.migration.repository.ClusterNodeRepository;
import com.jira.migration.repository.LeaderElectionRepository;
import com.jira.migration.repository.JobClaimRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for cluster coordination services.
 */
@Slf4j
@Configuration
@EnableScheduling
public class ClusterCoordinationConfig {

    @Autowired
    private ClusterConfig clusterConfig;

    /**
     * Ensure ClusterConfig is properly initialized.
     */
    @Bean
    public ClusterConfigInitializer clusterConfigInitializer() {
        return new ClusterConfigInitializer(clusterConfig);
    }

    /**
     * Helper class to ensure cluster config is initialized early.
     */
    private static class ClusterConfigInitializer {
        @Autowired
        private ClusterNodeRepository clusterNodeRepository;

        @Autowired
        private DistributedLockRepository distributedLockRepository;

        @Autowired
        private LeaderElectionRepository leaderElectionRepository;

        @Autowired
        private JobClaimRepository jobClaimRepository;

        ClusterConfigInitializer(ClusterConfig config) {
            log.info("Cluster configuration loaded: nodeId={}, host={}, port={}",
                    config.getNodeId(), config.getHost(), config.getPort());
            log.info("Lock config: type={}, defaultTtl={}min, waitTimeout={}s",
                    config.getLock().getType(),
                    config.getLock().getDefaultTtlMinutes(),
                    config.getLock().getWaitTimeoutSeconds());
            log.info("Leader config: enabled={}, leaseDuration={}min, renewalInterval={}s",
                    config.getLeader().isEnabled(),
                    config.getLeader().getLeaseDurationMinutes(),
                    config.getLeader().getRenewalIntervalSeconds());
        }
    }
}