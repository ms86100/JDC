package com.avionics_systems.migration.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class ProjectWorkflowXmlBootstrap {

    private static final String RESOURCE = "migration/bootstrap-workflow.xml";

    public String loadDefaultWorkflowXml() {
        try {
            ClassPathResource resource = new ClassPathResource(RESOURCE);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Missing bootstrap workflow: " + RESOURCE, e);
        }
    }
}
