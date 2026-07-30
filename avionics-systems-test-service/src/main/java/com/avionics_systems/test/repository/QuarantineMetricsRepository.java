package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.QuarantineMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuarantineMetricsRepository extends JpaRepository<QuarantineMetrics, UUID> {

    List<QuarantineMetrics> findByQuarantineIdOrderByMetricDateDesc(UUID quarantineId);
}