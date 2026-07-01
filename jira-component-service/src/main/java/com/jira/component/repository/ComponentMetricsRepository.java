package com.jira.component.repository;

import com.jira.component.entity.ComponentMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComponentMetricsRepository extends JpaRepository<ComponentMetrics, UUID> {

    List<ComponentMetrics> findByComponentIdOrderBySnapshotDateAsc(UUID componentId);

    Optional<ComponentMetrics> findByComponentIdAndSnapshotDate(UUID componentId, LocalDate snapshotDate);

    @Query("SELECT cm FROM ComponentMetrics cm WHERE cm.componentId = :componentId ORDER BY cm.snapshotDate DESC LIMIT 1")
    Optional<ComponentMetrics> findLatestByComponentId(@Param("componentId") UUID componentId);
}