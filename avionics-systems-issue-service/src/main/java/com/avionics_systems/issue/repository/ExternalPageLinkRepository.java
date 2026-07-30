package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.ExternalPageLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExternalPageLinkRepository extends JpaRepository<ExternalPageLink, UUID> {
    List<ExternalPageLink> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}
