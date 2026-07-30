package com.avionics_systems.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class JdcWebhookApi {

    private int responseCode = 200;
    private Object responseBody = null;
    private final Map<String, String> responseHeaders = new LinkedHashMap<>();
    private Map<String, String> requestHeaders = new LinkedHashMap<>();

    @HostAccess.Export
    public void setResponseCode(int code) {
        if (code >= 100 && code < 600) {
            this.responseCode = code;
        }
    }

    @HostAccess.Export
    public void setResponseBody(Object body) {
        this.responseBody = body;
    }

    @HostAccess.Export
    public void setResponseHeader(String name, String value) {
        if (name != null && value != null) {
            this.responseHeaders.put(name, value);
        }
    }

    public void setRequestHeaders(Map<String, String> headers) {
        if (headers != null) {
            this.requestHeaders = new LinkedHashMap<>(headers);
        }
    }

    @HostAccess.Export
    public Map<String, String> getRequestHeaders() {
        return java.util.Collections.unmodifiableMap(requestHeaders);
    }

    public int getResponseCode() { return responseCode; }
    public Object getResponseBody() { return responseBody; }
    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public boolean hasOverrides() { return responseCode != 200 || responseBody != null || !responseHeaders.isEmpty(); }
}
