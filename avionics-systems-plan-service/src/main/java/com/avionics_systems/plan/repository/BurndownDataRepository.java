package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.BurndownData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BurndownDataRepository extends JpaRepository<BurndownData, UUID> {
    List<BurndownData> findBySprintIdOrderByDataDateAsc(UUID sprintId);
    List<BurndownData> findBySprintIdAndDataDateBetweenOrderByDataDateAsc(UUID sprintId, LocalDate startDate, LocalDate endDate);
}