package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cluster_nodes")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String nodeId;

    @Column(nullable = false)
    private String nodeName;

    private String host;

    @Column(name = "ip_address")
    private String ipAddress;

    private Integer port;

    private String nodeState = "ACTIVE";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_heartbeat")
    private Long lastHeartbeat;

    private String version;

    private String roles;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_usage")
    private Double memoryUsage;
}