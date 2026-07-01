package com.jira.test.repository;

import com.jira.test.entity.CodeChangeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodeChangeEventRepository extends JpaRepository<CodeChangeEvent, UUID> {

    List<CodeChangeEvent> findByProjectIdOrderByTimestampDesc(UUID projectId);

    Optional<CodeChangeEvent> findByCommitSha(String commitSha);

    List<CodeChangeEvent> findByProjectIdAndBranch(UUID projectId, String branch);

    List<CodeChangeEvent> findByProjectIdAndPrId(UUID projectId, String prId);
}