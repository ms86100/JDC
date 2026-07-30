package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestVersionRepository extends JpaRepository<TestVersion, UUID> {

    List<TestVersion> findByTestIdOrderByVersionNumberDesc(UUID testId);
}