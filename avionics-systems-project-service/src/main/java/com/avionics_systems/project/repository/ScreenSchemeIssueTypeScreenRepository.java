package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.ScreenSchemeIssueTypeScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenSchemeIssueTypeScreenRepository
        extends JpaRepository<ScreenSchemeIssueTypeScreen, ScreenSchemeIssueTypeScreen.IdClass> {

    List<ScreenSchemeIssueTypeScreen> findBySchemeId(UUID schemeId);

    List<ScreenSchemeIssueTypeScreen> findBySchemeIdAndIssueTypeId(UUID schemeId, UUID issueTypeId);
}
