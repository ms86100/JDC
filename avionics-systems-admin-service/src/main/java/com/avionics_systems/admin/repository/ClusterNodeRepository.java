package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ClusterNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClusterNodeRepository extends JpaRepository<ClusterNodeEntity, String> {
    Optional<ClusterNodeEntity> findByNodeId(String nodeId);
}