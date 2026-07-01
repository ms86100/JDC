package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "index_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String indexName;

    private Long documentCount;

    private Long sizeInBytes;

    @Column(name = "shard_count")
    private Integer shardCount;

    @Column(name = "replica_count")
    private Integer replicaCount;

    @Column(name = "last_optimize")
    private LocalDateTime lastOptimize;

    @Column(name = "health_status")
    private String healthStatus;
}