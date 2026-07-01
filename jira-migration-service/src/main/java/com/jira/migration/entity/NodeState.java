package com.jira.migration.entity;

/**
 * Enumeration of possible states for a cluster node.
 */
public enum NodeState {
    /**
     * Node is starting up and initializing.
     */
    STARTING,

    /**
     * Node is active and ready to accept work.
     */
    ACTIVE,

    /**
     * Node is on standby, ready to take over if needed.
     */
    STANDBY,

    /**
     * Node is draining (completing current work before shutdown).
     */
    DRAINING,

    /**
     * Node has been terminated or is unreachable.
     */
    TERMINATED,

    /**
     * Node has failed unexpectedly.
     */
    FAILED
}