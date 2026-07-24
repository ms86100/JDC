package com.jira.issue.repository;

import com.jira.issue.entity.CustomFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, UUID> {

    List<CustomFieldValue> findByIssueId(UUID issueId);

    Optional<CustomFieldValue> findByIssueIdAndFieldId(UUID issueId, UUID fieldId);

    @Modifying
    @Query("DELETE FROM CustomFieldValue v WHERE v.issueId = :issueId AND v.fieldId = :fieldId")
    void deleteByIssueIdAndFieldId(@Param("issueId") UUID issueId, @Param("fieldId") UUID fieldId);

    @Modifying
    @Query("DELETE FROM CustomFieldValue v WHERE v.issueId = :issueId")
    void deleteAllByIssueId(@Param("issueId") UUID issueId);

    @Query("SELECT v FROM CustomFieldValue v WHERE v.fieldId = :fieldId")
    List<CustomFieldValue> findByFieldId(@Param("fieldId") UUID fieldId);
}
