package com.jira.test.repository;

import com.jira.test.entity.ScreenSchemeScreen;
import com.jira.test.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenSchemeScreenRepository extends JpaRepository<ScreenSchemeScreen, UUID> {

    List<ScreenSchemeScreen> findByScreenSchemeId(UUID screenSchemeId);

    Optional<ScreenSchemeScreen> findByScreenSchemeIdAndScreenType(UUID screenSchemeId, Screen.ScreenType screenType);

    Optional<ScreenSchemeScreen> findByScreenSchemeIdAndScreenId(UUID screenSchemeId, UUID screenId);

    boolean existsByScreenSchemeIdAndScreenType(UUID screenSchemeId, Screen.ScreenType screenType);

    void deleteByScreenSchemeIdAndScreenId(UUID screenSchemeId, UUID screenId);

    List<ScreenSchemeScreen> findByScreenId(UUID screenId);
}