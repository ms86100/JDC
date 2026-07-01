package com.jira.portal.repository;

import com.jira.portal.entity.CustomerPortal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerPortalRepository extends JpaRepository<CustomerPortal, UUID> {

    List<CustomerPortal> findByProjectId(UUID projectId);

    Optional<CustomerPortal> findByPortalKey(String portalKey);

    List<CustomerPortal> findByStatus(String status);

    @Query("SELECT cp FROM CustomerPortal cp WHERE cp.isPublic = true AND cp.status = 'PUBLISHED'")
    List<CustomerPortal> findPublicPortals();

    @Query("SELECT cp FROM CustomerPortal cp WHERE cp.status = 'PUBLISHED'")
    List<CustomerPortal> findPublishedPortals();
}