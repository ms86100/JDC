package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.InitiativePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InitiativePlanRepository extends JpaRepository<InitiativePlan, UUID> {

    List<InitiativePlan> findByInitiativeIdOrderBySequenceAsc(UUID initiativeId);

    List<InitiativePlan> findByPlanId(UUID planId);

    Optional<InitiativePlan> findByInitiativeIdAndPlanId(UUID initiativeId, UUID planId);

    boolean existsByInitiativeIdAndPlanId(UUID initiativeId, UUID planId);

    void deleteByInitiativeIdAndPlanId(UUID initiativeId, UUID planId);
}