package com.avionics_systems.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class JdcIncludeApi {

    private final Set<String> resolvedIncludes;

    public JdcIncludeApi(Set<String> resolvedIncludes) {
        this.resolvedIncludes = resolvedIncludes != null ? resolvedIncludes : new HashSet<>();
    }

    @HostAccess.Export
    public boolean isIncluded(String scriptKey) {
        return resolvedIncludes.contains(scriptKey);
    }

    @HostAccess.Export
    public String[] getIncludedScripts() {
        return resolvedIncludes.toArray(new String[0]);
    }

    @HostAccess.Export
    public boolean include(String scriptKey) {
        log.debug("Runtime include('{}') called — includes are resolved at compile-time via textual prepending. " +
                "Use include('key') in your script body and it will be resolved before execution.", scriptKey);
        return resolvedIncludes.contains(scriptKey);
    }
}
