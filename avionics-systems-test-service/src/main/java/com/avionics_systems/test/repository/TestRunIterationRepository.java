package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestRunIteration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestRunIterationRepository extends JpaRepository<TestRunIteration, UUID> {

    List<TestRunIteration> findByTestRunIdOrderByIterationIndexAsc(UUID testRunId);

    long countByTestRunId(UUID testRunId);
}
