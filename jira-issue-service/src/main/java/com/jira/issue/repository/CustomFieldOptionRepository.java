package com.jira.issue.repository;

import com.jira.issue.entity.CustomFieldOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomFieldOptionRepository extends JpaRepository<CustomFieldOption, UUID> {

    List<CustomFieldOption> findByFieldIdOrderByPositionAsc(UUID fieldId);

    List<CustomFieldOption> findByFieldIdAndDisabledFalseOrderByPositionAsc(UUID fieldId);

    List<CustomFieldOption> findByFieldIdAndParentOptionIdIsNullOrderByPositionAsc(UUID fieldId);

    List<CustomFieldOption> findByParentOptionIdOrderByPositionAsc(UUID parentOptionId);

    @Modifying
    @Query("DELETE FROM CustomFieldOption o WHERE o.fieldId = :fieldId")
    void deleteAllByFieldId(@Param("fieldId") UUID fieldId);

    boolean existsByFieldIdAndId(UUID fieldId, UUID id);

    @Query("SELECT COALESCE(MAX(o.position), -1) FROM CustomFieldOption o WHERE o.fieldId = :fieldId")
    int findMaxPositionByFieldId(@Param("fieldId") UUID fieldId);
}
