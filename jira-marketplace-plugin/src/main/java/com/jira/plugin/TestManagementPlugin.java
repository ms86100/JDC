package com.jira.plugin;

import com.atlassian.plugin.spring.scanner.annotation.ComponentImport;
import com.atlassian.spring.container.ContainerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestManagementPlugin {

    private final ContainerContext containerContext;

    @Autowired
    public TestManagementPlugin(@ComponentImport ContainerContext containerContext) {
        this.containerContext = containerContext;
    }

    public String getPluginVersion() {
        return "1.0.0";
    }

    public String getPluginKey() {
        return "com.jira.platform.jira-test-management-plugin";
    }
}