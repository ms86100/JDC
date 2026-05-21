package com.jira.test.repository;

import com.jira.test.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {

    List<Screen> findByScreenTypeOrderByNameAsc(Screen.ScreenType screenType);

    Optional<Screen> findByName(String name);

    List<Screen> findByNameContainingIgnoreCase(String searchTerm);
}