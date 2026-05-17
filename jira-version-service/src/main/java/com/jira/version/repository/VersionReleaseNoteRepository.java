package com.jira.version.repository;

import com.jira.version.entity.VersionReleaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VersionReleaseNoteRepository extends JpaRepository<VersionReleaseNote, UUID> {

    Optional<VersionReleaseNote> findByVersionId(UUID versionId);

    boolean existsByVersionId(UUID versionId);
}