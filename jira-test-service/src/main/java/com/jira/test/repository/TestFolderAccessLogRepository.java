package com.jira.test.repository;

import com.jira.test.entity.TestFolderAccessLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestFolderAccessLogRepository extends JpaRepository<TestFolderAccessLog, UUID> {

    List<TestFolderAccessLog> findByUserIdOrderByAccessTimeDesc(UUID userId);

    List<TestFolderAccessLog> findByFolderIdOrderByAccessTimeDesc(UUID folderId);

    @Query("SELECT l FROM TestFolderAccessLog l WHERE l.userId = :userId ORDER BY l.accessTime DESC")
    List<TestFolderAccessLog> findRecentByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT l.folderId, COUNT(l) as accessCount FROM TestFolderAccessLog l " +
            "WHERE l.userId = :userId AND l.accessTime >= :since " +
            "GROUP BY l.folderId ORDER BY accessCount DESC")
    List<Object[]> findMostAccessedFolders(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(l) FROM TestFolderAccessLog l WHERE l.folderId = :folderId AND l.accessTime >= :since")
    long countAccessesSince(@Param("folderId") UUID folderId, @Param("since") LocalDateTime since);
}