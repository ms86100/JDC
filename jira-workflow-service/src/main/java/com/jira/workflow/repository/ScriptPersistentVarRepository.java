package com.jira.workflow.repository;

import com.jira.workflow.entity.ScriptPersistentVar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScriptPersistentVarRepository extends JpaRepository<ScriptPersistentVar, UUID> {

    Optional<ScriptPersistentVar> findByVarKeyAndScopeAndScopeId(String varKey, String scope, UUID scopeId);

    Optional<ScriptPersistentVar> findByVarKeyAndScopeAndScopeIdIsNull(String varKey, String scope);

    void deleteByVarKeyAndScopeAndScopeId(String varKey, String scope, UUID scopeId);

    void deleteByVarKeyAndScopeAndScopeIdIsNull(String varKey, String scope);
}
