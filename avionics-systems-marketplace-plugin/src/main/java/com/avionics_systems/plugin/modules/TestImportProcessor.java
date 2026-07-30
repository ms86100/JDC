package com.avionics_systems.plugin.modules;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.spring.scanner.annotation.ComponentImport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TestImportProcessor implements ModuleDescriptor<ModuleDescriptor> {

    @Value("${plugin.importProcessor.completeKey:com.avionics_systems.platform.avionics-systems-test-management-plugin:test-import-processor}")
    private String completeKey;

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
        return completeKey;
    }
}
