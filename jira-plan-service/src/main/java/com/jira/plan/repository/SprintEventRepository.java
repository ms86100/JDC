package com.jira.plan.repository;

import com.jira.plan.entity.SprintEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SprintEventRepository extends JpaRepository<SprintEvent, Long> {

    List<SprintEvent> findBySprintIdOrderByEventTimestampAsc(UUID sprintId);
}
