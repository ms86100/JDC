package com.jira.admin.repository;

import com.jira.admin.entity.SystemSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettingsEntity, String> {
    Optional<SystemSettingsEntity> findBySettingKey(String settingKey);
    List<SystemSettingsEntity> findByCategory(String category);
}