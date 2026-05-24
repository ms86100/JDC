package com.jira.notification.repository;

import com.jira.notification.entity.NotificationScheme;
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
public interface NotificationSchemeRepository extends JpaRepository<NotificationScheme, UUID> {

    Optional<NotificationScheme> findByName(String name);

    List<NotificationScheme> findByProjectId(UUID projectId);

    Page<NotificationScheme> findByCreatedBy(UUID createdBy, Pageable pageable);

    @Query("SELECT ns FROM NotificationScheme ns WHERE ns.projectId = :projectId AND ns.isDefault = true")
    Optional<NotificationScheme> findDefaultByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT ns FROM NotificationScheme ns WHERE ns.isDefault = true")
    List<NotificationScheme> findAllDefault();

    boolean existsByName(String name);

    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationScheme ns WHERE ns.projectId = :projectId AND ns.isDefault = true")
    boolean existsDefaultSchemeForProject(@Param("projectId") UUID projectId);
}