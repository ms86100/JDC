package com.jira.admin.repository;

import com.jira.admin.entity.BackupScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupScheduleRepository extends JpaRepository<BackupScheduleEntity, String> {
}
