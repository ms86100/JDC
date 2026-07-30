package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.MasterStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterStatusRepository extends JpaRepository<MasterStatusEntity, UUID> {

    Optional<MasterStatusEntity> findByStatusKey(String statusKey);

    List<MasterStatusEntity> findByIsActiveTrueOrderBySortOrderAsc();

    List<MasterStatusEntity> findByCategoryOrderBySortOrderAsc(String category);

    boolean existsByStatusKey(String statusKey);
}
