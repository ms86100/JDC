package com.jira.test.repository;

import com.jira.test.entity.TestFolderAccess;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestFolderAccessRepository extends JpaRepository<TestFolderAccess, UUID> {

    List<TestFolderAccess> findByFolderId(UUID folderId);

    Optional<TestFolderAccess> findByFolderIdAndUserId(UUID folderId, UUID userId);

    Optional<TestFolderAccess> findByFolderIdAndGroupId(UUID folderId, UUID groupId);

    @Query("SELECT fa FROM TestFolderAccess fa WHERE fa.userId = :userId ORDER BY fa.lastAccessedAt DESC")
    List<TestFolderAccess> findRecentByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT fa FROM TestFolderAccess fa WHERE fa.folderId = :folderId AND fa.lastAccessedAt < :before")
    List<TestFolderAccess> findByFolderIdAndLastAccessedBefore(
            @Param("folderId") UUID folderId,
            @Param("before") LocalDateTime before);

    @Modifying
    @Query("UPDATE TestFolderAccess fa SET fa.lastAccessedAt = :accessedAt WHERE fa.folderId = :folderId AND fa.userId = :userId")
    void updateLastAccessed(
            @Param("folderId") UUID folderId,
            @Param("userId") UUID userId,
            @Param("accessedAt") LocalDateTime accessedAt);

    void deleteByFolderId(UUID folderId);

    void deleteByFolderIdAndUserId(UUID folderId, UUID userId);

    void deleteByFolderIdAndGroupId(UUID folderId, UUID groupId);

    @Query("SELECT COUNT(fa) FROM TestFolderAccess fa WHERE fa.folderId = :folderId")
    long countByFolderId(@Param("folderId") UUID folderId);
}