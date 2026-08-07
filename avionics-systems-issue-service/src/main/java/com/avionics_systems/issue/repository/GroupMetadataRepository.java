package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.GroupMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMetadataRepository extends JpaRepository<GroupMetadata, UUID> {

    Optional<GroupMetadata> findByIssueId(UUID issueId);
}
