package com.jira.test.repository;

import com.jira.test.entity.TestStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestStepRepository extends JpaRepository<TestStep, UUID> {

    List<TestStep> findByTestIdOrderByStepOrderAsc(UUID testId);

    void deleteByTestId(UUID testId);
}