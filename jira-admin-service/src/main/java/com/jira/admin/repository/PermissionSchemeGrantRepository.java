package com.jira.admin.repository;

import com.jira.admin.entity.PermissionSchemeGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionSchemeGrantRepository extends JpaRepository<PermissionSchemeGrantEntity, String> {

    List<PermissionSchemeGrantEntity> findByPermissionSchemeId(String permissionSchemeId);

    @Query("SELECT psg FROM PermissionSchemeGrantEntity psg WHERE psg.permissionSchemeId = :schemeId AND psg.holderType = :holderType AND psg.holderId = :holderId")
    List<PermissionSchemeGrantEntity> findBySchemeAndHolder(@Param("schemeId") String schemeId, @Param("holderType") String holderType, @Param("holderId") String holderId);

    @Query("SELECT psg FROM PermissionSchemeGrantEntity psg WHERE psg.permissionSchemeId = :schemeId AND psg.holderId IN :holderIds")
    List<PermissionSchemeGrantEntity> findBySchemeAndHolders(@Param("schemeId") String schemeId, @Param("holderIds") List<String> holderIds);

    boolean existsByPermissionSchemeIdAndHolderTypeAndHolderId(String schemeId, String holderType, String holderId);

    Optional<PermissionSchemeGrantEntity> findByPermissionSchemeIdAndPermissionIdAndHolderTypeAndHolderId(
            String schemeId, String permissionId, String holderType, String holderId);
}