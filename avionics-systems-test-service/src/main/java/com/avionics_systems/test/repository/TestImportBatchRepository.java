package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestImportBatchRepository extends JpaRepository<TestImportBatch, UUID> {

    List<TestImportBatch> findByStatus(String status);

    List<TestImportBatch> findByImportType(String importType);

    @Query("SELECT b FROM TestImportBatch b WHERE b.createdAt >= :since ORDER BY b.createdAt DESC")
    List<TestImportBatch> findRecent(@Param("since") LocalDateTime since);

    @Query("SELECT b FROM TestImportBatch b WHERE b.ciSource = :source ORDER BY b.createdAt DESC")
    List<TestImportBatch> findByCiSource(@Param("source") String source);

    @Query("SELECT b FROM TestImportBatch b WHERE b.ciBuildUrl = :url AND b.ciBuildNumber = :buildNum")
    List<TestImportBatch> findByBuildInfo(@Param("url") String url, @Param("buildNum") String buildNum);
}