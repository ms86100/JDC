package com.jira.plugin.modules;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.spring.scanner.annotation.ComponentImport;
import org.springframework.stereotype.Component;

@Component
public class TestImportProcessor implements ModuleDescriptor<ModuleDescriptor> {

    public TestImportProcessor() {
    }

    public void processCucumberImport(byte[] featureContent) {
        // Parse .feature file content
    }

    public void processJUnitImport(byte[] xmlContent) {
        // Parse JUnit XML results
    }

    @Override
    public String getCompleteKey() {
        return "com.jira.platform.jira-test-management-plugin:test-import-processor";
    }
}