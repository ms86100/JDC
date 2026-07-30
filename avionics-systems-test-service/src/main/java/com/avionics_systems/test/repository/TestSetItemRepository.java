package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestSetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestSetItemRepository extends JpaRepository<TestSetItem, UUID> {

    List<TestSetItem> findByTestSetId(UUID testSetId);

    List<TestSetItem> findByTestId(UUID testId);

    Optional<TestSetItem> findByTestSetIdAndTestId(UUID testSetId, UUID testId);

    void deleteByTestSetId(UUID testSetId);

    long countByTestSetId(UUID testSetId);
}