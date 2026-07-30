package com.avionics_systems.portal.repository;

import com.avionics_systems.portal.entity.CustomerPortal;
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

    @Query("SELECT cp FROM CustomerPortal cp WHERE cp.isPublic = true AND cp.status = :status")
    List<CustomerPortal> findPublicPortals(@Param("status") String status);

    @Query("SELECT cp FROM CustomerPortal cp WHERE cp.status = :status")
    List<CustomerPortal> findPublishedPortals(@Param("status") String status);
}