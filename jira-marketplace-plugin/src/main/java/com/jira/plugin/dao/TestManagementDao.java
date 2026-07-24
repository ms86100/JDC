package com.jira.plugin.dao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.ComponentImport;
import net.java.amateros.xray.entities.TestEntity;
import net.java.amateros.xray.entities.TestStepEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestManagementDao {

    private final ActiveObjects ao;

    @Autowired
    public TestManagementDao(@ComponentImport ActiveObjects ao) {
        this.ao = ao;
    }

    public TestEntity createTest(String projectId, String name, String testType) {
        TestEntity test = ao.create(TestEntity.class);
        test.setProjectId(projectId);
        test.setName(name);
        test.setTestType(testType);
        test.setStatus("DRAFT");
        test.save();
        return test;
    }

    public TestEntity getTest(String id) {
        return ao.get(TestEntity.class, id);
    }

    public List<TestEntity> findTestsByProject(String projectId) {
        return List.of();
    }

    public TestStepEntity addStep(String testId, String description, String expectedResult) {
        TestEntity test = getTest(testId);
        TestStepEntity step = ao.create(TestStepEntity.class);
        step.setTest(test);
        step.setDescription(description);
        step.setExpectedResult(expectedResult);
        step.setOrderIndex(test.getSteps().size());
        step.save();
        return step;
    }
}