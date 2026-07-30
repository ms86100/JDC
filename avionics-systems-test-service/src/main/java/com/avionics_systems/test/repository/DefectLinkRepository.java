package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.DefectLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DefectLinkRepository extends JpaRepository<DefectLink, UUID> {

    List<DefectLink> findByDefectKey(String defectKey);

    List<DefectLink> findByExecutionId(UUID executionId);

    List<DefectLink> findByStepResultId(UUID stepResultId);

    List<DefectLink> findByStatus(String status);
}