package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.MigrationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MigrationEventRepository extends JpaRepository<MigrationEvent, UUID> {

    List<MigrationEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
