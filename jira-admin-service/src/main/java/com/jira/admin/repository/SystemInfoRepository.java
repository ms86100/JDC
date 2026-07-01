package com.jira.admin.repository;

import com.jira.admin.entity.SystemInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemInfoRepository extends JpaRepository<SystemInfoEntity, String> {
    Optional<SystemInfoEntity> findByKey(String key);
}