package com.jira.test.repository;

import com.jira.test.entity.TestFolderTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestFolderTemplateRepository extends JpaRepository<TestFolderTemplate, UUID> {

    List<TestFolderTemplate> findByIsSystemTemplate(Boolean isSystemTemplate);

    List<TestFolderTemplate> findByCategory(String category);

    List<TestFolderTemplate> findByFolderType(String folderType);

    @Query("SELECT t FROM TestFolderTemplate t WHERE t.isSystemTemplate = true OR t.createdBy = :userId")
    List<TestFolderTemplate> findAvailableForUser(@Param("userId") UUID userId);

    @Query("SELECT t FROM TestFolderTemplate t WHERE t.name LIKE %:query% OR t.description LIKE %:query%")
    List<TestFolderTemplate> searchByName(@Param("query") String query);

    @Query("SELECT t FROM TestFolderTemplate t ORDER BY t.usageCount DESC")
    List<TestFolderTemplate> findMostUsed();
}