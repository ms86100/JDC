package com.jira.project.repository;

import com.jira.project.entity.ScreenSchemeIssueTypeScreen;
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
