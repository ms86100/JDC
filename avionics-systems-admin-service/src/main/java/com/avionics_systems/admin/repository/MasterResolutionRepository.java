package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.MasterResolutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterResolutionRepository extends JpaRepository<MasterResolutionEntity, UUID> {

    Optional<MasterResolutionEntity> findByResolutionKey(String resolutionKey);

    List<MasterResolutionEntity> findByIsActiveTrueOrderBySortOrderAsc();

    Optional<MasterResolutionEntity> findByIsDefaultTrue();

    boolean existsByResolutionKey(String resolutionKey);
}
