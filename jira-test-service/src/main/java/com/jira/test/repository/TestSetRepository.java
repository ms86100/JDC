package com.jira.test.repository;

import com.jira.test.entity.TestIssue;
import com.jira.test.entity.TestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestSetRepository extends JpaRepository<TestSet, UUID> {

    List<TestSet> findByProjectIdAndArchivedFalse(UUID projectId);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM TestIssue t JOIN TestSetItem tsi ON t.id = tsi.testId WHERE tsi.testSetId = :testSetId")
    List<TestIssue> findTestsByTestSetId(@Param("testSetId") UUID testSetId);
}