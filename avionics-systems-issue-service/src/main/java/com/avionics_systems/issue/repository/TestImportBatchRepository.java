package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.TestImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestImportBatchRepository extends JpaRepository<TestImportBatch, UUID> {

    List<TestImportBatch> findByStatusOrderByStartedAtDesc(String status);

    List<TestImportBatch> findByImportTypeOrderByStartedAtDesc(String importType);

    List<TestImportBatch> findByCiSourceOrderByStartedAtDesc(String ciSource);

    @Query("SELECT tb FROM TestImportBatch tb WHERE tb.ciBuildUrl = :buildUrl")
    List<TestImportBatch> findByCiBuildUrl(@Param("buildUrl") String buildUrl);

    @Query("SELECT tb FROM TestImportBatch tb WHERE tb.startedAt >= :start AND tb.startedAt <= :end ORDER BY tb.startedAt DESC")
    List<TestImportBatch> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT tb FROM TestImportBatch tb WHERE tb.status = 'PROCESSING' ORDER BY tb.startedAt ASC")
    List<TestImportBatch> findPendingImports();

    @Query("SELECT AVG(tb.totalTests) FROM TestImportBatch tb WHERE tb.status = 'COMPLETED'")
    Double findAverageTestsPerImport();

    @Query("SELECT SUM(tb.totalPassed) * 1.0 / SUM(tb.totalTests) FROM TestImportBatch tb WHERE tb.status = 'COMPLETED' AND tb.totalTests > 0")
    Double findOverallPassRate();
}