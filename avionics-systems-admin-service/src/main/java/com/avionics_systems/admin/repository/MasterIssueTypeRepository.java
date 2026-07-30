package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.MasterIssueTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterIssueTypeRepository extends JpaRepository<MasterIssueTypeEntity, UUID> {

    Optional<MasterIssueTypeEntity> findByTypeKey(String typeKey);

    List<MasterIssueTypeEntity> findByIsActiveTrueOrderBySortOrderAsc();

    List<MasterIssueTypeEntity> findByIsSubtaskFalseAndIsActiveTrueOrderBySortOrderAsc();

    List<MasterIssueTypeEntity> findByIsSubtaskTrueAndIsActiveTrueOrderBySortOrderAsc();

    boolean existsByTypeKey(String typeKey);
}
