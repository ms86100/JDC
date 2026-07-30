package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.SprintBurndown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintBurndownRepository extends JpaRepository<SprintBurndown, Long> {

    List<SprintBurndown> findBySprintIdOrderBySnapshotDateAsc(UUID sprintId);

    Optional<SprintBurndown> findBySprintIdAndSnapshotDate(UUID sprintId, LocalDate snapshotDate);

    boolean existsBySprintIdAndSnapshotDate(UUID sprintId, LocalDate snapshotDate);
}