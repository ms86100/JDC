package com.jira.workflow.engine.script;

import com.jira.workflow.entity.ScriptDefinition;
import com.jira.workflow.repository.ScriptDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class JdcIncludeApi {

    private final ScriptDefinitionRepository repository;
    private final GraalScriptEngine engine;
    private final Set<String> included = new HashSet<>();
    private final StringBuilder libraryCode = new StringBuilder();

    public JdcIncludeApi(ScriptDefinitionRepository repository, GraalScriptEngine engine) {
        this.repository = repository;
        this.engine = engine;
    }

    @HostAccess.Export
    public boolean include(String scriptKey) {
        try {
            if (scriptKey == null || included.contains(scriptKey)) return false;
            Optional<ScriptDefinition> script = repository.findByScriptKey(scriptKey);
            if (script.isEmpty() || !Boolean.TRUE.equals(script.get().getIsEnabled())) {
                log.warn("Include failed: script '{}' not found or disabled", scriptKey);
                return false;
            }
            included.add(scriptKey);
            libraryCode.append("// --- included: ").append(scriptKey).append(" ---\n");
            libraryCode.append(script.get().getScriptBody()).append("\n");
            return true;
        } catch (Exception e) {
            log.warn("Include failed for '{}': {}", scriptKey, e.getMessage());
            return false;
        }
    }

    public String getIncludedCode() {
        return libraryCode.toString();
    }

    public boolean hasIncludes() {
        return !included.isEmpty();
    }

    public Set<String> getIncludedKeys() {
        return java.util.Collections.unmodifiableSet(included);
    }
}
