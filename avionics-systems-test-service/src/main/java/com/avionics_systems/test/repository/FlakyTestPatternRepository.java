package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.FlakyTestPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlakyTestPatternRepository extends JpaRepository<FlakyTestPattern, UUID> {

    List<FlakyTestPattern> findByTestId(UUID testId);

    List<FlakyTestPattern> findByPatternType(String patternType);

    List<FlakyTestPattern> findByRootCauseCategory(String rootCauseCategory);
}