package com.avionics_systems.plugin;

import com.atlassian.plugin.spring.scanner.annotation.ComponentImport;
import com.atlassian.spring.container.ContainerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TestManagementPlugin {

    private final ContainerContext containerContext;

    @Value("${plugin.version:1.0.0}")
    private String pluginVersion;

    @Value("${plugin.key:com.avionics_systems.platform.avionics-systems-test-management-plugin}")
    private String pluginKey;

    @Autowired
    public TestManagementPlugin(@ComponentImport ContainerContext containerContext) {
        this.containerContext = containerContext;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public String getPluginKey() {
        return pluginKey;
    }
}
