package com.jira.workflow.engine.script;

import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class JdcConsole {

    private static final Logger log = LoggerFactory.getLogger("jdc.script.console");
    private final StringBuilder capturedOutput = new StringBuilder();

    @HostAccess.Export
    public void log(Object... args) {
        String msg = formatArgs(args);
        capturedOutput.append("[LOG] ").append(msg).append('\n');
        log.info("[script] {}", msg);
    }

    @HostAccess.Export
    public void info(Object... args) {
        String msg = formatArgs(args);
        capturedOutput.append("[INFO] ").append(msg).append('\n');
        log.info("[script] {}", msg);
    }

    @HostAccess.Export
    public void debug(Object... args) {
        String msg = formatArgs(args);
        capturedOutput.append("[DEBUG] ").append(msg).append('\n');
        log.debug("[script] {}", msg);
    }

    @HostAccess.Export
    public void warn(Object... args) {
        String msg = formatArgs(args);
        capturedOutput.append("[WARN] ").append(msg).append('\n');
        log.warn("[script] {}", msg);
    }

    @HostAccess.Export
    public void error(Object... args) {
        String msg = formatArgs(args);
        capturedOutput.append("[ERROR] ").append(msg).append('\n');
        log.error("[script] {}", msg);
    }

    @HostAccess.Export
    public void table(Object data) {
        String msg = String.valueOf(data);
        capturedOutput.append("[TABLE] ").append(msg).append('\n');
        log.info("[script:table] {}", msg);
    }

    @HostAccess.Export
    public void dir(Object obj) {
        String msg = String.valueOf(obj);
        capturedOutput.append("[DIR] ").append(msg).append('\n');
        log.info("[script:dir] {}", msg);
    }

    @HostAccess.Export
    public void trace(Object... args) {
        String msg = formatArgs(args);
        capturedOutput.append("[TRACE] ").append(msg).append('\n');
        log.debug("[script:trace] {}", msg);
    }

    public String getCapturedOutput() {
        return capturedOutput.toString();
    }

    private String formatArgs(Object[] args) {
        if (args == null) return "null";
        return Arrays.stream(args)
                .map(a -> a == null ? "null" : String.valueOf(a))
                .collect(Collectors.joining(" "));
    }
}
