package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.RequirementLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequirementLinkRepository extends JpaRepository<RequirementLink, UUID> {

    List<RequirementLink> findByRequirementKey(String requirementKey);

    List<RequirementLink> findByTestId(UUID testId);

    List<RequirementLink> findByRequirementKeyAndTestId(String requirementKey, UUID testId);

    List<RequirementLink> findByProjectId(UUID projectId);

    boolean existsByRequirementKeyAndTestId(String requirementKey, UUID testId);
}