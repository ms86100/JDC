package com.jira.admin.repository;

import com.jira.admin.entity.IssueTypeScreenSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IssueTypeScreenSchemeRepository extends JpaRepository<IssueTypeScreenSchemeEntity, String> {
    Optional<IssueTypeScreenSchemeEntity> findByName(String name);
}