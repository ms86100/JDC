package com.jira.migration.repository;

import com.jira.migration.entity.BackupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BackupEntityRepository extends JpaRepository<BackupEntity, UUID> {

    List<BackupEntity> findByBackupIdOrderBySequenceOrderAsc(UUID backupId);

    List<BackupEntity> findByBackupIdAndEntityType(UUID backupId, String entityType);

    List<BackupEntity> findByBackupIdAndParentKey(UUID backupId, String parentKey);

    long countByBackupId(UUID backupId);

    long countByBackupIdAndEntityType(UUID backupId, String entityType);

    void deleteByBackupId(UUID backupId);
}