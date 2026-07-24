package com.jira.admin.repository;

import com.jira.admin.entity.AssetTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetTypeRepository extends JpaRepository<AssetTypeEntity, UUID> {

    List<AssetTypeEntity> findByIsActiveTrue();
}
