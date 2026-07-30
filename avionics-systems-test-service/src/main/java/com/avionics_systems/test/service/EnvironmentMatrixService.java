package com.avionics_systems.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.EnvironmentCombination;
import com.avionics_systems.test.entity.EnvironmentMatrix;
import com.avionics_systems.test.entity.EnvironmentProvisioningRule;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.repository.EnvironmentCombinationRepository;
import com.avionics_systems.test.repository.EnvironmentMatrixRepository;
import com.avionics_systems.test.repository.EnvironmentProvisioningRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentMatrixService {

    private final EnvironmentMatrixRepository matrixRepository;
    private final EnvironmentCombinationRepository combinationRepository;
    private final EnvironmentProvisioningRuleRepository provisioningRuleRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.defaults.provisioning-status:PENDING}")
    private String defaultProvisioningStatus;

    @Value("${app.defaults.execution-estimated-duration:300}")
    private int defaultEstimatedDuration;

    @Value("${app.defaults.cloud-provider:AWS}")
    private String defaultCloudProvider;

    @Value("${app.defaults.provisioning.max-concurrent:5}")
    private int defaultMaxConcurrent;

    @Value("${app.defaults.provisioning.timeout-seconds:300}")
    private int defaultTimeoutSeconds;

    @Value("${app.defaults.provisioning.retry-count:3}")
    private int defaultRetryCount;

    @Value("${app.defaults.provisioning.max-parallel-tasks:10}")
    private int maxParallelTasks;

    // Cloud provider stubs
    private static final Map<String, CloudProviderConfig> CLOUD_PROVIDERS = new HashMap<>();
    static {
        CLOUD_PROVIDERS.put("AWS", new CloudProviderConfig("AWS", "Amazon Web Services", "https://aws.amazon.com/",
                Map.of("instanceTypes", List.of("t2.micro", "t2.medium", "t2.large", "m5.large", "m5.xlarge"),
                        "regions", List.of("us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1"),
                        "capabilities", List.of("EC2", "ECS", "EKS", "Lambda"))));

        CLOUD_PROVIDERS.put("AZURE", new CloudProviderConfig("AZURE", "Microsoft Azure", "https://azure.microsoft.com/",
                Map.of("instanceTypes", List.of("Standard_B1s", "Standard_B2s", "Standard_D2s_v3", "Standard_D4s_v3"),
                        "regions", List.of("eastus", "westus2", "westeurope", "southeastasia"),
                        "capabilities", List.of("VMs", "AKS", "Azure Functions", "App Service"))));

        CLOUD_PROVIDERS.put("GCP", new CloudProviderConfig("GCP", "Google Cloud Platform", "https://cloud.google.com/",
                Map.of("instanceTypes", List.of("e2-micro", "e2-medium", "e2-standard-2", "e2-standard-4"),
                        "regions", List.of("us-central1", "us-east1", "europe-west1", "asia-southeast1"),
                        "capabilities", List.of("Compute Engine", "GKE", "Cloud Functions", "App Engine"))));

        CLOUD_PROVIDERS.put("BROWSERSTACK", new CloudProviderConfig("BROWSERSTACK", "BrowserStack", "https://www.browserstack.com/",
                Map.of("browsers", List.of("Chrome", "Firefox", "Safari", "Edge"),
                        "os", List.of("Windows", "macOS", "Android", "iOS"),
                        "capabilities", List.of("Selenium", "Appium", "Playwright", "Cypress"))));

        CLOUD_PROVIDERS.put("SAUCELABS", new CloudProviderConfig("SAUCELABS", "Sauce Labs", "https://saucelabs.com/",
                Map.of("browsers", List.of("Chrome", "Firefox", "Safari", "Edge"),
                        "os", List.of("Windows", "macOS", "Linux"),
                        "capabilities", List.of("Selenium", "Appium", "Playwright", "Cypress"))));
    }

    // ==================== Matrix Configuration ====================

    @Transactional
    public MatrixConfigurationResponse createMatrix(MatrixConfigurationRequest request) {
        log.info("Creating environment matrix: {} with {} dimensions", request.getName(), request.getDimensions().size());

        EnvironmentMatrix matrix = EnvironmentMatrix.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .dimensionConfigs(serializeDimensions(request.getDimensions()))
                .filterRules(serializeFilterRules(request.getFilterRules()))
                .conflictRules(serializeConflictRules(request.getConflictRules()))
                .build();

        matrix = matrixRepository.save(matrix);

        // Generate combinations
        List<MatrixConfigurationRequest.DimensionConfig> dimensions = request.getDimensions();
        List<List<String>> valueLists = dimensions.stream()
                .map(MatrixConfigurationRequest.DimensionConfig::getValues)
                .collect(Collectors.toList());

        List<Map<String, String>> combinations = generateCartesianProduct(valueLists, dimensions);
        List<MatrixConfigurationRequest.FilterRule> filters = request.getFilterRules();
        List<MatrixConfigurationRequest.ConflictRule> conflicts = request.getConflictRules();

        int index = 0;
        int validCount = 0;
        for (Map<String, String> combo : combinations) {
            if (filters != null && !filters.isEmpty() && !passesFilters(combo, filters)) {
                continue;
            }

            List<CombinationResponse.ValidationError> errors = checkConflicts(combo, conflicts);

            EnvironmentCombination combination = EnvironmentCombination.builder()
                    .matrixId(matrix.getId())
                    .combinationIndex(index++)
                    .combinationData(serializeMap(combo))
                    .isValid(errors.isEmpty())
                    .validationErrors(errors.isEmpty() ? null : serializeErrors(errors))
                    .provisioningStatus(defaultProvisioningStatus)
                    .build();

            combinationRepository.save(combination);

            if (errors.isEmpty()) {
                validCount++;
            }
        }

        matrix.setTotalCombinations(index);
        matrix.setValidCombinations(validCount);
        matrix = matrixRepository.save(matrix);

        log.info("Created matrix {} with {} total combinations, {} valid", matrix.getId(), index, validCount);
        return mapToMatrixResponse(matrix);
    }

    @Transactional(readOnly = true)
    public List<MatrixConfigurationResponse> getMatrices(UUID projectId) {
        return matrixRepository.findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToMatrixResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatrixConfigurationResponse getMatrix(UUID matrixId) {
        EnvironmentMatrix matrix = matrixRepository.findById(matrixId)
                .orElseThrow(() -> new ResourceNotFoundException("EnvironmentMatrix", "id", matrixId));
        return mapToMatrixResponse(matrix);
    }

    @Transactional
    public void deleteMatrix(UUID matrixId) {
        combinationRepository.deleteByMatrixId(matrixId);
        matrixRepository.deleteById(matrixId);
        log.info("Deleted matrix: {}", matrixId);
    }

    // ==================== Matrix Visualization ====================

    @Transactional(readOnly = true)
    public MatrixVisualizationData getVisualizationData(UUID matrixId) {
        EnvironmentMatrix matrix = matrixRepository.findById(matrixId)
                .orElseThrow(() -> new ResourceNotFoundException("EnvironmentMatrix", "id", matrixId));

        List<EnvironmentCombination> combinations = combinationRepository.findByMatrixIdOrderByCombinationIndexAsc(matrixId);
        List<MatrixConfigurationRequest.DimensionConfig> dimensions = parseDimensions(matrix.getDimensionConfigs());

        // Generate heatmap data
        Map<String, Map<String, Double>> heatmapData = generateHeatmapData(combinations, dimensions);

        // Generate distribution stats
        Map<String, Map<String, Integer>> distributionStats = generateDistributionStats(combinations, dimensions);

        // Generate validity matrix
        List<List<Boolean>> validityMatrix = generateValidityMatrix(combinations, dimensions);

        return MatrixVisualizationData.builder()
                .matrixId(matrixId)
                .matrixName(matrix.getName())
                .dimensions(dimensions)
                .heatmapData(heatmapData)
                .distributionStats(distributionStats)
                .validityMatrix(validityMatrix)
                .totalCombinations(matrix.getTotalCombinations())
                .validCombinations(matrix.getValidCombinations())
                .invalidCombinations(matrix.getTotalCombinations() - matrix.getValidCombinations())
                .coveragePercentage(matrix.getTotalCombinations() > 0 ?
                        (matrix.getValidCombinations() * 100.0 / matrix.getTotalCombinations()) : 0.0)
                .build();
    }

    private Map<String, Map<String, Double>> generateHeatmapData(List<EnvironmentCombination> combinations,
                                                                   List<MatrixConfigurationRequest.DimensionConfig> dimensions) {
        Map<String, Map<String, Double>> heatmap = new HashMap<>();

        if (dimensions.isEmpty() || combinations.isEmpty()) return heatmap;

        // Use first two dimensions for heatmap
        String dimX = dimensions.get(0).getName();
        String dimY = dimensions.size() > 1 ? dimensions.get(1).getName() : null;

        for (EnvironmentCombination combo : combinations) {
            Map<String, String> data = parseStringMap(combo.getCombinationData());
            String xValue = data.get(dimX);

            if (xValue != null) {
                heatmap.computeIfAbsent(xValue, k -> new HashMap<>());

                if (dimY != null) {
                    String yValue = data.get(dimY);
                    if (yValue != null) {
                        // Score based on validity
                        double score = combo.getIsValid() ? 1.0 : 0.0;
                        // Add provisioning status influence
                        if ("PROVISIONED".equals(combo.getProvisioningStatus())) {
                            score += 0.5;
                        }
                        heatmap.get(xValue).merge(yValue, score, Double::sum);
                    }
                }
            }
        }

        return heatmap;
    }

    private Map<String, Map<String, Integer>> generateDistributionStats(List<EnvironmentCombination> combinations,
                                                                          List<MatrixConfigurationRequest.DimensionConfig> dimensions) {
        Map<String, Map<String, Integer>> stats = new HashMap<>();

        for (EnvironmentCombination combo : combinations) {
            Map<String, String> data = parseStringMap(combo.getCombinationData());

            for (MatrixConfigurationRequest.DimensionConfig dim : dimensions) {
                String value = data.get(dim.getName());
                if (value != null) {
                    stats.computeIfAbsent(dim.getName(), k -> new HashMap<>())
                            .merge(value, 1, Integer::sum);
                }
            }
        }

        return stats;
    }

    private List<List<Boolean>> generateValidityMatrix(List<EnvironmentCombination> combinations,
                                                       List<MatrixConfigurationRequest.DimensionConfig> dimensions) {
        List<List<Boolean>> matrix = new ArrayList<>();

        if (dimensions.size() < 2) return matrix;

        String dimX = dimensions.get(0).getName();
        String dimY = dimensions.get(1).getName();
        List<String> xValues = dimensions.get(0).getValues();
        List<String> yValues = dimensions.get(1).getValues();

        for (String xVal : xValues) {
            List<Boolean> row = new ArrayList<>();
            for (String yVal : yValues) {
                boolean valid = combinations.stream()
                        .anyMatch(c -> {
                            Map<String, String> data = parseStringMap(c.getCombinationData());
                            return xVal.equals(data.get(dimX)) && yVal.equals(data.get(dimY)) && c.getIsValid();
                        });
                row.add(valid);
            }
            matrix.add(row);
        }

        return matrix;
    }

    // ==================== Compatibility Checking ====================

    @Transactional(readOnly = true)
    public CompatibilityCheckResult checkCompatibility(UUID matrixId, Map<String, String> testRequirements) {
        EnvironmentMatrix matrix = matrixRepository.findById(matrixId)
                .orElseThrow(() -> new ResourceNotFoundException("EnvironmentMatrix", "id", matrixId));

        List<EnvironmentCombination> validCombinations = combinationRepository.findByMatrixIdAndIsValidTrue(matrixId);

        // Filter combinations that match requirements
        List<UUID> compatibleCombinations = validCombinations.stream()
                .filter(combo -> matchesRequirements(combo, testRequirements))
                .map(EnvironmentCombination::getId)
                .collect(Collectors.toList());

        // Check for issues
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (compatibleCombinations.isEmpty()) {
            errors.add("No compatible environment combinations found for the given requirements");
        }

        // Check for partial matches
        for (Map.Entry<String, String> req : testRequirements.entrySet()) {
            boolean found = validCombinations.stream()
                    .anyMatch(c -> {
                        Map<String, String> data = parseStringMap(c.getCombinationData());
                        return req.getValue().equals(data.get(req.getKey()));
                    });
            if (!found) {
                warnings.add("Requirement '" + req.getKey() + "=" + req.getValue() + "' has limited coverage");
            }
        }

        // Suggest alternatives
        List<Map<String, String>> suggestedAlternatives = suggestAlternatives(validCombinations, testRequirements);

        return CompatibilityCheckResult.builder()
                .isCompatible(!compatibleCombinations.isEmpty())
                .compatibleCount(compatibleCombinations.size())
                .compatibleCombinationIds(compatibleCombinations)
                .warnings(warnings)
                .errors(errors)
                .suggestedAlternatives(suggestedAlternatives)
                .build();
    }

    private boolean matchesRequirements(EnvironmentCombination combo, Map<String, String> requirements) {
        Map<String, String> data = parseStringMap(combo.getCombinationData());

        for (Map.Entry<String, String> req : requirements.entrySet()) {
            String value = data.get(req.getKey());
            if (value == null || !value.equals(req.getValue())) {
                return false;
            }
        }
        return true;
    }

    private List<Map<String, String>> suggestAlternatives(List<EnvironmentCombination> combinations,
                                                          Map<String, String> requirements) {
        List<Map<String, String>> suggestions = new ArrayList<>();

        // Find combinations that match most requirements
        for (EnvironmentCombination combo : combinations) {
            Map<String, String> data = parseStringMap(combo.getCombinationData());
            int matchCount = 0;

            for (Map.Entry<String, String> req : requirements.entrySet()) {
                if (req.getValue().equals(data.get(req.getKey()))) {
                    matchCount++;
                }
            }

            if (matchCount > 0 && matchCount < requirements.size()) {
                suggestions.add(data);
                if (suggestions.size() >= 5) break;
            }
        }

        return suggestions;
    }

    // ==================== Matrix-Based Test Execution ====================

    @Transactional(readOnly = true)
    public TestExecutionPlan generateExecutionPlan(UUID matrixId, UUID testId, List<UUID> testCaseIds) {
        List<EnvironmentCombination> validCombinations = combinationRepository.findByMatrixIdAndIsValidTrue(matrixId);

        // Generate execution plan
        List<ExecutionTask> tasks = new ArrayList<>();
        int priority = 1;

        for (EnvironmentCombination combo : validCombinations) {
            for (UUID tcId : testCaseIds) {
                ExecutionTask task = ExecutionTask.builder()
                        .taskId(UUID.randomUUID())
                        .combinationId(combo.getId())
                        .testCaseId(tcId)
                        .priority(priority++)
                        .environmentConfig(parseStringMap(combo.getCombinationData()))
                        .estimatedDuration(defaultEstimatedDuration)
                        .dependsOn(new ArrayList<>())
                        .build();
                tasks.add(task);
            }
        }

        // Optimize execution order (parallel where possible)
        List<List<ExecutionTask>> executionGroups = optimizeExecutionOrder(tasks);

        return TestExecutionPlan.builder()
                .planId(UUID.randomUUID())
                .matrixId(matrixId)
                .testId(testId)
                .totalCombinations(validCombinations.size())
                .totalTestCases(testCaseIds.size())
                .totalTasks(tasks.size())
                .estimatedDuration(calculateTotalDuration(executionGroups))
                .executionGroups(executionGroups)
                .canRunParallel(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<List<ExecutionTask>> optimizeExecutionOrder(List<ExecutionTask> tasks) {
        // Simple grouping - in production use more sophisticated scheduling
        List<List<ExecutionTask>> groups = new ArrayList<>();
        List<ExecutionTask> currentGroup = new ArrayList<>();

        for (ExecutionTask task : tasks) {
            currentGroup.add(task);
            if (currentGroup.size() >= maxParallelTasks) {
                groups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
            }
        }

        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        return groups;
    }

    private int calculateTotalDuration(List<List<ExecutionTask>> groups) {
        return groups.size() * defaultEstimatedDuration;
    }

    // ==================== Cloud Provider Integration ====================

    @Transactional(readOnly = true)
    public CloudProviderInfo getCloudProviders() {
        Map<String, CloudProviderDetails> providers = new HashMap<>();

        for (Map.Entry<String, CloudProviderConfig> entry : CLOUD_PROVIDERS.entrySet()) {
            CloudProviderConfig config = entry.getValue();
            providers.put(entry.getKey(), CloudProviderDetails.builder()
                    .providerType(entry.getKey())
                    .displayName(config.name)
                    .website(config.website)
                    .capabilities(config.capabilities.get("capabilities"))
                    .availableRegions(config.capabilities.get("regions"))
                    .availableInstanceTypes(config.capabilities.get("instanceTypes"))
                    .isAvailable(true)
                    .build());
        }

        return CloudProviderInfo.builder()
                .providers(providers)
                .defaultProvider(defaultCloudProvider)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProviderCapabilities(String providerType) {
        CloudProviderConfig config = CLOUD_PROVIDERS.get(providerType.toUpperCase());
        if (config == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerType);
        }

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("providerType", providerType);
        capabilities.put("displayName", config.name);
        capabilities.put("capabilities", config.capabilities);

        // Add API endpoint stub
        capabilities.put("apiEndpoint", getApiEndpointStub(providerType));

        // Add credential requirements
        capabilities.put("requiresCredentials", true);
        capabilities.put("credentialFields", List.of("accessKey", "secretKey", "region"));

        return capabilities;
    }

    private String getApiEndpointStub(String providerType) {
        return switch (providerType.toUpperCase()) {
            case "AWS" -> "https://sts.amazonaws.com/";
            case "AZURE" -> "https://login.microsoftonline.com/";
            case "GCP" -> "https://oauth2.googleapis.com/";
            case "BROWSERSTACK" -> "https://api.browserstack.com/";
            case "SAUCELABS" -> "https://api.saucelabs.com/";
            default -> "https://api.example.com/";
        };
    }

    // ==================== Combination Operations ====================

    @Transactional(readOnly = true)
    public List<CombinationResponse> getCombinations(UUID matrixId) {
        return combinationRepository.findByMatrixIdOrderByCombinationIndexAsc(matrixId).stream()
                .map(this::mapToCombinationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CombinationResponse> getValidCombinations(UUID matrixId) {
        return combinationRepository.findByMatrixIdAndIsValidTrue(matrixId).stream()
                .map(this::mapToCombinationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CombinationResponse> validateCombinations(UUID matrixId) {
        EnvironmentMatrix matrix = matrixRepository.findById(matrixId)
                .orElseThrow(() -> new ResourceNotFoundException("EnvironmentMatrix", "id", matrixId));

        List<MatrixConfigurationRequest.ConflictRule> conflicts = parseConflictRules(matrix.getConflictRules());

        List<EnvironmentCombination> combinations = combinationRepository.findByMatrixIdOrderByCombinationIndexAsc(matrixId);
        int validCount = 0;

        for (EnvironmentCombination combo : combinations) {
            Map<String, String> comboData = parseStringMap(combo.getCombinationData());
            List<CombinationResponse.ValidationError> errors = checkConflicts(comboData, conflicts);

            combo.setIsValid(errors.isEmpty());
            combo.setValidationErrors(errors.isEmpty() ? null : serializeErrors(errors));
            combinationRepository.save(combo);

            if (errors.isEmpty()) validCount++;
        }

        matrix.setValidCombinations(validCount);
        matrix.setInvalidCombinations(combinations.size() - validCount);
        matrixRepository.save(matrix);

        return getCombinations(matrixId);
    }

    // ==================== Provisioning Operations ====================

    @Transactional
    public ProvisionResponse provisionEnvironment(EnvironmentProvisionRequest request) {
        log.info("Provisioning environment for combination: {}", request.getCombinationId());

        EnvironmentCombination combination = combinationRepository.findById(request.getCombinationId())
                .orElseThrow(() -> new ResourceNotFoundException("EnvironmentCombination", "id", request.getCombinationId()));

        EnvironmentProvisioningRule rule = null;
        if (request.getProvisioningRuleId() != null) {
            rule = provisioningRuleRepository.findById(request.getProvisioningRuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProvisioningRule", "id", request.getProvisioningRuleId()));
        } else {
            List<EnvironmentProvisioningRule> activeRules = provisioningRuleRepository.findByIsActiveTrueOrderByPriorityDesc();
            if (!activeRules.isEmpty()) {
                rule = activeRules.get(0);
            }
        }

        if (rule == null) {
            Map<String, Object> basicConfig = parseMap(combination.getCombinationData());
            combination.setProvisionedConfig(serializeMap(basicConfig));
            combination.setProvisioningStatus("PROVISIONED");
            combination.setProvisionedAt(LocalDateTime.now());
            combinationRepository.save(combination);

            return ProvisionResponse.builder()
                    .combinationId(combination.getId())
                    .providerType("LOCAL")
                    .provisionedConfig(basicConfig)
                    .provisioningStatus("PROVISIONED")
                    .provisionedAt(LocalDateTime.now())
                    .status("SUCCESS")
                    .build();
        }

        try {
            Map<String, Object> provisionedConfig = buildProvisionedConfig(combination, rule);

            combination.setProvisionedConfig(serializeMap(provisionedConfig));
            combination.setProvisioningStatus("PROVISIONED");
            combination.setProvisionedAt(LocalDateTime.now());
            combination.setProvisioningError(null);
            combinationRepository.save(combination);

            Map<String, String> envVars = buildEnvironmentVariables(provisionedConfig, rule);

            return ProvisionResponse.builder()
                    .combinationId(combination.getId())
                    .provisioningRuleId(rule.getId())
                    .providerType(rule.getProviderType())
                    .provisionedConfig(provisionedConfig)
                    .environmentVariables(envVars)
                    .accessUrl((String) provisionedConfig.get("accessUrl"))
                    .provisionedAt(LocalDateTime.now())
                    .status("SUCCESS")
                    .build();

        } catch (Exception e) {
            combination.setProvisioningStatus("FAILED");
            combination.setProvisioningError(e.getMessage());
            combinationRepository.save(combination);

            return ProvisionResponse.builder()
                    .combinationId(combination.getId())
                    .provisioningRuleId(rule != null ? rule.getId() : null)
                    .providerType(rule != null ? rule.getProviderType() : "UNKNOWN")
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public ProvisionResponse getProvisionedEnvironment(UUID combinationId) {
        EnvironmentCombination combination = combinationRepository.findById(combinationId)
                .orElseThrow(() -> new ResourceNotFoundException("EnvironmentCombination", "id", combinationId));

        Map<String, Object> provisionedConfig = combination.getProvisionedConfig() != null ?
                parseMap(combination.getProvisionedConfig()) : Map.of();

        Map<String, String> envVars = new HashMap<>();
        provisionedConfig.forEach((k, v) -> envVars.put(k, String.valueOf(v)));

        return ProvisionResponse.builder()
                .combinationId(combination.getId())
                .provisionedConfig(provisionedConfig)
                .environmentVariables(envVars)
                .provisioningStatus(combination.getProvisioningStatus())
                .provisionedAt(combination.getProvisionedAt())
                .errorMessage(combination.getProvisioningError())
                .status(combination.getProvisioningStatus())
                .build();
    }

    // ==================== Provisioning Workflow ====================

    @Transactional
    public ProvisioningWorkflowResponse startProvisioningWorkflow(UUID matrixId, List<UUID> combinationIds) {
        List<EnvironmentCombination> combinations = combinationIds.isEmpty() ?
                combinationRepository.findByMatrixIdAndIsValidTrue(matrixId) :
                combinationRepository.findAllById(combinationIds);

        List<ProvisioningTask> tasks = new ArrayList<>();

        for (EnvironmentCombination combo : combinations) {
            ProvisioningTask task = ProvisioningTask.builder()
                    .taskId(UUID.randomUUID())
                    .combinationId(combo.getId())
                    .status("PENDING")
                    .startedAt(null)
                    .completedAt(null)
                    .build();
            tasks.add(task);
        }

        // Start async provisioning (simplified)
        for (ProvisioningTask task : tasks) {
            try {
                EnvironmentProvisionRequest request = EnvironmentProvisionRequest.builder()
                        .combinationId(task.getCombinationId())
                        .build();
                ProvisionResponse response = provisionEnvironment(request);
                task.setStatus(response.getStatus());
            } catch (Exception e) {
                task.setStatus("FAILED");
                task.setError(e.getMessage());
            }
        }

        int successCount = (int) tasks.stream().filter(t -> "PROVISIONED".equals(t.getStatus())).count();
        int failedCount = (int) tasks.stream().filter(t -> "FAILED".equals(t.getStatus())).count();

        return ProvisioningWorkflowResponse.builder()
                .workflowId(UUID.randomUUID())
                .matrixId(matrixId)
                .totalTasks(tasks.size())
                .pendingTasks((int) tasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count())
                .inProgressTasks((int) tasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count())
                .completedTasks(successCount)
                .failedTasks(failedCount)
                .tasks(tasks)
                .build();
    }

    @Transactional(readOnly = true)
    public ProvisioningWorkflowStatus getWorkflowStatus(UUID workflowId) {
        // In production, track workflow state in database
        return ProvisioningWorkflowStatus.builder()
                .workflowId(workflowId)
                .status("IN_PROGRESS")
                .progressPercentage(50)
                .build();
    }

    // ==================== Provisioning Rules ====================

    @Transactional
    public ProvisioningRuleResponse createProvisioningRule(ProvisioningRuleRequest request) {
        EnvironmentProvisioningRule rule = EnvironmentProvisioningRule.builder()
                .projectId(request.getProjectId())
                .ruleName(request.getRuleName())
                .description(request.getDescription())
                .providerType(request.getProviderType())
                .providerConfig(serializeMap(request.getProviderConfig()))
                .provisioningScript(request.getProvisioningScript())
                .capabilitiesTemplate(serializeMap(request.getCapabilitiesTemplate()))
                .environmentTemplate(request.getEnvironmentTemplate() != null ?
                        serializeMap(request.getEnvironmentTemplate()) : null)
                .maxConcurrent(request.getMaxConcurrent() != null ? request.getMaxConcurrent() : defaultMaxConcurrent)
                .timeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : defaultTimeoutSeconds)
                .retryCount(request.getRetryCount() != null ? request.getRetryCount() : defaultRetryCount)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .createdBy(request.getCreatedBy())
                .build();

        rule = provisioningRuleRepository.save(rule);
        log.info("Created provisioning rule: {} for provider {}", rule.getId(), rule.getProviderType());
        return mapToRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<ProvisioningRuleResponse> getProvisioningRules(UUID projectId) {
        if (projectId != null) {
            return provisioningRuleRepository.findByProjectIdAndIsActiveTrue(projectId).stream()
                    .map(this::mapToRuleResponse)
                    .collect(Collectors.toList());
        }
        return provisioningRuleRepository.findByIsActiveTrueOrderByPriorityDesc().stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProvisioningRuleResponse getProvisioningRule(UUID ruleId) {
        EnvironmentProvisioningRule rule = provisioningRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("ProvisioningRule", "id", ruleId));
        return mapToRuleResponse(rule);
    }

    @Transactional
    public void deleteProvisioningRule(UUID ruleId) {
        provisioningRuleRepository.deleteById(ruleId);
        log.info("Deleted provisioning rule: {}", ruleId);
    }

    // ==================== Helper Methods ====================

    private List<Map<String, String>> generateCartesianProduct(List<List<String>> valueLists,
                                                                 List<MatrixConfigurationRequest.DimensionConfig> dimensions) {
        List<Map<String, String>> result = new ArrayList<>();
        generateCartesianRecursive(valueLists, dimensions, 0, new HashMap<>(), result);
        return result;
    }

    private void generateCartesianRecursive(List<List<String>> valueLists,
                                            List<MatrixConfigurationRequest.DimensionConfig> dimensions,
                                            int index, Map<String, String> current, List<Map<String, String>> result) {
        if (index == valueLists.size()) {
            result.add(new HashMap<>(current));
            return;
        }

        String dimensionName = dimensions.get(index).getName();
        for (String value : valueLists.get(index)) {
            current.put(dimensionName, value);
            generateCartesianRecursive(valueLists, dimensions, index + 1, current, result);
            current.remove(dimensionName);
        }
    }

    private boolean passesFilters(Map<String, String> combination, List<MatrixConfigurationRequest.FilterRule> filters) {
        for (MatrixConfigurationRequest.FilterRule filter : filters) {
            String dimValue = combination.get(filter.getDimension());
            if (dimValue == null) continue;

            if ("INCLUDE".equals(filter.getType()) && !filter.getValues().contains(dimValue)) {
                return false;
            }
            if ("EXCLUDE".equals(filter.getType()) && filter.getValues().contains(dimValue)) {
                return false;
            }
        }
        return true;
    }

    private List<CombinationResponse.ValidationError> checkConflicts(Map<String, String> combination,
                                                                       List<MatrixConfigurationRequest.ConflictRule> conflicts) {
        List<CombinationResponse.ValidationError> errors = new ArrayList<>();
        if (conflicts == null || conflicts.isEmpty()) return errors;

        for (MatrixConfigurationRequest.ConflictRule conflict : conflicts) {
            Map<String, List<String>> conflictMap = conflict.getConflicts();
            if (conflictMap == null || conflictMap.isEmpty()) continue;

            for (Map.Entry<String, List<String>> entry : conflictMap.entrySet()) {
                String dim = entry.getKey();
                List<String> invalidValues = entry.getValue();

                String value = combination.get(dim);
                if (value != null && invalidValues.contains(value)) {
                    errors.add(CombinationResponse.ValidationError.builder()
                            .rule(conflict.getRuleName())
                            .details("Value '" + value + "' for '" + dim + "' is incompatible")
                            .affectedDimensions(dim)
                            .build());
                }
            }
        }
        return errors;
    }

    private Map<String, Object> buildProvisionedConfig(EnvironmentCombination combination,
                                                        EnvironmentProvisioningRule rule) {
        Map<String, Object> baseConfig = parseMap(combination.getCombinationData());

        Map<String, Object> providerConfig = parseMap(rule.getProviderConfig());
        Map<String, Object> capabilities = rule.getCapabilitiesTemplate() != null ?
                parseMap(rule.getCapabilitiesTemplate()) : new HashMap<>();

        Map<String, Object> result = new HashMap<>(baseConfig);
        result.putAll(providerConfig);
        result.putAll(capabilities);

        String accessUrl = generateAccessUrl(rule.getProviderType(), baseConfig);
        result.put("accessUrl", accessUrl);

        return result;
    }

    private String generateAccessUrl(String providerType, Map<String, Object> config) {
        return switch (providerType) {
            case "BROWSERSTACK" -> "https://hub.browserstack.com/wd/hub";
            case "SAUCELABS" -> "https://ondemand.saucelabs.com/wd/hub";
            case "KUBERNETES" -> "https://k8s.example.com/api/v1/namespaces/test/pods";
            case "DOCKER" -> "tcp://localhost:2375";
            default -> "http://localhost:8080";
        };
    }

    private Map<String, String> buildEnvironmentVariables(Map<String, Object> config, EnvironmentProvisioningRule rule) {
        Map<String, String> envVars = new HashMap<>();
        config.forEach((k, v) -> envVars.put("ENV_" + k.toUpperCase(), String.valueOf(v)));

        if (rule.getEnvironmentTemplate() != null) {
            Map<String, String> template = parseStringMap(rule.getEnvironmentTemplate());
            template.forEach(envVars::put);
        }

        return envVars;
    }

    private MatrixConfigurationResponse mapToMatrixResponse(EnvironmentMatrix matrix) {
        return MatrixConfigurationResponse.builder()
                .id(matrix.getId())
                .projectId(matrix.getProjectId())
                .name(matrix.getName())
                .description(matrix.getDescription())
                .dimensions(parseDimensions(matrix.getDimensionConfigs()))
                .filterRules(parseFilterRules(matrix.getFilterRules()))
                .conflictRules(parseConflictRules(matrix.getConflictRules()))
                .totalCombinations(matrix.getTotalCombinations())
                .validCombinations(matrix.getValidCombinations())
                .invalidCombinations(matrix.getTotalCombinations() - matrix.getValidCombinations())
                .isActive(matrix.getIsActive())
                .createdBy(matrix.getCreatedBy())
                .createdAt(matrix.getCreatedAt())
                .updatedAt(matrix.getUpdatedAt())
                .build();
    }

    private CombinationResponse mapToCombinationResponse(EnvironmentCombination combo) {
        List<CombinationResponse.ValidationError> errors = null;
        if (combo.getValidationErrors() != null) {
            errors = parseErrors(combo.getValidationErrors());
        }

        return CombinationResponse.builder()
                .id(combo.getId())
                .matrixId(combo.getMatrixId())
                .combinationIndex(combo.getCombinationIndex())
                .combinationData(parseStringMap(combo.getCombinationData()))
                .isValid(combo.getIsValid())
                .validationErrors(errors)
                .provisionedConfig(combo.getProvisionedConfig() != null ? parseMap(combo.getProvisionedConfig()) : null)
                .provisioningStatus(combo.getProvisioningStatus())
                .provisionedAt(combo.getProvisionedAt())
                .provisioningError(combo.getProvisioningError())
                .createdAt(combo.getCreatedAt())
                .build();
    }

    private ProvisioningRuleResponse mapToRuleResponse(EnvironmentProvisioningRule rule) {
        return ProvisioningRuleResponse.builder()
                .id(rule.getId())
                .projectId(rule.getProjectId())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .providerType(rule.getProviderType())
                .providerConfig(parseMap(rule.getProviderConfig()))
                .provisioningScript(rule.getProvisioningScript())
                .capabilitiesTemplate(rule.getCapabilitiesTemplate() != null ?
                        parseMap(rule.getCapabilitiesTemplate()) : null)
                .environmentTemplate(rule.getEnvironmentTemplate() != null ?
                        parseStringMap(rule.getEnvironmentTemplate()) : null)
                .maxConcurrent(rule.getMaxConcurrent())
                .timeoutSeconds(rule.getTimeoutSeconds())
                .retryCount(rule.getRetryCount())
                .priority(rule.getPriority())
                .isActive(rule.getIsActive())
                .createdBy(rule.getCreatedBy())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    // Serialization helpers
    private String serializeDimensions(List<MatrixConfigurationRequest.DimensionConfig> dimensions) {
        try { return objectMapper.writeValueAsString(dimensions); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private List<MatrixConfigurationRequest.DimensionConfig> parseDimensions(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<List<MatrixConfigurationRequest.DimensionConfig>>() {}); }
        catch (JsonProcessingException e) { return new ArrayList<>(); }
    }

    private String serializeFilterRules(List<MatrixConfigurationRequest.FilterRule> rules) {
        if (rules == null) return null;
        try { return objectMapper.writeValueAsString(rules); }
        catch (JsonProcessingException e) { return null; }
    }

    private List<MatrixConfigurationRequest.FilterRule> parseFilterRules(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<List<MatrixConfigurationRequest.FilterRule>>() {}); }
        catch (JsonProcessingException e) { return new ArrayList<>(); }
    }

    private String serializeConflictRules(List<MatrixConfigurationRequest.ConflictRule> rules) {
        if (rules == null) return null;
        try { return objectMapper.writeValueAsString(rules); }
        catch (JsonProcessingException e) { return null; }
    }

    private List<MatrixConfigurationRequest.ConflictRule> parseConflictRules(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<List<MatrixConfigurationRequest.ConflictRule>>() {}); }
        catch (JsonProcessingException e) { return new ArrayList<>(); }
    }

    private String serializeMap(Map<String, ?> map) {
        try { return objectMapper.writeValueAsString(map); }
        catch (JsonProcessingException e) { return "{}"; }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try { return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {}); }
        catch (JsonProcessingException e) { return new HashMap<>(); }
    }

    private String serializeErrors(List<CombinationResponse.ValidationError> errors) {
        try { return objectMapper.writeValueAsString(errors); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private List<CombinationResponse.ValidationError> parseErrors(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<List<CombinationResponse.ValidationError>>() {}); }
        catch (JsonProcessingException e) { return new ArrayList<>(); }
    }

    private Map<String, String> parseStringMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            Map<String, Object> objMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, String> result = new HashMap<>();
            objMap.forEach((k, v) -> result.put(k, v != null ? String.valueOf(v) : ""));
            return result;
        }
        catch (JsonProcessingException e) { return new HashMap<>(); }
    }

    // ==================== Inner Classes ====================

    @lombok.Data
    @lombok.Builder
    public static class MatrixVisualizationData {
        private UUID matrixId;
        private String matrixName;
        private List<MatrixConfigurationRequest.DimensionConfig> dimensions;
        private Map<String, Map<String, Double>> heatmapData;
        private Map<String, Map<String, Integer>> distributionStats;
        private List<List<Boolean>> validityMatrix;
        private int totalCombinations;
        private int validCombinations;
        private int invalidCombinations;
        private double coveragePercentage;
    }

    @lombok.Data
    @lombok.Builder
    public static class CompatibilityCheckResult {
        private boolean isCompatible;
        private int compatibleCount;
        private List<UUID> compatibleCombinationIds;
        private List<String> warnings;
        private List<String> errors;
        private List<Map<String, String>> suggestedAlternatives;
    }

    @lombok.Data
    @lombok.Builder
    public static class TestExecutionPlan {
        private UUID planId;
        private UUID matrixId;
        private UUID testId;
        private int totalCombinations;
        private int totalTestCases;
        private int totalTasks;
        private int estimatedDuration;
        private List<List<ExecutionTask>> executionGroups;
        private boolean canRunParallel;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class ExecutionTask {
        private UUID taskId;
        private UUID combinationId;
        private UUID testCaseId;
        private int priority;
        private Map<String, String> environmentConfig;
        private int estimatedDuration;
        private List<UUID> dependsOn;
    }

    @lombok.Data
    @lombok.Builder
    public static class CloudProviderConfig {
        private String code;
        private String name;
        private String website;
        private Map<String, List<String>> capabilities;
    }

    @lombok.Data
    @lombok.Builder
    public static class CloudProviderInfo {
        private Map<String, CloudProviderDetails> providers;
        private String defaultProvider;
    }

    @lombok.Data
    @lombok.Builder
    public static class CloudProviderDetails {
        private String providerType;
        private String displayName;
        private String website;
        private List<String> capabilities;
        private List<String> availableRegions;
        private List<String> availableInstanceTypes;
        private boolean isAvailable;
    }

    @lombok.Data
    @lombok.Builder
    public static class ProvisioningWorkflowResponse {
        private UUID workflowId;
        private UUID matrixId;
        private int totalTasks;
        private int pendingTasks;
        private int inProgressTasks;
        private int completedTasks;
        private int failedTasks;
        private List<ProvisioningTask> tasks;
    }

    @lombok.Data
    @lombok.Builder
    public static class ProvisioningTask {
        private UUID taskId;
        private UUID combinationId;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    public static class ProvisioningWorkflowStatus {
        private UUID workflowId;
        private String status;
        private int progressPercentage;
    }
}