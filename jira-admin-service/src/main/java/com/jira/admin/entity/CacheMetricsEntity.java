package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cache_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String cacheName;

    @Column(nullable = false)
    private String nodeId;

    private Long hits;

    private Long misses;

    private Long size;

    private Long capacity;

    @Column(name = "hit_ratio")
    private Double hitRatio;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}