package com.jira.test.config;

import com.jira.test.dto.CreateTestRequest;
import com.jira.test.dto.RequirementLinkRequest;
import com.jira.test.dto.TestResponse;
import com.jira.test.service.TestService;
import com.jira.test.service.TraceabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

/**
 * Data Seeding Configuration - Seeds demo data on startup
 * Only runs once when database is empty
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeederConfig {

    @Bean
    CommandLineRunner seedTestData(TestService testService, TraceabilityService traceabilityService) {
        return args -> {
            UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

            // Check if data already exists
            List<TestResponse> existingTests = testService.getTestsByProject(projectId);
            if (!existingTests.isEmpty()) {
                log.info("Test data already exists ({} tests), skipping seed", existingTests.size());
                return;
            }

            log.info("Seeding demo test data...");

            try {
                // Create Test 1: Login Functionality
                TestResponse test1 = testService.createTest(CreateTestRequest.builder()
                        .projectId(projectId)
                        .name("Login Functionality Test")
                        .description("Verify user can login with valid credentials and see appropriate error messages for invalid ones")
                        .testType("MANUAL")
                        .priority("HIGH")
                        .labels(List.of("login", "authentication", "smoke-test"))
                        .requirementKeys(List.of("PROJ-001"))
                        .steps(List.of(
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("GIVEN")
                                        .description("User is on the login page at /login")
                                        .expectedResult("Login form is displayed with username and password fields")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("WHEN")
                                        .description("User enters valid username 'admin' and password 'Test@123'")
                                        .expectedResult("Credentials are accepted and form validation passes")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("THEN")
                                        .description("User clicks the 'Sign In' button")
                                        .expectedResult("User is redirected to dashboard and sees welcome message")
                                        .build()
                        ))
                        .build());
                log.info("Created test: {}", test1.getName());

                // Link to requirement
                traceabilityService.linkRequirementToTest(RequirementLinkRequest.builder()
                        .requirementKey("PROJ-001 - User Authentication")
                        .testId(test1.getId())
                        .build());

                // Create Test 2: Payment Processing
                TestResponse test2 = testService.createTest(CreateTestRequest.builder()
                        .projectId(projectId)
                        .name("Payment Processing Test")
                        .description("Test credit card payment flow including validation and confirmation")
                        .testType("AUTOMATED")
                        .priority("CRITICAL")
                        .labels(List.of("payment", "checkout", "regression"))
                        .requirementKeys(List.of("PROJ-002"))
                        .steps(List.of(
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("GIVEN")
                                        .description("User has items in shopping cart with total amount $199.99")
                                        .expectedResult("Cart summary shows items and total")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("WHEN")
                                        .description("User selects 'Credit Card' payment method")
                                        .expectedResult("Credit card form is displayed with card number, expiry, CVV fields")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("AND")
                                        .description("User enters valid card details: 4111111111111111, 12/28, 123")
                                        .expectedResult("Card validation passes")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("THEN")
                                        .description("User clicks 'Pay Now' button")
                                        .expectedResult("Payment is processed and success confirmation is shown")
                                        .build()
                        ))
                        .build());
                log.info("Created test: {}", test2.getName());

                // Link to requirement
                traceabilityService.linkRequirementToTest(RequirementLinkRequest.builder()
                        .requirementKey("PROJ-002 - Payment Processing")
                        .testId(test2.getId())
                        .build());

                // Create Test 3: Search Functionality
                TestResponse test3 = testService.createTest(CreateTestRequest.builder()
                        .projectId(projectId)
                        .name("Search with Filters Test")
                        .description("Verify search returns relevant results with proper filtering")
                        .testType("MANUAL")
                        .priority("MEDIUM")
                        .labels(List.of("search", "filters", "ui-test"))
                        .requirementKeys(List.of("PROJ-003"))
                        .steps(List.of(
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("GIVEN")
                                        .description("User is on the search page with 100+ items in database")
                                        .expectedResult("Search interface is loaded with search bar and filter options")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("WHEN")
                                        .description("User enters 'automation' in search box and selects 'Type: Test' filter")
                                        .expectedResult("Search results are filtered in real-time")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("THEN")
                                        .description("User views the filtered results")
                                        .expectedResult("Only items containing 'automation' and matching 'Type: Test' are shown")
                                        .build()
                        ))
                        .build());
                log.info("Created test: {}", test3.getName());

                // Link to requirement
                traceabilityService.linkRequirementToTest(RequirementLinkRequest.builder()
                        .requirementKey("PROJ-003 - Search & Discovery")
                        .testId(test3.getId())
                        .build());

                // Create Test 4: BDD Feature Test
                TestResponse test4 = testService.createTest(CreateTestRequest.builder()
                        .projectId(projectId)
                        .name("User Registration BDD Test")
                        .description("Verify user registration flow with all validation scenarios")
                        .testType("CUKE")
                        .priority("HIGH")
                        .labels(List.of("registration", "bdd", "validation"))
                        .requirementKeys(List.of("PROJ-001"))
                        .steps(List.of(
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("GIVEN")
                                        .description("I am on the registration page")
                                        .expectedResult("Registration form is displayed")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("WHEN")
                                        .description("I fill in valid details: email 'user@test.com', password 'Secure@123', confirm 'Secure@123'")
                                        .expectedResult("All fields validate successfully")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("AND")
                                        .description("I accept terms and conditions checkbox")
                                        .expectedResult("Checkbox is checked")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("THEN")
                                        .description("I click the 'Create Account' button")
                                        .expectedResult("Account is created and confirmation email is sent")
                                        .build()
                        ))
                        .build());
                log.info("Created test: {}", test4.getName());

                // Link to requirement
                traceabilityService.linkRequirementToTest(RequirementLinkRequest.builder()
                        .requirementKey("PROJ-001 - User Authentication")
                        .testId(test4.getId())
                        .build());

                // Create Test 5: Dashboard Verification
                TestResponse test5 = testService.createTest(CreateTestRequest.builder()
                        .projectId(projectId)
                        .name("Dashboard Statistics Test")
                        .description("Verify dashboard displays correct metrics and charts")
                        .testType("MANUAL")
                        .priority("LOW")
                        .labels(List.of("dashboard", "metrics", "charts"))
                        .requirementKeys(List.of("PROJ-004"))
                        .steps(List.of(
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("GIVEN")
                                        .description("User is logged in and has executed tests in the system")
                                        .expectedResult("Dashboard loads with user's historical data")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("WHEN")
                                        .description("User views the main dashboard page")
                                        .expectedResult("Statistics charts, pass rate, and recent activity are visible")
                                        .build(),
                                CreateTestRequest.TestStepDto.builder()
                                        .stepType("THEN")
                                        .description("User can see summary cards with total tests, pass rate, and trends")
                                        .expectedResult("All metrics are accurate and charts render correctly")
                                        .build()
                        ))
                        .build());
                log.info("Created test: {}", test5.getName());

                // Link to requirement
                traceabilityService.linkRequirementToTest(RequirementLinkRequest.builder()
                        .requirementKey("PROJ-004 - Dashboard Overview")
                        .testId(test5.getId())
                        .build());

                log.info("✅ Successfully seeded {} test cases with requirement links", 5);

            } catch (Exception e) {
                log.error("Error seeding test data: {}", e.getMessage(), e);
            }
        };
    }
}