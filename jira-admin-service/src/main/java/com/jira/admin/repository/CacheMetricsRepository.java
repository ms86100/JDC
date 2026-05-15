package com.jira.admin.repository;

import com.jira.admin.entity.CacheMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CacheMetricsRepository extends JpaRepository<CacheMetricsEntity, String> {
    List<CacheMetricsEntity> findByNodeId(String nodeId);
}