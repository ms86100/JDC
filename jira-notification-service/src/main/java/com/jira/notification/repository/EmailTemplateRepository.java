package com.jira.notification.repository;

import com.jira.notification.entity.EmailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    Optional<EmailTemplate> findByTemplateKey(String templateKey);

    List<EmailTemplate> findByEventType(String eventType);

    @Query("SELECT et FROM EmailTemplate et WHERE et.eventType = :eventType AND et.enabled = true")
    List<EmailTemplate> findActiveByEventType(@Param("eventType") String eventType);

    @Query("SELECT et FROM EmailTemplate et WHERE et.eventType = :eventType AND et.isDefault = true AND et.enabled = true")
    Optional<EmailTemplate> findDefaultByEventType(@Param("eventType") String eventType);

    Page<EmailTemplate> findByCreatedBy(UUID createdBy, Pageable pageable);

    @Query("SELECT et FROM EmailTemplate et WHERE et.enabled = true")
    List<EmailTemplate> findAllActive();

    boolean existsByTemplateKey(String templateKey);

    @Query("SELECT CASE WHEN COUNT(et) > 0 THEN true ELSE false END FROM EmailTemplate et WHERE et.eventType = :eventType AND et.isDefault = true")
    boolean existsDefaultForEventType(@Param("eventType") String eventType);
}