package com.jira.admin.repository;

import com.jira.admin.entity.IssueTypeSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IssueTypeSchemeRepository extends JpaRepository<IssueTypeSchemeEntity, String> {
    Optional<IssueTypeSchemeEntity> findByName(String name);
}