package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.ScreenSchemeScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScreenSchemeScreenRepository extends JpaRepository<ScreenSchemeScreen, ScreenSchemeScreen.IdClass> {

    List<ScreenSchemeScreen> findBySchemeId(UUID schemeId);
}