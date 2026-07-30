package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ScreenSchemeScreen;
import com.avionics_systems.test.entity.Screen;
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

    void deleteByScreenSchemeId(UUID screenSchemeId);

    List<ScreenSchemeScreen> findByScreenId(UUID screenId);
}