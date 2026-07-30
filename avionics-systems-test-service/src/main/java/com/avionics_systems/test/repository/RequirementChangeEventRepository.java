package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.RequirementChangeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequirementChangeEventRepository extends JpaRepository<RequirementChangeEvent, UUID> {

    List<RequirementChangeEvent> findByRequirementIdOrderByCreatedAtDesc(UUID requirementId);
}