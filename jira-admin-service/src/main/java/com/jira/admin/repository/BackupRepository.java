package com.jira.admin.repository;

import com.jira.admin.entity.BackupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupRepository extends JpaRepository<BackupEntity, String> {
    List<BackupEntity> findAllByOrderByStartedAtDesc();
}
