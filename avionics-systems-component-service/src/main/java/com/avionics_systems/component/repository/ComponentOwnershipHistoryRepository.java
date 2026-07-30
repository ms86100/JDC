package com.avionics_systems.component.repository;

import com.avionics_systems.component.entity.ComponentOwnershipHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentOwnershipHistoryRepository extends JpaRepository<ComponentOwnershipHistory, UUID> {

    List<ComponentOwnershipHistory> findByComponentIdOrderByTransferredAtDesc(UUID componentId);
}
