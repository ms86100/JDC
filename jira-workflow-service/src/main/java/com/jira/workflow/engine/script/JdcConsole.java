package com.jira.workflow.engine.script;

import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class JdcConsole {

    private static final Logger log = LoggerFactory.getLogger("jdc.script.console");
    private static final int MAX_CAPTURE_SIZE = 65536;
    private final StringBuilder capturedOutput = new StringBuilder();
    private boolean truncated = false;

    @HostAccess.Export
    public void log(Object... args) {
        String msg = formatArgs(args);
        capture("LOG", msg);
        JdcConsole.log.info("[script] {}", sanitize(msg));
    }

    @HostAccess.Export
    public void info(Object... args) {
        String msg = formatArgs(args);
        capture("INFO", msg);
        JdcConsole.log.info("[script] {}", sanitize(msg));
    }

    @HostAccess.Export
    public void debug(Object... args) {
        String msg = formatArgs(args);
        capture("DEBUG", msg);
        JdcConsole.log.debug("[script] {}", sanitize(msg));
    }

    @HostAccess.Export
    public void warn(Object... args) {
        String msg = formatArgs(args);
        capture("WARN", msg);
        JdcConsole.log.warn("[script] {}", sanitize(msg));
    }

    @HostAccess.Export
    public void error(Object... args) {
        String msg = formatArgs(args);
        capture("ERROR", msg);
        JdcConsole.log.error("[script] {}", sanitize(msg));
    }

    @HostAccess.Export
    public void table(Object data) {
        String msg = String.valueOf(data);
        capture("TABLE", msg);
        JdcConsole.log.info("[script:table] {}", sanitize(msg));
    }

    @HostAccess.Export
    public void dir(Object obj) {
        String msg = String.valueOf(obj);
        capture("DIR", msg);
        JdcConsole.log.info("[script:dir] {}", sanitize(msg));
    }

    @HostAccess.Export
    public void trace(Object... args) {
        String msg = formatArgs(args);
        capture("TRACE", msg);
        JdcConsole.log.debug("[script:trace] {}", sanitize(msg));
    }

    public String getCapturedOutput() {
        String output = capturedOutput.toString();
        return truncated ? output + "\n[output truncated at 64KB]" : output;
    }

    private void capture(String level, String msg) {
        if (!truncated && capturedOutput.length() < MAX_CAPTURE_SIZE) {
            capturedOutput.append('[').append(level).append("] ").append(sanitize(msg)).append('\n');
        } else {
            truncated = true;
        }
    }

    private String sanitize(String msg) {
        return msg.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
    }

    private String formatArgs(Object[] args) {
        if (args == null) return "null";
        return Arrays.stream(args)
                .map(a -> a == null ? "null" : String.valueOf(a))
                .collect(Collectors.joining(" "));
    }
}
