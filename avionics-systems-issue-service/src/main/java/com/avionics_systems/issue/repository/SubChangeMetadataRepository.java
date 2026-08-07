package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.SubChangeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubChangeMetadataRepository extends JpaRepository<SubChangeMetadata, UUID> {

    Optional<SubChangeMetadata> findByIssueId(UUID issueId);

    List<SubChangeMetadata> findByParentChangeCardId(UUID parentId);
}
