package com.jira.test.repository;

import com.jira.test.entity.TestQuarantine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestQuarantineRepository extends JpaRepository<TestQuarantine, UUID> {

    Optional<TestQuarantine> findByTestId(UUID testId);

    List<TestQuarantine> findByStatus(String status);

    List<TestQuarantine> findAllByOrderByTriggeredAtDesc();

    boolean existsByTestIdAndStatusIn(UUID testId, List<String> statuses);
}