package com.jira.admin.repository;

import com.jira.admin.entity.MasterQuickFilterPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MasterQuickFilterPresetRepository extends JpaRepository<MasterQuickFilterPresetEntity, UUID> {

    List<MasterQuickFilterPresetEntity> findByIsActiveTrueOrderBySortOrderAsc();
}
