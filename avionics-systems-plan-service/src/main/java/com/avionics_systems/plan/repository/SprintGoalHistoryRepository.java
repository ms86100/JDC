package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.SprintGoalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SprintGoalHistoryRepository extends JpaRepository<SprintGoalHistory, UUID> {
    List<SprintGoalHistory> findBySprintIdOrderByChangedAtDesc(UUID sprintId);
}