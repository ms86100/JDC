package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.MasterQuickFilterPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MasterQuickFilterPresetRepository extends JpaRepository<MasterQuickFilterPresetEntity, UUID> {

    List<MasterQuickFilterPresetEntity> findByIsActiveTrueOrderBySortOrderAsc();
}
