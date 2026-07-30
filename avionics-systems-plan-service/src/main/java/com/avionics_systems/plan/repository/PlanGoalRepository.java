package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.PlanGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanGoalRepository extends JpaRepository<PlanGoal, UUID> {

    List<PlanGoal> findByPlanId(UUID planId);

    List<PlanGoal> findByParentGoalId(UUID parentGoalId);

    List<PlanGoal> findByStatus(String status);

    List<PlanGoal> findByPlanIdAndParentGoalIdIsNull(UUID planId);

    List<PlanGoal> findByPlanIdAndStatus(UUID planId, String status);
}
