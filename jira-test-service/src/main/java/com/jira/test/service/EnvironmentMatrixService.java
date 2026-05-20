package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.EnvironmentCombination;
import com.jira.test.entity.EnvironmentMatrix;
import com.jira.test.entity.EnvironmentProvisioningRule;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.repository.EnvironmentCombinationRepository;
import com.jira.test.repository.EnvironmentMatrixRepository;
import com.jira.test.repository.EnvironmentProvisioningRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            // Apply filters
            if (filters != null && !filters.isEmpty() && !passesFilters(combo, filters)) {
                continue;
            }

            // Check conflicts
            List<CombinationResponse.ValidationError> errors = checkConflicts(combo, conflicts);

            EnvironmentCombination combination = EnvironmentCombination.builder()
                    .matrixId(matrix.getId())
                    .combinationIndex(index++)
                    .combinationData(serializeMap(combo))
                    .isValid(errors.isEmpty())
                    .validationErrors(errors.isEmpty() ? null : serializeErrors(errors))
                    .provisioningStatus("PENDING")
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
        // Delete combinations first
        combinationRepository.deleteByMatrixId(matrixId);
        matrixRepository.deleteById(matrixId);
        log.info("Deleted matrix: {}", matrixId);
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
            Map<String, String> comboData = parseMap(combo.getCombinationData());
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
            // Auto-select based on provider type in combination
            List<EnvironmentProvisioningRule> activeRules = provisioningRuleRepository.findByIsActiveTrueOrderByPriorityDesc();
            if (!activeRules.isEmpty()) {
                rule = activeRules.get(0);
            }
        }

        if (rule == null) {
            // Return basic config without external provisioning
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
            // Build provisioned config from rule
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
                .provisioningError(combination.getProvisioningError())
                .status(combination.getProvisioningStatus())
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
                .maxConcurrent(request.getMaxConcurrent() != null ? request.getMaxConcurrent() : 5)
                .timeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 300)
                .retryCount(request.getRetryCount() != null ? request.getRetryCount() : 3)
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

        // Add provider-specific config
        Map<String, Object> providerConfig = parseMap(rule.getProviderConfig());
        Map<String, Object> capabilities = rule.getCapabilitiesTemplate() != null ?
                parseMap(rule.getCapabilitiesTemplate()) : new HashMap<>();

        Map<String, Object> result = new HashMap<>(baseConfig);
        result.putAll(providerConfig);
        result.putAll(capabilities);

        // Generate access URL based on provider
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
            Map<String, String> template = rule.getEnvironmentTemplate();
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
                .combinationData(parseMap(combo.getCombinationData()))
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
                        parseMap(rule.getEnvironmentTemplate()) : null)
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
}