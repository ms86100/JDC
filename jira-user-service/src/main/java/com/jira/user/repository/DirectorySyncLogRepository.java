package com.jira.user.repository;

import com.jira.user.entity.DirectorySyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DirectorySyncLogRepository extends JpaRepository<DirectorySyncLog, UUID> {

    Page<DirectorySyncLog> findByDirectoryIdOrderByStartedAtDesc(UUID directoryId, Pageable pageable);
}
