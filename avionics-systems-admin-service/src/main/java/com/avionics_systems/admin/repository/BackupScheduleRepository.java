package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.BackupScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupScheduleRepository extends JpaRepository<BackupScheduleEntity, String> {
}
