package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowScreenTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowScreenTabRepository extends JpaRepository<WorkflowScreenTab, UUID> {
    List<WorkflowScreenTab> findByScreenIdOrderByOrderIndexAsc(UUID screenId);
    void deleteByScreenId(UUID screenId);
}