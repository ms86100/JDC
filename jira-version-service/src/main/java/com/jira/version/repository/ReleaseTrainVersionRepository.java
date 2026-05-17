package com.jira.version.repository;

import com.jira.version.entity.ReleaseTrainVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReleaseTrainVersionRepository extends JpaRepository<ReleaseTrainVersion, UUID> {

    List<ReleaseTrainVersion> findByTrainIdOrderBySequenceAsc(UUID trainId);

    List<ReleaseTrainVersion> findByVersionId(UUID versionId);

    void deleteByTrainIdAndVersionId(UUID trainId, UUID versionId);

    boolean existsByTrainIdAndVersionId(UUID trainId, UUID versionId);
}