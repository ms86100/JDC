package com.avionics_systems.migration.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.config.ClusterConfig;
import com.avionics_systems.migration.entity.LeaderElection;
import com.avionics_systems.migration.repository.LeaderElectionRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of leader election using database-based consensus.
 * Uses compare-and-swap semantics for leader election.
 */
@Slf4j
@Service
public class LeaderElectionServiceImpl implements LeaderElectionService {

    private final LeaderElectionRepository leaderElectionRepository;
    private final ClusterConfig clusterConfig;
    private final ObjectMapper objectMapper;

    // Local cache of listeners
    private final Map<String, List<LeaderChangeListener>> listeners = new ConcurrentHashMap<>();

    // Local cache of leadership state
    private final Map<String, LeaderInfo> localLeaderCache = new ConcurrentHashMap<>();

    @Autowired
    public LeaderElectionServiceImpl(LeaderElectionRepository leaderElectionRepository,
                                     ClusterConfig clusterConfig) {
        this.leaderElectionRepository = leaderElectionRepository;
        this.clusterConfig = clusterConfig;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryBecomeLeader(String leadershipGroup) {
        String nodeId = clusterConfig.getNodeId();
        Instant now = Instant.now();
        Instant leaseExpiry = now.plus(clusterConfig.getLeader().getLeaseDuration());

        try {
            // Check for existing leader
            Optional<LeaderElection> existingLeader = leaderElectionRepository
                    .findCurrentLeader(leadershipGroup, now);

            if (existingLeader.isPresent()) {
                LeaderElection current = existingLeader.get();
                // If current leader is this node, renew the lease
                if (current.getNodeId().equals(nodeId)) {
                    return renewLeaseInternal(leadershipGroup, nodeId, now, leaseExpiry);
                }
                // Another node is the leader
                log.debug("Cannot become leader for {} - current leader is {}", leadershipGroup, current.getNodeId());
                return false;
            }

            // Try to create new leader entry
            LeaderElection newLeader = LeaderElection.builder()
                    .leadershipGroup(leadershipGroup)
                    .leaderId(nodeId)
                    .nodeId(nodeId)
                    .host(clusterConfig.getHost())
                    .port(clusterConfig.getPort())
                    .electedAt(now)
                    .lastHeartbeat(now)
                    .leaseExpiresAt(leaseExpiry)
                    .term(1L)
                    .votes(1)
                    .build();

            leaderElectionRepository.save(newLeader);
            log.info("Node {} became leader for {}", nodeId, leadershipGroup);

            // Update local cache and notify listeners
            LeaderInfo newLeaderInfo = createLeaderInfo(newLeader);
            LeaderInfo oldLeader = localLeaderCache.put(leadershipGroup, newLeaderInfo);
            notifyLeaderChange(leadershipGroup, oldLeader, newLeaderInfo);

            return true;

        } catch (Exception e) {
            log.error("Error trying to become leader for {}: {}", leadershipGroup, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isLeader(String leadershipGroup) {
        String nodeId = clusterConfig.getNodeId();
        return getLeader(leadershipGroup)
                .map(leader -> leader.getNodeId().equals(nodeId))
                .orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<LeaderInfo> getLeader(String leadershipGroup) {
        try {
            Optional<LeaderElection> election = leaderElectionRepository
                    .findCurrentLeader(leadershipGroup, Instant.now());

            return election.map(this::createLeaderInfo);
        } catch (Exception e) {
            log.error("Error getting leader for {}: {}", leadershipGroup, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resign(String leadershipGroup) {
        String nodeId = clusterConfig.getNodeId();

        try {
            Optional<LeaderElection> election = leaderElectionRepository
                    .findByLeadershipGroup(leadershipGroup);

            if (election.isPresent() && election.get().getNodeId().equals(nodeId)) {
                leaderElectionRepository.deleteByGroup(leadershipGroup);
                log.info("Node {} resigned as leader for {}", nodeId, leadershipGroup);

                // Update cache and notify listeners
                LeaderInfo oldLeader = localLeaderCache.remove(leadershipGroup);
                notifyLeaderChange(leadershipGroup, oldLeader, null);
            }
        } catch (Exception e) {
            log.error("Error resigning leadership for {}: {}", leadershipGroup, e.getMessage(), e);
        }
    }

    @Override
    public List<String> getLeadershipGroups() {
        try {
            return leaderElectionRepository.findAllLeadershipGroups();
        } catch (Exception e) {
            log.error("Error getting leadership groups: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void addLeaderChangeListener(String leadershipGroup, LeaderChangeListener listener) {
        listeners.computeIfAbsent(leadershipGroup, k -> new ArrayList<>()).add(listener);
        log.debug("Added leader change listener for group {}", leadershipGroup);
    }

    @Override
    public void removeLeaderChangeListener(String leadershipGroup, LeaderChangeListener listener) {
        List<LeaderChangeListener> groupListeners = listeners.get(leadershipGroup);
        if (groupListeners != null) {
            groupListeners.remove(listener);
            log.debug("Removed leader change listener for group {}", leadershipGroup);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean extendLeadership(String leadershipGroup) {
        String nodeId = clusterConfig.getNodeId();
        Instant now = Instant.now();
        Instant newLeaseExpiry = now.plus(clusterConfig.getLeader().getLeaseDuration());

        return renewLeaseInternal(leadershipGroup, nodeId, now, newLeaseExpiry);
    }

    @Override
    public long getTerm(String leadershipGroup) {
        return getLeader(leadershipGroup)
                .map(LeaderInfo::getTerm)
                .orElse(0L);
    }

    @Override
    public boolean hasActiveLeader(String leadershipGroup) {
        return getLeader(leadershipGroup).isPresent();
    }

    @Override
    public List<LeaderInfo> getAllLeaders() {
        try {
            List<LeaderElection> elections = leaderElectionRepository.findAllActiveElections(Instant.now());
            List<LeaderInfo> leaders = new ArrayList<>();
            for (LeaderElection election : elections) {
                leaders.add(createLeaderInfo(election));
            }
            return leaders;
        } catch (Exception e) {
            log.error("Error getting all leaders: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteLeadershipGroup(String leadershipGroup) {
        try {
            leaderElectionRepository.deleteByGroup(leadershipGroup);
            localLeaderCache.remove(leadershipGroup);
            log.info("Deleted leadership group {}", leadershipGroup);
        } catch (Exception e) {
            log.error("Error deleting leadership group {}: {}", leadershipGroup, e.getMessage(), e);
        }
    }

    /**
     * Internal method to renew leader lease.
     */
    private boolean renewLeaseInternal(String leadershipGroup, String nodeId, Instant heartbeat, Instant leaseExpiry) {
        int renewed = leaderElectionRepository.renewLease(leadershipGroup, nodeId, heartbeat, leaseExpiry);
        if (renewed > 0) {
            log.debug("Renewed lease for leadership group {}", leadershipGroup);
            return true;
        }
        return false;
    }

    /**
     * Create LeaderInfo from LeaderElection entity.
     */
    private LeaderInfo createLeaderInfo(LeaderElection election) {
        return LeaderInfo.builder()
                .group(election.getLeadershipGroup())
                .leaderId(election.getLeaderId())
                .nodeId(election.getNodeId())
                .host(election.getHost())
                .port(election.getPort())
                .electedAt(election.getElectedAt())
                .lastHeartbeat(election.getLastHeartbeat())
                .leaseExpiresAt(election.getLeaseExpiresAt())
                .term(election.getTerm())
                .votes(election.getVotes())
                .build();
    }

    /**
     * Notify listeners of leader change.
     */
    private void notifyLeaderChange(String group, LeaderInfo oldLeader, LeaderInfo newLeader) {
        List<LeaderChangeListener> groupListeners = listeners.get(group);
        if (groupListeners != null) {
            for (LeaderChangeListener listener : groupListeners) {
                try {
                    listener.onLeaderChanged(group, oldLeader, newLeader);
                } catch (Exception e) {
                    log.error("Error notifying leader change listener for group {}: {}", group, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Scheduled task to renew leadership lease.
     */
    @Scheduled(fixedDelayString = "${cluster.leader.renewal-interval-seconds:30}000")
    @SchedulerLock(name = "LeaderElectionServiceImpl_scheduledLeadershipRenewal", lockAtMostFor = "PT24S", lockAtLeastFor = "PT12S")
    public void scheduledLeadershipRenewal() {
        if (!clusterConfig.getLeader().isEnabled()) {
            return;
        }

        for (String group : getLeadershipGroups()) {
            if (isLeader(group)) {
                try {
                    extendLeadership(group);
                } catch (Exception e) {
                    log.error("Error in scheduled leadership renewal for {}: {}", group, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Scheduled task to clean up expired elections.
     */
    @Scheduled(fixedDelayString = "${cluster.leader.cleanup-interval-seconds:60}000")
    @SchedulerLock(name = "LeaderElectionServiceImpl_cleanupExpiredElections", lockAtMostFor = "PT48S", lockAtLeastFor = "PT24S")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupExpiredElections() {
        try {
            int cleaned = leaderElectionRepository.deleteExpiredElections(Instant.now());
            if (cleaned > 0) {
                log.info("Cleaned up {} expired leader elections", cleaned);
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired elections: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up all leadership when node shuts down.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupAllLeadership() {
        String nodeId = clusterConfig.getNodeId();
        try {
            leaderElectionRepository.deleteByNodeId(nodeId);
            log.info("Cleaned up all leadership for node {}", nodeId);
        } catch (Exception e) {
            log.error("Error cleaning up leadership for node {}: {}", nodeId, e.getMessage(), e);
        }
    }
}