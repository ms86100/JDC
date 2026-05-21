package com.jira.test.repository;

import com.jira.test.entity.PreconditionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreconditionTemplateRepository extends JpaRepository<PreconditionTemplate, UUID> {

    List<PreconditionTemplate> findByCategory(String category);

    List<PreconditionTemplate> findByIsSystemTemplate(Boolean isSystemTemplate);

    List<PreconditionTemplate> findByPreconditionType(String preconditionType);

    List<PreconditionTemplate> findAllByOrderByUsageCountDesc();
}