package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.IvvCardMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IvvCardMetadataRepository extends JpaRepository<IvvCardMetadata, UUID> {

    Optional<IvvCardMetadata> findByIssueId(UUID issueId);

    List<IvvCardMetadata> findByVvmCardId(UUID vvmCardId);
}
