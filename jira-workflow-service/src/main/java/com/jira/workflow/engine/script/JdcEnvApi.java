package com.jira.workflow.engine.script;

import com.jira.workflow.config.ScriptEngineProperties;
import org.graalvm.polyglot.HostAccess;

import java.util.List;

public class JdcEnvApi {

    private final List<String> whitelistKeys;

    public JdcEnvApi(ScriptEngineProperties properties) {
        this.whitelistKeys = properties.getEnvWhitelistKeys();
    }

    @HostAccess.Export
    public String get(String key) {
        if (key == null || key.isBlank()) return null;
        if (whitelistKeys == null || whitelistKeys.isEmpty()) return null;
        if (!whitelistKeys.contains(key)) return null;
        return System.getenv(key);
    }
}
