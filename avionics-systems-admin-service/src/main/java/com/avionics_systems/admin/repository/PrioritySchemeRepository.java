package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.PrioritySchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrioritySchemeRepository extends JpaRepository<PrioritySchemeEntity, String> {
    Optional<PrioritySchemeEntity> findByName(String name);
    Optional<PrioritySchemeEntity> findByIsDefaultTrue();
}
