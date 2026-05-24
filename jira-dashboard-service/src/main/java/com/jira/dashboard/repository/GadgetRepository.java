package com.jira.dashboard.repository;

import com.jira.dashboard.entity.Gadget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GadgetRepository extends JpaRepository<Gadget, UUID> {

    List<Gadget> findByIsEnabledTrue();

    List<Gadget> findByCategory(String category);

    Optional<Gadget> findByModuleKey(String moduleKey);

    @Query("SELECT g FROM Gadget g WHERE g.isEnabled = true ORDER BY g.category, g.title")
    List<Gadget> findAllEnabledGadgets();

    @Query("SELECT g FROM Gadget g WHERE g.isEnabled = true AND g.category = :category")
    List<Gadget> findEnabledByCategory(@Param("category") String category);

    boolean existsByModuleKey(String moduleKey);
}