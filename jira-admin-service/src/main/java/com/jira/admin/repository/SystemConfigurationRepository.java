package com.jira.admin.repository;

import com.jira.admin.entity.SystemConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfigurationEntity, UUID> {

    Optional<SystemConfigurationEntity> findByConfigKey(String configKey);

    List<SystemConfigurationEntity> findByCategory(String category);

    List<SystemConfigurationEntity> findByCategoryOrderByConfigKeyAsc(String category);

    boolean existsByConfigKey(String configKey);
}
