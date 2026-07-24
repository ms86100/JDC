package com.jira.workflow.engine.script;

import com.jira.workflow.entity.ScriptPersistentVar;
import com.jira.workflow.repository.ScriptPersistentVarRepository;
import org.graalvm.polyglot.HostAccess;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class JdcPersistentVarApi {

    private final ScriptPersistentVarRepository repository;
    private final Map<String, Object> context;

    public JdcPersistentVarApi(ScriptPersistentVarRepository repository, Map<String, Object> context) {
        this.repository = repository;
        this.context = context;
    }

    @HostAccess.Export
    public String get(String key) {
        try {
            if (key == null) return null;
            Optional<ScriptPersistentVar> var = repository.findByVarKeyAndScopeAndScopeIdIsNull(key, "GLOBAL");
            return var.map(ScriptPersistentVar::getVarValue).orElse(null);
        } catch (Exception e) { return null; }
    }

    @HostAccess.Export
    public boolean set(String key, String value) {
        try {
            if (key == null) return false;
            Optional<ScriptPersistentVar> existing = repository.findByVarKeyAndScopeAndScopeIdIsNull(key, "GLOBAL");
            if (existing.isPresent()) {
                existing.get().setVarValue(value);
                repository.save(existing.get());
            } else {
                repository.save(ScriptPersistentVar.builder()
                        .varKey(key).varValue(value).scope("GLOBAL").build());
            }
            return true;
        } catch (Exception e) { return false; }
    }

    @HostAccess.Export
    public boolean remove(String key) {
        try {
            if (key == null) return false;
            repository.deleteByVarKeyAndScopeAndScopeIdIsNull(key, "GLOBAL");
            return true;
        } catch (Exception e) { return false; }
    }

    @HostAccess.Export
    public String getForIssue(String key) {
        try {
            Object issueId = context.get("issueId");
            if (key == null || issueId == null) return null;
            Optional<ScriptPersistentVar> var = repository.findByVarKeyAndScopeAndScopeId(
                    key, "ISSUE", UUID.fromString(issueId.toString()));
            return var.map(ScriptPersistentVar::getVarValue).orElse(null);
        } catch (Exception e) { return null; }
    }

    @HostAccess.Export
    public boolean setForIssue(String key, String value) {
        try {
            Object issueId = context.get("issueId");
            if (key == null || issueId == null) return false;
            UUID scopeId = UUID.fromString(issueId.toString());
            Optional<ScriptPersistentVar> existing = repository.findByVarKeyAndScopeAndScopeId(key, "ISSUE", scopeId);
            if (existing.isPresent()) {
                existing.get().setVarValue(value);
                repository.save(existing.get());
            } else {
                repository.save(ScriptPersistentVar.builder()
                        .varKey(key).varValue(value).scope("ISSUE").scopeId(scopeId).build());
            }
            return true;
        } catch (Exception e) { return false; }
    }

    @HostAccess.Export
    public String getForProject(String key) {
        try {
            Object projectId = context.get("projectId");
            if (key == null || projectId == null) return null;
            Optional<ScriptPersistentVar> var = repository.findByVarKeyAndScopeAndScopeId(
                    key, "PROJECT", UUID.fromString(projectId.toString()));
            return var.map(ScriptPersistentVar::getVarValue).orElse(null);
        } catch (Exception e) { return null; }
    }

    @HostAccess.Export
    public boolean setForProject(String key, String value) {
        try {
            Object projectId = context.get("projectId");
            if (key == null || projectId == null) return false;
            UUID scopeId = UUID.fromString(projectId.toString());
            Optional<ScriptPersistentVar> existing = repository.findByVarKeyAndScopeAndScopeId(key, "PROJECT", scopeId);
            if (existing.isPresent()) {
                existing.get().setVarValue(value);
                repository.save(existing.get());
            } else {
                repository.save(ScriptPersistentVar.builder()
                        .varKey(key).varValue(value).scope("PROJECT").scopeId(scopeId).build());
            }
            return true;
        } catch (Exception e) { return false; }
    }
}
