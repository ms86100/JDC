package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.MasterPriorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterPriorityRepository extends JpaRepository<MasterPriorityEntity, UUID> {

    Optional<MasterPriorityEntity> findByPriorityKey(String priorityKey);

    List<MasterPriorityEntity> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<MasterPriorityEntity> findByIsDefaultTrue();

    boolean existsByPriorityKey(String priorityKey);
}
