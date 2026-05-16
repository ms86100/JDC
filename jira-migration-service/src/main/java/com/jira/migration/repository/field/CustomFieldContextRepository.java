package com.jira.migration.repository.field;

import com.jira.migration.entity.field.CustomFieldContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomFieldContextRepository extends JpaRepository<CustomFieldContext, UUID> {

    List<CustomFieldContext> findByCustomFieldId(UUID customFieldId);

    List<CustomFieldContext> findByAllProjects(Boolean allProjects);

    @Query("SELECT cfc FROM CustomFieldContext cfc WHERE cfc.enabled = true")
    List<CustomFieldContext> findAllEnabled();

    @Query("SELECT cfc FROM CustomFieldContext cfc WHERE cfc.customFieldId = :customFieldId AND cfc.enabled = true")
    List<CustomFieldContext> findEnabledByCustomFieldId(UUID customFieldId);

    @Query("SELECT cfc FROM CustomFieldContext cfc WHERE cfc.customFieldId = :customFieldId AND cfc.allProjects = true")
    List<CustomFieldContext> findGlobalContextsByCustomFieldId(UUID customFieldId);
}