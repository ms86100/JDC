package com.jira.admin.repository;

import com.jira.admin.entity.ClusterNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClusterNodeRepository extends JpaRepository<ClusterNodeEntity, String> {
    Optional<ClusterNodeEntity> findByNodeId(String nodeId);
}