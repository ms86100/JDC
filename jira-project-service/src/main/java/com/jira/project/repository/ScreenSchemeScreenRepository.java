package com.jira.project.repository;

import com.jira.project.entity.ScreenSchemeScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenSchemeScreenRepository extends JpaRepository<ScreenSchemeScreen, ScreenSchemeScreen.IdClass> {

    List<ScreenSchemeScreen> findBySchemeId(UUID schemeId);
}