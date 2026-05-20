package com.jira.test.repository;

import com.jira.test.entity.QuarantineTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuarantineTransitionRepository extends JpaRepository<QuarantineTransition, UUID> {

    List<QuarantineTransition> findByQuarantineIdOrderByTransitionedAtDesc(UUID quarantineId);
}