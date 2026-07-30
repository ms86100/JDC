package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.PriorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PriorityRepository extends JpaRepository<PriorityEntity, String> {
    Optional<PriorityEntity> findByName(String name);
}