package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.SprintProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintPropertyRepository extends JpaRepository<SprintProperty, UUID> {

    List<SprintProperty> findBySprintId(UUID sprintId);

    Optional<SprintProperty> findBySprintIdAndPropertyKey(UUID sprintId, String propertyKey);

    void deleteBySprintIdAndPropertyKey(UUID sprintId, String propertyKey);
}
