package com.jira.admin.repository;

import com.jira.admin.entity.MasterLinkTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterLinkTypeRepository extends JpaRepository<MasterLinkTypeEntity, UUID> {

    Optional<MasterLinkTypeEntity> findByLinkKey(String linkKey);

    List<MasterLinkTypeEntity> findByIsActiveTrueOrderBySortOrderAsc();

    boolean existsByLinkKey(String linkKey);
}
