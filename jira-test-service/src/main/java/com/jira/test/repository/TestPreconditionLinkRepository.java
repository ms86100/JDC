package com.jira.test.repository;

import com.jira.test.entity.TestPreconditionLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestPreconditionLinkRepository extends JpaRepository<TestPreconditionLink, UUID> {

    List<TestPreconditionLink> findByTestId(UUID testId);

    List<TestPreconditionLink> findByPreconditionId(UUID preconditionId);

    Optional<TestPreconditionLink> findByTestIdAndPreconditionId(UUID testId, UUID preconditionId);

    boolean existsByTestIdAndPreconditionId(UUID testId, UUID preconditionId);

    void deleteByTestIdAndPreconditionId(UUID testId, UUID preconditionId);

    void deleteByTestId(UUID testId);

    void deleteByPreconditionId(UUID preconditionId);

    default List<TestPreconditionLink> findByTestIdOrderByStepOrderAsc(UUID testId) {
        return findByTestId(testId).stream()
                .sorted(Comparator.comparingInt(link -> link.getStepOrder() != null ? link.getStepOrder() : 0))
                .toList();
    }
}