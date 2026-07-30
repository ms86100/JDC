package com.avionics_systems.sprint.repository;

import com.avionics_systems.sprint.entity.SavedFilterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedFilterRepository extends JpaRepository<SavedFilterEntity, UUID> {
    List<SavedFilterEntity> findByIsSystemTrue();
    List<SavedFilterEntity> findByOwnerIdAndIsSystemFalse(UUID ownerId);
    List<SavedFilterEntity> findByIsSharedTrueAndIsSystemFalse();
}
