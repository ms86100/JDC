package com.jira.admin.repository;

import com.jira.admin.entity.GlobalPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GlobalPermissionRepository extends JpaRepository<GlobalPermissionEntity, String> {
    List<GlobalPermissionEntity> findByGroupId(String groupId);
    List<GlobalPermissionEntity> findByUserId(String userId);
}