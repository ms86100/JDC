package com.jira.plan.repository;

import com.jira.plan.entity.BoardPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardPermissionRepository extends JpaRepository<BoardPermission, UUID> {

    List<BoardPermission> findByBoardConfigId(UUID boardId);

    List<BoardPermission> findByBoardConfigIdAndPermissionType(UUID boardId, String permissionType);

    List<BoardPermission> findByBoardConfigIdAndPrincipalTypeAndPrincipalId(UUID boardId, String principalType, String principalId);

    boolean existsByBoardConfigIdAndPermissionTypeAndPrincipalId(UUID boardId, String permissionType, String principalId);

    boolean existsByBoardConfigIdAndPrincipalId(UUID boardId, String principalId);

    void deleteByBoardConfigId(UUID boardId);
}