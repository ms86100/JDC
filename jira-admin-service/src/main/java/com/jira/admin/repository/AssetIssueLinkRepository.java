package com.jira.admin.repository;

import com.jira.admin.entity.AssetIssueLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetIssueLinkRepository extends JpaRepository<AssetIssueLinkEntity, UUID> {

    List<AssetIssueLinkEntity> findByAssetId(UUID assetId);

    List<AssetIssueLinkEntity> findByIssueId(UUID issueId);

    boolean existsByAssetIdAndIssueId(UUID assetId, UUID issueId);
}
