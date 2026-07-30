package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.TestService;
import com.avionics_systems.test.service.TraceabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Test Data Seeding Controller - For development/testing only
 * Allows seeding test data without authentication for demo purposes
 */
@RestController
@RequestMapping("/test-auth")
@RequiredArgsConstructor
@Slf4j
public class TestDataSeedController {

    private final TestService testService;
    private final TraceabilityService traceabilityService;

    @PostMapping("/seed-all")
    public ResponseEntity<SeedResponse> seedAllData() {
        log.info("Seeding all test data...");
        SeedResponse response = new SeedResponse();

        try {
            UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

            // Create tests
            TestResponse test1 = testService.createTest(CreateTestRequest.builder()
                    .projectId(projectId)
                    .name("Login Functionality Test")
                    .description("Test login with valid and invalid credentials")
                    .testType("MANUAL")
                    .priority("HIGH")
                    .labels(List.of("login", "smoke-test"))
                    .requirementKeys(List.of("PROJ-001"))
                    .steps(List.of(
                            CreateTestRequest.TestStepDto.builder().stepType("GIVEN").description("User is on the login page").expectedResult("Login page is displayed").build(),
                            CreateTestRequest.TestStepDto.builder().stepType("WHEN").description("User enters valid credentials").expectedResult("Credentials are accepted").build(),
                            CreateTestRequest.TestStepDto.builder().stepType("THEN").description("User clicks login button").expectedResult("User is redirected to dashboard").build()
                    ))
                    .build());
            response.testsCreated.add(test1.getName());

            TestResponse test2 = testService.createTest(CreateTestRequest.builder()
                    .projectId(projectId)
                    .name("Payment Processing Test")
                    .description("Test payment with credit card and PayPal")
                    .testType("AUTOMATED")
                    .priority("CRITICAL")
                    .labels(List.of("payment", "regression"))
                    .requirementKeys(List.of("PROJ-002"))
                    .steps(List.of(
                            CreateTestRequest.TestStepDto.builder().stepType("GIVEN").description("User has items in cart").expectedResult("Cart has items").build(),
                            CreateTestRequest.TestStepDto.builder().stepType("WHEN").description("User selects credit card payment").expectedResult("Payment form is displayed").build(),
                            CreateTestRequest.TestStepDto.builder().stepType("THEN").description("User enters card details and submits").expectedResult("Payment is processed").build()
                    ))
                    .build());
            response.testsCreated.add(test2.getName());

            TestResponse test3 = testService.createTest(CreateTestRequest.builder()
                    .projectId(projectId)
                    .name("Search Functionality Test")
                    .description("Test search with various filters")
                    .testType("MANUAL")
                    .priority("MEDIUM")
                    .labels(List.of("search", "ui-test"))
                    .requirementKeys(List.of("PROJ-003"))
                    .steps(List.of(
                            CreateTestRequest.TestStepDto.builder().stepType("GIVEN").description("User is on the search page").expectedResult("Search page is displayed").build(),
                            CreateTestRequest.TestStepDto.builder().stepType("WHEN").description("User enters search query with filters").expectedResult("Results are filtered").build(),
                            CreateTestRequest.TestStepDto.builder().stepType("THEN").description("User views results").expectedResult("Relevant results shown").build()
                    ))
                    .build());
            response.testsCreated.add(test3.getName());

            // Link requirements to tests
            traceabilityService.linkRequirementToTest(RequirementLinkRequest.builder()
                    .requirementKey("PROJ-001")
                    .testId(test1.getId())
                    .build());
            response.requirementsLinked.add("PROJ-001 -> " + test1.getName());

            traceabilityService.linkRequirementToTest(RequirementLinkRequest.builder()
                    .requirementKey("PROJ-002")
                    .testId(test2.getId())
                    .build());
            response.requirementsLinked.add("PROJ-002 -> " + test2.getName());

            traceabilityService.linkRequirementToTest(RequirementLinkRequest.builder()
                    .requirementKey("PROJ-003")
                    .testId(test3.getId())
                    .build());
            response.requirementsLinked.add("PROJ-003 -> " + test3.getName());

            response.success = true;
            response.message = "Successfully seeded " + response.testsCreated.size() + " tests with " + response.requirementsLinked.size() + " requirement links";

        } catch (Exception e) {
            log.error("Error seeding data: {}", e.getMessage(), e);
            response.success = false;
            response.message = "Error: " + e.getMessage();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        try {
            UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<TestResponse> tests = testService.getTestsByProject(projectId);

            StatsResponse stats = new StatsResponse();
            stats.totalTests = tests.size();
            stats.manualTests = (int) tests.stream().filter(t -> "MANUAL".equals(t.getTestType())).count();
            stats.automatedTests = (int) tests.stream().filter(t -> "AUTOMATED".equals(t.getTestType())).count();
            stats.bddTests = (int) tests.stream().filter(t -> "CUKE".equals(t.getTestType())).count();

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    public static class SeedResponse {
        public boolean success = false;
        public String message = "";
        public List<String> testsCreated = new java.util.ArrayList<>();
        public List<String> requirementsLinked = new java.util.ArrayList<>();
    }

    public static class StatsResponse {
        public int totalTests = 0;
        public int manualTests = 0;
        public int automatedTests = 0;
        public int bddTests = 0;
    }
}