package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetService {

    private final TestDatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;
    private final DatasetVariableRepository variableRepository;
    private final TestDatasetBindingRepository bindingRepository;
    private final TestIssueRepository testIssueRepository;
    private final ObjectMapper objectMapper;

    // ==================== Dataset CRUD ====================

    @Transactional
    public DatasetResponse createDataset(CreateDatasetRequest request) {
        log.info("Creating dataset: {} for project: {}", request.getName(), request.getProjectId());

        if (datasetRepository.existsByProjectIdAndNameAndArchivedFalse(request.getProjectId(), request.getName())) {
            throw new DuplicateResourceException("Dataset with name '" + request.getName() + "' already exists in this project");
        }

        List<List<String>> rows = request.getRows();
        String dataJson = null;

        if (rows != null && !rows.isEmpty()) {
            try {
                dataJson = objectMapper.writeValueAsString(rows);
            } catch (JsonProcessingException e) {
                throw new ValidationException("Invalid data format: " + e.getMessage());
            }
        } else if (request.getCsvData() != null) {
            dataJson = parseCsvToJson(request.getCsvData(), request.getColumnNames());
        } else if (request.getJsonData() != null) {
            dataJson = request.getJsonData();
            rows = parseJsonToRows(request.getJsonData());
        }

        TestDataset dataset = TestDataset.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .dataFormat(request.getDataFormat() != null ? request.getDataFormat() : "TABULAR")
                .columnNames(request.getColumnNames())
                .columnTypes(request.getColumnTypes())
                .data(dataJson)
                .csvData(request.getCsvData())
                .rowCount(rows != null ? rows.size() : 0)
                .folderId(request.getFolderId())
                .version(1)
                .isImmutable(false)
                .build();

        dataset = datasetRepository.save(dataset);

        // Create initial version
        if (dataJson != null) {
            createVersionSnapshot(dataset.getId(), "Initial version", null);
        }

        log.info("Dataset created with id: {}", dataset.getId());
        return mapToDatasetResponse(dataset);
    }

    @Transactional(readOnly = true)
    public DatasetResponse getDataset(UUID datasetId) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        return mapToDatasetResponse(dataset);
    }

    @Transactional(readOnly = true)
    public List<DatasetResponse> getDatasetsByProject(UUID projectId) {
        return datasetRepository.findByProjectIdAndArchivedFalse(projectId).stream()
                .map(this::mapToDatasetResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DatasetResponse> searchDatasets(UUID projectId, String searchTerm) {
        return datasetRepository.findByProjectIdAndNameContainingIgnoreCaseAndArchivedFalse(projectId, searchTerm).stream()
                .map(this::mapToDatasetResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DatasetResponse updateDataset(UUID datasetId, UpdateDatasetRequest request) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        if (dataset.getIsImmutable()) {
            throw new InvalidOperationException("Cannot update immutable dataset. Create a new version instead.");
        }

        boolean hasDataChanges = false;

        if (request.getName() != null) dataset.setName(request.getName());
        if (request.getDescription() != null) dataset.setDescription(request.getDescription());
        if (request.getIsImmutable() != null) dataset.setIsImmutable(request.getIsImmutable());

        if (request.getColumnNames() != null) {
            dataset.setColumnNames(request.getColumnNames());
            hasDataChanges = true;
        }
        if (request.getColumnTypes() != null) {
            dataset.setColumnTypes(request.getColumnTypes());
        }
        if (request.getRows() != null) {
            try {
                dataset.setData(objectMapper.writeValueAsString(request.getRows()));
                dataset.setRowCount(request.getRows().size());
                hasDataChanges = true;
            } catch (JsonProcessingException e) {
                throw new ValidationException("Invalid data format: " + e.getMessage());
            }
        }
        if (request.getCsvData() != null) {
            dataset.setCsvData(request.getCsvData());
            dataset.setData(parseCsvToJson(request.getCsvData(), request.getColumnNames()));
            hasDataChanges = true;
        }
        if (request.getJsonData() != null) {
            dataset.setData(request.getJsonData());
            List<List<String>> rows = parseJsonToRows(request.getJsonData());
            dataset.setRowCount(rows != null ? rows.size() : 0);
            hasDataChanges = true;
        }

        dataset = datasetRepository.save(dataset);

        if (hasDataChanges) {
            createVersionSnapshot(dataset.getId(), "Data updated", null);
        }

        log.info("Dataset updated: {}", datasetId);
        return mapToDatasetResponse(dataset);
    }

    @Transactional
    public void deleteDataset(UUID datasetId) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        // Check if dataset is bound to any tests
        List<TestDatasetBinding> bindings = bindingRepository.findByDatasetId(datasetId);
        if (!bindings.isEmpty()) {
            throw new InvalidOperationException("Cannot delete dataset that is bound to tests. Unbind first.");
        }

        dataset.setArchived(true);
        datasetRepository.save(dataset);
        log.info("Dataset archived: {}", datasetId);
    }

    // ==================== Versioning ====================

    @Transactional
    public DatasetVersionResponse createVersion(UUID datasetId, String changeSummary, UUID createdBy) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        return createVersionSnapshot(datasetId, changeSummary, createdBy);
    }

    private DatasetVersionResponse createVersionSnapshot(UUID datasetId, String changeSummary, UUID createdBy) {
        TestDataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        int newVersionNum = versionRepository.findMaxVersionByDatasetId(datasetId).orElse(0) + 1;

        DatasetVersion version = DatasetVersion.builder()
                .datasetId(datasetId)
                .versionNumber(newVersionNum)
                .data(dataset.getData())
                .columnNames(dataset.getColumnNames() != null ? dataset.getColumnNames().toArray(new String[0]) : null)
                .columnTypes(dataset.getColumnTypes() != null ? dataset.getColumnTypes().toArray(new String[0]) : null)
                .rowCount(dataset.getRowCount())
                .changeSummary(changeSummary)
                .createdBy(createdBy)
                .isImmutable(true)
                .build();

        version = versionRepository.save(version);

        dataset.setVersion(newVersionNum);
        datasetRepository.save(dataset);

        return mapToVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public List<DatasetVersionResponse> getVersions(UUID datasetId) {
        return versionRepository.findByDatasetIdOrderByVersionNumberDesc(datasetId).stream()
                .map(this::mapToVersionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DatasetVersionResponse getVersion(UUID datasetId, Integer versionNumber) {
        DatasetVersion version = versionRepository.findByDatasetIdAndVersionNumber(datasetId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("DatasetVersion",
                        "datasetId=" + datasetId + " and version=" + versionNumber, null));
        return mapToVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public DatasetResponse getImmutableSnapshot(UUID datasetId, UUID executionId) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        DatasetVersion snapshot = versionRepository.findByDatasetIdAndIsImmutableTrue(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("No immutable snapshot found for dataset", "id", datasetId));

        return DatasetResponse.builder()
                .id(dataset.getId())
                .projectId(dataset.getProjectId())
                .name(dataset.getName())
                .description(dataset.getDescription())
                .dataFormat(dataset.getDataFormat())
                .columnNames(snapshot.getColumnNames() != null ? Arrays.asList(snapshot.getColumnNames()) : null)
                .columnTypes(snapshot.getColumnTypes() != null ? Arrays.asList(snapshot.getColumnTypes()) : null)
                .rows(parseJsonToRows(snapshot.getData()))
                .rowCount(snapshot.getRowCount())
                .version(snapshot.getVersionNumber())
                .isImmutable(true)
                .createdAt(dataset.getCreatedAt())
                .updatedAt(dataset.getUpdatedAt())
                .build();
    }

    // ==================== Import/Export ====================

    @Transactional
    public DatasetResponse importFromCSV(UUID projectId, String csvData, String fileName) {
        CreateDatasetRequest request = CreateDatasetRequest.builder()
                .projectId(projectId)
                .name(fileName != null ? fileName.replace(".csv", "") : "Imported Dataset")
                .dataFormat("CSV")
                .csvData(csvData)
                .build();

        return createDataset(request);
    }

    @Transactional
    public DatasetResponse importFromJSON(UUID projectId, String jsonData, String name) {
        CreateDatasetRequest request = CreateDatasetRequest.builder()
                .projectId(projectId)
                .name(name != null ? name : "Imported Dataset")
                .dataFormat("JSON")
                .jsonData(jsonData)
                .build();

        return createDataset(request);
    }

    public String exportToCSV(UUID datasetId) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        List<List<String>> rows = parseJsonToRows(dataset.getData());
        StringBuilder csv = new StringBuilder();

        if (dataset.getColumnNames() != null && !dataset.getColumnNames().isEmpty()) {
            csv.append(String.join(",", dataset.getColumnNames())).append("\n");
        }

        if (rows != null) {
            for (List<String> row : rows) {
                csv.append(String.join(",", row)).append("\n");
            }
        }

        return csv.toString();
    }

    public String exportToJSON(UUID datasetId) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        return dataset.getData();
    }

    // ==================== Binding ====================

    @Transactional
    public DatasetBindingResponse bindToTest(BindDatasetRequest request) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(request.getDatasetId())
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", request.getDatasetId()));

        TestIssue test = testIssueRepository.findById(request.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", request.getTestId()));

        if (bindingRepository.findByTestIdAndDatasetId(request.getTestId(), request.getDatasetId()).isPresent()) {
            throw new DuplicateResourceException("Dataset already bound to this test");
        }

        TestDatasetBinding binding = TestDatasetBinding.builder()
                .testId(request.getTestId())
                .datasetId(request.getDatasetId())
                .datasetVersionId(request.getDatasetVersionId())
                .columnMappings(request.getColumnMappings() != null ?
                        mapToJson(request.getColumnMappings()) : createDefaultMappings(dataset))
                .createdBy(null)
                .build();

        binding = bindingRepository.save(binding);
        log.info("Dataset {} bound to test {}", request.getDatasetId(), request.getTestId());

        return mapToBindingResponse(binding, dataset, test);
    }

    @Transactional
    public void unbindFromTest(UUID testId, UUID datasetId) {
        bindingRepository.deleteByTestIdAndDatasetId(testId, datasetId);
        log.info("Dataset {} unbound from test {}", datasetId, testId);
    }

    @Transactional(readOnly = true)
    public List<DatasetBindingResponse> getBindingsForTest(UUID testId) {
        List<TestDatasetBinding> bindings = bindingRepository.findByTestId(testId);
        return bindings.stream().map(binding -> {
            TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(binding.getDatasetId()).orElse(null);
            TestIssue test = testIssueRepository.findById(testId).orElse(null);
            return mapToBindingResponse(binding, dataset, test);
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DatasetBindingResponse> getTestsBoundToDataset(UUID datasetId) {
        List<TestDatasetBinding> bindings = bindingRepository.findByDatasetId(datasetId);
        return bindings.stream().map(binding -> {
            TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId).orElse(null);
            TestIssue test = testIssueRepository.findById(binding.getTestId()).orElse(null);
            return mapToBindingResponse(binding, dataset, test);
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    // ==================== Execution Expansion ====================

    @Transactional(readOnly = true)
    public List<Map<String, String>> expandParameters(UUID testId, UUID datasetId) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        Optional<TestDatasetBinding> bindingOpt = bindingRepository.findByTestIdAndDatasetId(testId, datasetId);
        if (bindingOpt.isEmpty()) {
            throw new ResourceNotFoundException("Dataset binding not found for test", "testId", testId);
        }

        TestDatasetBinding binding = bindingOpt.get();
        List<List<String>> rows = parseJsonToRows(dataset.getData());
        Map<String, String> columnMappings = parseJsonToMap(binding.getColumnMappings());

        List<Map<String, String>> expandedParams = new ArrayList<>();
        if (rows != null) {
            for (List<String> row : rows) {
                Map<String, String> params = new HashMap<>();
                for (int i = 0; i < row.size(); i++) {
                    String columnName = dataset.getColumnNames() != null && i < dataset.getColumnNames().size() ?
                            dataset.getColumnNames().get(i) : "col_" + i;
                    params.put(columnName, row.get(i));
                }
                expandedParams.add(params);
            }
        }

        return expandedParams;
    }

    // ==================== Helper Methods ====================

    private String parseCsvToJson(String csvData, List<String> headers) {
        String[] lines = csvData.split("\n");
        List<List<String>> rows = new ArrayList<>();

        int startIndex = 0;
        if (headers == null || headers.isEmpty()) {
            // First line is headers
            String[] headerLine = lines[0].split(",");
            headers = Arrays.asList(headerLine);
            startIndex = 1;
        }

        for (int i = startIndex; i < lines.length; i++) {
            String[] values = lines[i].split(",");
            rows.add(Arrays.asList(values));
        }

        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to parse CSV: " + e.getMessage());
        }
    }

    private List<List<String>> parseJsonToRows(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<List<String>>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON to rows: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, String> parseJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON to map: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String mapToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize: " + e.getMessage());
        }
    }

    private String createDefaultMappings(TestDataset dataset) {
        if (dataset.getColumnNames() == null) return "{}";
        Map<String, String> mappings = new HashMap<>();
        for (String col : dataset.getColumnNames()) {
            mappings.put(col, "${" + col + "}");
        }
        return mapToJson(mappings);
    }

    private DatasetResponse mapToDatasetResponse(TestDataset dataset) {
        List<DatasetVersion> versions = versionRepository.findByDatasetIdOrderByVersionNumberDesc(dataset.getId());

        return DatasetResponse.builder()
                .id(dataset.getId())
                .projectId(dataset.getProjectId())
                .name(dataset.getName())
                .description(dataset.getDescription())
                .dataFormat(dataset.getDataFormat())
                .columnNames(dataset.getColumnNames())
                .columnTypes(dataset.getColumnTypes())
                .rows(parseJsonToRows(dataset.getData()))
                .rowCount(dataset.getRowCount())
                .version(dataset.getVersion())
                .isImmutable(dataset.getIsImmutable())
                .folderId(dataset.getFolderId())
                .totalVersions(versions.size())
                .versions(versions.stream().map(this::mapToVersionResponse).collect(Collectors.toList()))
                .createdAt(dataset.getCreatedAt())
                .updatedAt(dataset.getUpdatedAt())
                .build();
    }

    private DatasetVersionResponse mapToVersionResponse(DatasetVersion version) {
        return DatasetVersionResponse.builder()
                .id(version.getId())
                .datasetId(version.getDatasetId())
                .versionNumber(version.getVersionNumber())
                .columnNames(version.getColumnNames() != null ? Arrays.asList(version.getColumnNames()) : null)
                .columnTypes(version.getColumnTypes() != null ? Arrays.asList(version.getColumnTypes()) : null)
                .data(parseJsonToRows(version.getData()))
                .rowCount(version.getRowCount())
                .changeSummary(version.getChangeSummary())
                .createdBy(version.getCreatedBy())
                .isImmutable(version.getIsImmutable())
                .createdAt(version.getCreatedAt())
                .build();
    }

    private DatasetBindingResponse mapToBindingResponse(TestDatasetBinding binding, TestDataset dataset, TestIssue test) {
        if (dataset == null || test == null) return null;

        Map<String, String> mappings = parseJsonToMap(binding.getColumnMappings());

        return DatasetBindingResponse.builder()
                .id(binding.getId())
                .testId(binding.getTestId())
                .testIssueKey(test.getName())
                .datasetId(binding.getDatasetId())
                .datasetName(dataset.getName())
                .datasetVersionId(binding.getDatasetVersionId())
                .datasetVersion(dataset.getVersion())
                .boundColumns(new ArrayList<>(mappings.keySet()))
                .rowCount(dataset.getRowCount())
                .createdBy(binding.getCreatedBy() != null ? binding.getCreatedBy().toString() : null)
                .createdAt(binding.getCreatedAt() != null ? binding.getCreatedAt().toString() : null)
                .build();
    }
}