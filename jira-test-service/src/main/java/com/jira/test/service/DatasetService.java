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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
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

        List<TestDatasetBinding> bindings = bindingRepository.findByDatasetId(datasetId);
        if (!bindings.isEmpty()) {
            throw new InvalidOperationException("Cannot delete dataset that is bound to tests. Unbind first.");
        }

        dataset.setArchived(true);
        datasetRepository.save(dataset);
        log.info("Dataset archived: {}", datasetId);
    }

    // ==================== Versioning with Diff ====================

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

    @Transactional(readOnly = true)
    public VersionDiffResponse getVersionDiff(UUID datasetId, Integer fromVersion, Integer toVersion) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        DatasetVersion from = versionRepository.findByDatasetIdAndVersionNumber(datasetId, fromVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found", "version", fromVersion));
        DatasetVersion to = versionRepository.findByDatasetIdAndVersionNumber(datasetId, toVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found", "version", toVersion));

        List<List<String>> fromRows = parseJsonToRows(from.getData());
        List<List<String>> toRows = parseJsonToRows(to.getData());
        List<String> fromCols = from.getColumnNames() != null ? Arrays.asList(from.getColumnNames()) : new ArrayList<>();
        List<String> toCols = to.getColumnNames() != null ? Arrays.asList(to.getColumnNames()) : new ArrayList<>();
        List<String> fromTypes = from.getColumnTypes() != null ? Arrays.asList(from.getColumnTypes()) : new ArrayList<>();
        List<String> toTypes = to.getColumnTypes() != null ? Arrays.asList(to.getColumnTypes()) : new ArrayList<>();

        // Column changes
        List<VersionDiffResponse.ColumnChange> columnChanges = new ArrayList<>();
        Set<String> fromColSet = new HashSet<>(fromCols);
        Set<String> toColSet = new HashSet<>(toCols);

        for (int i = 0; i < toCols.size(); i++) {
            String col = toCols.get(i);
            if (!fromColSet.contains(col)) {
                columnChanges.add(VersionDiffResponse.ColumnChange.builder()
                        .columnName(col).changeType("ADDED")
                        .typeTo(toTypes.size() > i ? toTypes.get(i) : null)
                        .positionTo(i).build());
            }
        }
        for (int i = 0; i < fromCols.size(); i++) {
            String col = fromCols.get(i);
            if (!toColSet.contains(col)) {
                columnChanges.add(VersionDiffResponse.ColumnChange.builder()
                        .columnName(col).changeType("REMOVED")
                        .typeFrom(fromTypes.size() > i ? fromTypes.get(i) : null)
                        .positionFrom(i).build());
            }
        }

        // Row changes - simple diff based on matching rows
        List<VersionDiffResponse.RowChange> rowChanges = new ArrayList<>();
        int rowsAdded = Math.max(0, toRows.size() - fromRows.size());
        int rowsRemoved = Math.max(0, fromRows.size() - toRows.size());

        if (rowsAdded > 0) {
            rowChanges.add(VersionDiffResponse.RowChange.builder()
                    .rowIndex(toRows.size() - 1).changeType("ADDED")
                    .modifiedColumns(List.of(rowsAdded + " rows added")).build());
        }
        if (rowsRemoved > 0) {
            rowChanges.add(VersionDiffResponse.RowChange.builder()
                    .rowIndex(fromRows.size() - 1).changeType("REMOVED")
                    .modifiedColumns(List.of(rowsRemoved + " rows removed")).build());
        }

        // Calculate cell changes
        int cellsModified = 0;
        int minRows = Math.min(fromRows.size(), toRows.size());
        int minCols = Math.min(fromCols.size(), toCols.size());
        for (int r = 0; r < minRows; r++) {
            for (int c = 0; c < minCols; c++) {
                String v1 = fromRows.get(r).get(c);
                String v2 = toRows.get(r).get(c);
                if (!Objects.equals(v1, v2)) {
                    cellsModified++;
                }
            }
        }

        VersionDiffResponse.ChangeSummary summary = VersionDiffResponse.ChangeSummary.builder()
                .columnsAdded((int) toColSet.stream().filter(c -> !fromColSet.contains(c)).count())
                .columnsRemoved((int) fromColSet.stream().filter(c -> !toColSet.contains(c)).count())
                .rowsAdded(rowsAdded).rowsRemoved(rowsRemoved).cellsModified(cellsModified)
                .build();

        return VersionDiffResponse.builder()
                .datasetId(datasetId).datasetName(dataset.getName())
                .fromVersion(fromVersion).toVersion(toVersion)
                .hasChanges(!columnChanges.isEmpty() || cellsModified > 0)
                .summary(summary)
                .columnChanges(columnChanges)
                .rowChanges(rowChanges)
                .fromVersionSummary(VersionDiffResponse.DataSummary.builder()
                        .rowCount(fromRows.size()).columnCount(fromCols.size())
                        .columnNames(fromCols).columnTypes(fromTypes).build())
                .toVersionSummary(VersionDiffResponse.DataSummary.builder()
                        .rowCount(toRows.size()).columnCount(toCols.size())
                        .columnNames(toCols).columnTypes(toTypes).build())
                .build();
    }

    // ==================== Data Validation ====================

    @Transactional(readOnly = true)
    public DatasetValidationResponse validateDataset(DatasetValidationRequest request) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(request.getDatasetId())
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", request.getDatasetId()));

        List<List<String>> rows = parseJsonToRows(dataset.getData());
        List<String> columns = dataset.getColumnNames() != null ? dataset.getColumnNames() : new ArrayList<>();
        List<String> types = dataset.getColumnTypes() != null ? dataset.getColumnTypes() : new ArrayList<>();

        List<DatasetValidationResponse.ValidationError> errors = new ArrayList<>();
        List<DatasetValidationResponse.ValidationWarning> warnings = new ArrayList<>();
        List<DatasetValidationResponse.ValidationInfo> infos = new ArrayList<>();
        Map<String, DatasetValidationResponse.ColumnValidationResult> columnResults = new HashMap<>();

        // Initialize column results
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            String type = i < types.size() ? types.get(i) : "STRING";
            columnResults.put(col, DatasetValidationResponse.ColumnValidationResult.builder()
                    .columnName(col).dataType(type).totalValues(rows.size()).nullValues(0)
                    .uniqueValues(0).completenessPercent(100.0).hasErrors(false).errorCount(0)
                    .sampleValues(new ArrayList<>()).build());
        }

        int validRows = 0;
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
        Pattern urlPattern = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");

        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            List<String> row = rows.get(rowIdx);
            boolean rowValid = true;

            for (int colIdx = 0; colIdx < columns.size() && colIdx < row.size(); colIdx++) {
                String colName = columns.get(colIdx);
                String value = row.get(colIdx);
                String type = colIdx < types.size() ? types.get(colIdx) : "STRING";

                DatasetValidationResponse.ColumnValidationResult colResult = columnResults.get(colName);

                // Check null values
                if (value == null || value.trim().isEmpty()) {
                    colResult.setNullValues(colResult.getNullValues() + 1);
                    if (colResult.getNullValues() == 1) {
                        colResult.setCompletenessPercent(100.0 * (rows.size() - colResult.getNullValues()) / rows.size());
                    }
                } else {
                    // Collect sample values
                    if (colResult.getSampleValues().size() < 5) {
                        colResult.getSampleValues().add(value);
                    }

                    // Type validation
                    boolean typeValid = true;
                    switch (type.toUpperCase()) {
                        case "NUMBER":
                            try {
                                Double.parseDouble(value);
                            } catch (NumberFormatException e) {
                                typeValid = false;
                                errors.add(DatasetValidationResponse.ValidationError.builder()
                                        .rowIndex(rowIdx).columnName(colName).ruleType("TYPE")
                                        .message("Expected number, got: " + value).actualValue(value).build());
                            }
                            break;
                        case "BOOLEAN":
                            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false") &&
                                    !value.equals("1") && !value.equals("0")) {
                                typeValid = false;
                                errors.add(DatasetValidationResponse.ValidationError.builder()
                                        .rowIndex(rowIdx).columnName(colName).ruleType("TYPE")
                                        .message("Expected boolean, got: " + value).actualValue(value).build());
                            }
                            break;
                        case "EMAIL":
                            if (!emailPattern.matcher(value).matches()) {
                                typeValid = false;
                                errors.add(DatasetValidationResponse.ValidationError.builder()
                                        .rowIndex(rowIdx).columnName(colName).ruleType("PATTERN")
                                        .message("Invalid email format: " + value).actualValue(value).build());
                            }
                            break;
                        case "URL":
                            if (!urlPattern.matcher(value).matches()) {
                                typeValid = false;
                                errors.add(DatasetValidationResponse.ValidationError.builder()
                                        .rowIndex(rowIdx).columnName(colName).ruleType("PATTERN")
                                        .message("Invalid URL format: " + value).actualValue(value).build());
                            }
                            break;
                    }

                    if (!typeValid) {
                        rowValid = false;
                        colResult.setHasErrors(true);
                        colResult.setErrorCount(colResult.getErrorCount() + 1);
                    }
                }
            }

            if (rowValid) validRows++;
        }

        // Update uniqueness counts
        for (String colName : columnResults.keySet()) {
            int colIdx = columns.indexOf(colName);
            if (colIdx >= 0) {
                Set<String> uniqueValues = new HashSet<>();
                for (List<String> row : rows) {
                    if (colIdx < row.size() && row.get(colIdx) != null) {
                        uniqueValues.add(row.get(colIdx));
                    }
                }
                columnResults.get(colName).setUniqueValues(uniqueValues.size());
            }
        }

        return DatasetValidationResponse.builder()
                .datasetId(dataset.getId()).datasetName(dataset.getName())
                .isValid(errors.isEmpty())
                .validatedAt(LocalDateTime.now())
                .totalRows(rows.size()).totalColumns(columns.size())
                .validRows(validRows).invalidRows(rows.size() - validRows)
                .errors(errors).warnings(warnings).infos(infos)
                .columnResults(columnResults)
                .build();
    }

    // ==================== Data Transformation ====================

    @Transactional(readOnly = true)
    public DataTransformResponse transformData(DataTransformRequest request) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(request.getDatasetId())
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", request.getDatasetId()));

        List<List<String>> rows = parseJsonToRows(dataset.getData());
        List<String> columns = dataset.getColumnNames() != null ? dataset.getColumnNames() : new ArrayList<>();
        List<String> types = dataset.getColumnTypes() != null ? dataset.getColumnTypes() : new ArrayList<>();

        List<List<String>> resultRows = new ArrayList<>(rows);
        List<String> resultColumns = new ArrayList<>(columns);
        List<String> resultTypes = new ArrayList<>(types);
        List<String> appliedOps = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (DataTransformRequest.TransformOperation op : request.getOperations()) {
            switch (op.getOperationType()) {
                case "FILTER":
                    resultRows = applyFilter(resultRows, resultColumns, op);
                    appliedOps.add("FILTER on " + op.getFilterColumn());
                    break;
                case "SORT":
                    applySort(resultRows, resultColumns, op);
                    appliedOps.add("SORT by " + op.getSortColumn());
                    break;
                case "PROJECT":
                    applyProject(resultRows, resultColumns, resultTypes, op);
                    appliedOps.add("PROJECT columns");
                    break;
                case "CALCULATE":
                    applyCalculate(resultRows, resultColumns, resultTypes, op);
                    appliedOps.add("CALCULATE " + op.getNewColumnName());
                    break;
            }
        }

        DataTransformResponse response = DataTransformResponse.builder()
                .success(true)
                .sourceDatasetId(dataset.getId())
                .originalRowCount(rows.size())
                .resultRowCount(resultRows.size())
                .originalColumnCount(columns.size())
                .resultColumnCount(resultColumns.size())
                .rows(resultRows)
                .columnNames(resultColumns)
                .columnTypes(resultTypes)
                .appliedOperations(appliedOps)
                .outputFormat(request.getOutputFormat() != null ? request.getOutputFormat() : "TABULAR")
                .warnings(warnings)
                .build();

        // Create new dataset if requested
        if (Boolean.TRUE.equals(request.getCreateNewDataset()) && request.getNewDatasetName() != null) {
            CreateDatasetRequest createRequest = CreateDatasetRequest.builder()
                    .projectId(dataset.getProjectId())
                    .name(request.getNewDatasetName())
                    .description("Transformed from " + dataset.getName())
                    .columnNames(resultColumns)
                    .columnTypes(resultTypes)
                    .rows(resultRows)
                    .build();
            DatasetResponse newDataset = createDataset(createRequest);
            response.setResultDatasetId(newDataset.getId());
            response.setResultDatasetName(newDataset.getName());
        }

        return response;
    }

    private List<List<String>> applyFilter(List<List<String>> rows, List<String> columns,
                                           DataTransformRequest.TransformOperation op) {
        if (op.getFilterColumn() == null || op.getFilterOperator() == null) return rows;

        int colIdx = columns.indexOf(op.getFilterColumn());
        if (colIdx < 0) return rows;

        return rows.stream().filter(row -> {
            if (colIdx >= row.size()) return false;
            String value = row.get(colIdx);
            String filterValue = op.getFilterValue() != null ? op.getFilterValue().toString() : null;

            switch (op.getFilterOperator()) {
                case "IS_NULL": return value == null || value.trim().isEmpty();
                case "IS_NOT_NULL": return value != null && !value.trim().isEmpty();
                case "EQ": return Objects.equals(value, filterValue);
                case "NE": return !Objects.equals(value, filterValue);
                case "LIKE": return value != null && value.contains(filterValue);
                case "NOT_LIKE": return value != null && !value.contains(filterValue);
                case "GT": return compareNumeric(value, filterValue) > 0;
                case "GTE": return compareNumeric(value, filterValue) >= 0;
                case "LT": return compareNumeric(value, filterValue) < 0;
                case "LTE": return compareNumeric(value, filterValue) <= 0;
                default: return true;
            }
        }).collect(Collectors.toList());
    }

    private void applySort(List<List<String>> rows, List<String> columns,
                           DataTransformRequest.TransformOperation op) {
        if (op.getSortColumn() == null) return;

        int colIdx = columns.indexOf(op.getSortColumn());
        if (colIdx < 0) return;

        boolean asc = op.getAscending() == null || op.getAscending();
        final int idx = colIdx;

        rows.sort((r1, r2) -> {
            String v1 = idx < r1.size() ? r1.get(idx) : "";
            String v2 = idx < r2.size() ? r2.get(idx) : "";
            int cmp = compareNumeric(v1, v2);
            if (cmp == 0) cmp = v1.compareTo(v2);
            return asc ? cmp : -cmp;
        });
    }

    private void applyProject(List<List<String>> rows, List<String> columns, List<String> types,
                               DataTransformRequest.TransformOperation op) {
        if (op.getSelectColumns() == null || op.getSelectColumns().isEmpty()) return;

        List<Integer> selectedIndices = new ArrayList<>();
        List<String> newColumns = new ArrayList<>();
        List<String> newTypes = new ArrayList<>();

        for (String col : op.getSelectColumns()) {
            int idx = columns.indexOf(col);
            if (idx >= 0) {
                selectedIndices.add(idx);
                newColumns.add(col);
                if (idx < types.size()) newTypes.add(types.get(idx));
            }
        }

        List<List<String>> projectedRows = rows.stream().map(row -> {
            List<String> newRow = new ArrayList<>();
            for (int idx : selectedIndices) {
                newRow.add(idx < row.size() ? row.get(idx) : null);
            }
            return newRow;
        }).collect(Collectors.toList());

        rows.clear();
        rows.addAll(projectedRows);
        columns.clear();
        columns.addAll(newColumns);
        types.clear();
        types.addAll(newTypes);
    }

    private void applyCalculate(List<List<String>> rows, List<String> columns, List<String> types,
                                DataTransformRequest.TransformOperation op) {
        if (op.getNewColumnName() == null || op.getExpression() == null) return;

        columns.add(op.getNewColumnName());
        types.add("STRING");

        for (List<String> row : rows) {
            String result = evaluateExpression(row, columns, op.getExpression());
            row.add(result);
        }
    }

    private String evaluateExpression(List<String> row, List<String> columns, String expression) {
        String expr = expression;
        for (int i = 0; i < columns.size() && i < row.size(); i++) {
            expr = expr.replace("${" + columns.get(i) + "}", row.get(i) != null ? row.get(i) : "0");
            expr = expr.replace("#{" + columns.get(i) + "}", row.get(i) != null ? row.get(i) : "0");
        }

        // Simple arithmetic evaluation
        expr = expr.replaceAll("([0-9.]+)\\s*\\+\\s*([0-9.]+)", "$1+$2");
        expr = expr.replaceAll("([0-9.]+)\\s*-\\s*([0-9.]+)", "$1-$2");
        expr = expr.replaceAll("([0-9.]+)\\s*\\*\\s*([0-9.]+)", "$1*$2");
        expr = expr.replaceAll("([0-9.]+)\\s*/\\s*([0-9.]+)", "$1/$2");

        try {
            return String.valueOf(new javax.script.ScriptEngineManager().getEngineByName("JavaScript").eval(expr));
        } catch (Exception e) {
            return expression;
        }
    }

    private int compareNumeric(String v1, String v2) {
        try {
            double d1 = Double.parseDouble(v1);
            double d2 = Double.parseDouble(v2);
            return Double.compare(d1, d2);
        } catch (NumberFormatException e) {
            return v1.compareTo(v2);
        }
    }

    // ==================== Dataset Comparison ====================

    @Transactional(readOnly = true)
    public DatasetCompareResponse compareDatasets(DatasetCompareRequest request) {
        TestDataset ds1 = datasetRepository.findByIdAndArchivedFalse(request.getDatasetId1())
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", request.getDatasetId1()));
        TestDataset ds2 = datasetRepository.findByIdAndArchivedFalse(request.getDatasetId2())
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", request.getDatasetId2()));

        List<List<String>> rows1 = parseJsonToRows(ds1.getData());
        List<List<String>> rows2 = parseJsonToRows(ds2.getData());
        List<String> cols1 = ds1.getColumnNames() != null ? ds1.getColumnNames() : new ArrayList<>();
        List<String> cols2 = ds2.getColumnNames() != null ? ds2.getColumnNames() : new ArrayList<>();
        List<String> types1 = ds1.getColumnTypes() != null ? ds1.getColumnTypes() : new ArrayList<>();
        List<String> types2 = ds2.getColumnTypes() != null ? ds2.getColumnTypes() : new ArrayList<>();

        // Structure comparison
        DatasetCompareResponse.StructureComparison structComp = compareStructure(
                cols1, cols2, types1, types2, request.isCaseSensitive());

        // Data comparison
        DatasetCompareResponse.DataComparison dataComp = compareData(
                rows1, rows2, cols1, cols2, request);

        // Calculate similarity
        double totalCells = (double) Math.max(rows1.size() * cols1.size(), rows2.size() * cols2.size());
        double diffCells = dataComp.getDifferentRows() * Math.max(cols1.size(), cols2.size()) +
                Math.abs(rows1.size() - rows2.size()) * Math.max(cols1.size(), cols2.size());
        double similarity = totalCells > 0 ? 100.0 * (1.0 - diffCells / totalCells) : 100.0;

        return DatasetCompareResponse.builder()
                .datasetId1(ds1.getId()).dataset1Name(ds1.getName())
                .datasetId2(ds2.getId()).dataset2Name(ds2.getName())
                .areIdentical(similarity == 100.0 && structComp.getColumnsOnlyInDataset1().isEmpty() &&
                        structComp.getColumnsOnlyInDataset2().isEmpty())
                .comparedAt(LocalDateTime.now())
                .structureComparison(structComp)
                .dataComparison(dataComp)
                .totalDifferences(dataComp.getDifferentRows() + structComp.getColumnsOnlyInDataset1().size() +
                        structComp.getColumnsOnlyInDataset2().size())
                .similarityPercent(Math.round(similarity * 100.0) / 100.0)
                .differenceSummary(buildDifferenceSummary(structComp, dataComp))
                .build();
    }

    private DatasetCompareResponse.StructureComparison compareStructure(
            List<String> cols1, List<String> cols2, List<String> types1, List<String> types2,
            boolean caseSensitive) {

        List<String> cols1Norm = caseSensitive ? cols1 : cols1.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<String> cols2Norm = caseSensitive ? cols2 : cols2.stream().map(String::toLowerCase).collect(Collectors.toList());

        List<String> onlyIn1 = cols1.stream().filter(c -> !cols2Norm.contains(caseSensitive ? c : c.toLowerCase())).collect(Collectors.toList());
        List<String> onlyIn2 = cols2.stream().filter(c -> !cols1Norm.contains(caseSensitive ? c : c.toLowerCase())).collect(Collectors.toList());

        List<DatasetCompareResponse.MapDifference> typeDiffs = new ArrayList<>();
        for (int i = 0; i < cols1.size() && i < cols2.size(); i++) {
            String c1 = cols1.get(i), c2 = cols2.get(i);
            if ((caseSensitive && c1.equals(c2)) || (!caseSensitive && c1.equalsIgnoreCase(c2))) {
                String t1 = i < types1.size() ? types1.get(i) : "STRING";
                String t2 = i < types2.size() ? types2.get(i) : "STRING";
                if (!t1.equals(t2)) {
                    typeDiffs.add(DatasetCompareResponse.MapDifference.builder()
                            .columnName(c1).typeInDataset1(t1).typeInDataset2(t2).build());
                }
            }
        }

        return DatasetCompareResponse.StructureComparison.builder()
                .hasSameColumns(new HashSet<>(cols1Norm).equals(new HashSet<>(cols2Norm)))
                .hasSameColumnOrder(cols1Norm.equals(cols2Norm))
                .hasSameColumnTypes(typeDiffs.isEmpty())
                .columnCount1(cols1.size()).columnCount2(cols2.size())
                .columnsOnlyInDataset1(onlyIn1).columnsOnlyInDataset2(onlyIn2)
                .columnsWithDifferentTypes(typeDiffs.stream().map(DatasetCompareResponse.MapDifference::getColumnName).collect(Collectors.toList()))
                .columnTypeDifferences(typeDiffs)
                .build();
    }

    private DatasetCompareResponse.DataComparison compareData(
            List<List<String>> rows1, List<List<String>> rows2,
            List<String> cols1, List<String> cols2, DatasetCompareRequest request) {

        int matching = 0, different = 0;
        List<DatasetCompareResponse.CellDifference> cellDiffs = new ArrayList<>();
        Map<String, Integer> colDiffCounts = new HashMap<>();

        int minRows = Math.min(rows1.size(), rows2.size());
        int minCols = Math.min(cols1.size(), cols2.size());

        for (int r = 0; r < minRows; r++) {
            boolean rowDifferent = false;
            for (int c = 0; c < minCols; c++) {
                String v1 = r < rows1.size() && c < rows1.get(r).size() ? rows1.get(r).get(c) : null;
                String v2 = r < rows2.size() && c < rows2.get(r).size() ? rows2.get(r).get(c) : null;

                boolean cellsMatch = compareValues(v1, v2, request);
                if (!cellsMatch) {
                    rowDifferent = true;
                    cellDiffs.add(DatasetCompareResponse.CellDifference.builder()
                            .columnName(cols1.get(c)).rowIndex(r)
                            .valueInDataset1(v1).valueInDataset2(v2).build());

                    colDiffCounts.merge(cols1.get(c), 1, Integer::sum);
                }
            }
            if (rowDifferent) different++;
            else matching++;
        }

        return DatasetCompareResponse.DataComparison.builder()
                .rowCount1(rows1.size()).rowCount2(rows2.size())
                .matchingRows(matching).differentRows(different)
                .rowsOnlyInDataset1(Math.max(0, rows1.size() - minRows))
                .rowsOnlyInDataset2(Math.max(0, rows2.size() - minRows))
                .cellDifferences(cellDiffs)
                .columnDiffCounts(colDiffCounts)
                .build();
    }

    private boolean compareValues(String v1, String v2, DatasetCompareRequest request) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;

        String s1 = request.isIgnoreWhitespace() ? v1.trim() : v1;
        String s2 = request.isIgnoreWhitespace() ? v2.trim() : v2;
        String compare1 = request.isCaseSensitive() ? s1 : s1.toLowerCase();
        String compare2 = request.isCaseSensitive() ? s2 : s2.toLowerCase();

        if (compare1.equals(compare2)) return true;

        // Numeric comparison with tolerance
        try {
            double d1 = Double.parseDouble(v1);
            double d2 = Double.parseDouble(v2);
            return Math.abs(d1 - d2) <= request.getNumericTolerance();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<String> buildDifferenceSummary(DatasetCompareResponse.StructureComparison struct,
                                                  DatasetCompareResponse.DataComparison data) {
        List<String> summary = new ArrayList<>();
        if (!struct.getColumnsOnlyInDataset1().isEmpty()) {
            summary.add(struct.getColumnsOnlyInDataset1().size() + " columns only in dataset 1");
        }
        if (!struct.getColumnsOnlyInDataset2().isEmpty()) {
            summary.add(struct.getColumnsOnlyInDataset2().size() + " columns only in dataset 2");
        }
        if (data.getRowCount1() != data.getRowCount2()) {
            summary.add("Row count differs: " + data.getRowCount1() + " vs " + data.getRowCount2());
        }
        if (data.getDifferentRows() > 0) {
            summary.add(data.getDifferentRows() + " rows have different values");
        }
        return summary;
    }

    // ==================== Mock Data Generation ====================

    @Transactional(readOnly = true)
    public MockDataResponse generateMockData(MockDataRequest request) {
        long startTime = System.currentTimeMillis();
        long seed = request.getSeed() != null ? request.getSeed() : ThreadLocalRandom.current().nextLong();

        List<List<String>> rows = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        List<String> columnTypes = new ArrayList<>();

        for (MockDataRequest.ColumnSchema col : request.getSchema()) {
            columnNames.add(col.getName());
            columnTypes.add(col.getType());
        }

        ThreadLocalRandom random = ThreadLocalRandom.of(seed);

        for (int r = 0; r < request.getRowCount(); r++) {
            List<String> row = new ArrayList<>();
            Set<String> generatedValues = new HashSet<>();

            for (int c = 0; c < request.getSchema().size(); c++) {
                MockDataRequest.ColumnSchema col = request.getSchema().get(c);
                String value = generateValue(col, random, generatedValues, r);
                row.add(value);
            }
            rows.add(row);
        }

        MockDataResponse response = MockDataResponse.builder()
                .success(true)
                .rowCount(rows.size())
                .columnCount(columnNames.size())
                .columnNames(columnNames)
                .columnTypes(columnTypes)
                .rows(rows)
                .outputFormat(request.getOutputFormat() != null ? request.getOutputFormat() : "TABULAR")
                .seedUsed(seed)
                .locale(request.getLocale())
                .generationTimeMs(System.currentTimeMillis() - startTime)
                .build();

        // Create dataset if requested
        if (request.getProjectId() != null) {
            try {
                CreateDatasetRequest createReq = CreateDatasetRequest.builder()
                        .projectId(request.getProjectId())
                        .name("MockData_" + System.currentTimeMillis())
                        .columnNames(columnNames)
                        .columnTypes(columnTypes)
                        .rows(rows)
                        .build();
                DatasetResponse ds = createDataset(createReq);
                response.setDatasetId(ds.getId());
                response.setDatasetName(ds.getName());
            } catch (Exception e) {
                log.warn("Failed to create dataset for mock data: {}", e.getMessage());
            }
        }

        return response;
    }

    private String generateValue(MockDataRequest.ColumnSchema col, ThreadLocalRandom random,
                                 Set<String> generatedValues, int rowIndex) {
        // Handle null values
        if (col.getNullPercentage() != null && random.nextDouble() < col.getNullPercentage()) {
            return null;
        }

        String value = null;

        // Use possible values if provided
        if (col.getPossibleValues() != null && !col.getPossibleValues().isEmpty()) {
            value = col.getPossibleValues().get(random.nextInt(col.getPossibleValues().size()));
        } else {
            switch (col.getType().toUpperCase()) {
                case "STRING":
                    value = generateString(random, col);
                    break;
                case "NUMBER":
                    value = generateNumber(random, col);
                    break;
                case "BOOLEAN":
                    value = random.nextBoolean() ? "true" : "false";
                    break;
                case "DATE":
                    value = generateDate(random, col);
                    break;
                case "DATETIME":
                    value = generateDateTime(random, col);
                    break;
                case "EMAIL":
                    value = generateEmail(random);
                    break;
                case "URL":
                    value = generateUrl(random);
                    break;
                case "PHONE":
                    value = generatePhone(random, col);
                    break;
                case "NAME":
                    value = generateName(random);
                    break;
                case "UUID":
                    value = UUID.randomUUID().toString();
                    break;
                case "ADDRESS":
                    value = generateAddress(random);
                    break;
                default:
                    value = "value_" + rowIndex;
            }
        }

        // Enforce uniqueness
        if (Boolean.TRUE.equals(col.getUnique())) {
            int attempts = 0;
            while (generatedValues.contains(value) && attempts < 100) {
                value = generateValue(col, random, generatedValues, rowIndex + attempts);
                attempts++;
            }
            generatedValues.add(value);
        }

        return value;
    }

    private String generateString(ThreadLocalRandom random, MockDataRequest.ColumnSchema col) {
        int minLen = col.getMinLength() != null ? col.getMinLength() : 5;
        int maxLen = col.getMaxLength() != null ? col.getMaxLength() : 20;
        int len = minLen + random.nextInt(Math.max(1, maxLen - minLen));

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateNumber(ThreadLocalRandom random, MockDataRequest.ColumnSchema col) {
        double min = col.getMin() != null ? col.getMin() : 0;
        double max = col.getMax() != null ? col.getMax() : 1000;
        int decimals = col.getDecimalPlaces() != null ? col.getDecimalPlaces() : 0;

        double value = min + (max - min) * random.nextDouble();
        if (decimals == 0) {
            return String.valueOf((int) value);
        }
        return String.format("%." + decimals + "f", value);
    }

    private String generateDate(ThreadLocalRandom random, MockDataRequest.ColumnSchema col) {
        int year = 2020 + random.nextInt(6);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private String generateDateTime(ThreadLocalRandom random, MockDataRequest.ColumnSchema col) {
        return generateDate(random, col) + "T" + String.format("%02d:%02d:%02d",
                random.nextInt(24), random.nextInt(60), random.nextInt(60));
    }

    private String generateEmail(ThreadLocalRandom random) {
        String[] domains = {"example.com", "test.org", "demo.net"};
        String[] names = {"user", "test", "demo", "admin", "john", "jane"};
        return names[random.nextInt(names.length)] + random.nextInt(1000) + "@" +
                domains[random.nextInt(domains.length)];
    }

    private String generateUrl(ThreadLocalRandom random) {
        String[] protocols = {"http", "https"};
        String[] domains = {"example.com", "test.org", "demo.net"};
        return protocols[random.nextInt(protocols.length)] + "://" + domains[random.nextInt(domains.length)] +
                "/page/" + random.nextInt(1000);
    }

    private String generatePhone(ThreadLocalRandom random, MockDataRequest.ColumnSchema col) {
        String pattern = col.getPattern() != null ? col.getPattern() : "XXX-XXX-XXXX";
        StringBuilder sb = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == 'X') {
                sb.append(random.nextInt(10));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String generateName(ThreadLocalRandom random) {
        String[] first = {"John", "Jane", "Bob", "Alice", "Charlie", "Diana"};
        String[] last = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Miller"};
        return first[random.nextInt(first.length)] + " " + last[random.nextInt(last.length)];
    }

    private String generateAddress(ThreadLocalRandom random) {
        int num = 100 + random.nextInt(9000);
        String[] streets = {"Main St", "Oak Ave", "Park Blvd", "Market St", "First Ave"};
        String[] cities = {"Springfield", "Riverside", "Georgetown", "Fairview", "Madison"};
        String[] states = {"CA", "NY", "TX", "FL", "WA"};
        return num + " " + streets[random.nextInt(streets.length)] + ", " +
                cities[random.nextInt(cities.length)] + ", " + states[random.nextInt(states.length)] +
                " " + (10000 + random.nextInt(90000));
    }

    // ==================== Dataset Templates ====================

    public List<DatasetTemplateResponse> getTemplates(String category) {
        List<DatasetTemplateResponse> templates = new ArrayList<>();

        templates.add(DatasetTemplateResponse.builder()
                .templateId("users-basic").name("Basic Users").category("USERS")
                .description("Basic user data with name, email, and role")
                .isBuiltIn(true).usageCount(150).isRecommended(true).difficultyLevel("BEGINNER")
                .columnNames(List.of("userId", "username", "email", "role", "department"))
                .columnTypes(List.of("STRING", "STRING", "EMAIL", "STRING", "STRING"))
                .tags(List.of("users", "authentication", "basic"))
                .build());

        templates.add(DatasetTemplateResponse.builder()
                .templateId("products-ecommerce").name("E-commerce Products").category("PRODUCTS")
                .description("Product catalog with pricing and inventory")
                .isBuiltIn(true).usageCount(120).isRecommended(true).difficultyLevel("INTERMEDIATE")
                .columnNames(List.of("productId", "name", "category", "price", "stock", "sku"))
                .columnTypes(List.of("STRING", "STRING", "STRING", "NUMBER", "NUMBER", "STRING"))
                .tags(List.of("products", "ecommerce", "inventory"))
                .build());

        templates.add(DatasetTemplateResponse.builder()
                .templateId("locations").name("Locations").category("LOCATIONS")
                .description("Geographic locations with addresses")
                .isBuiltIn(true).usageCount(95).isRecommended(false).difficultyLevel("BEGINNER")
                .columnNames(List.of("locationId", "name", "address", "city", "state", "country", "zipCode"))
                .columnTypes(List.of("STRING", "STRING", "ADDRESS", "STRING", "STRING", "STRING", "STRING"))
                .tags(List.of("locations", "addresses", "geo"))
                .build());

        templates.add(DatasetTemplateResponse.builder()
                .templateId("test-data-matrix").name("Test Data Matrix").category("TEST_DATA")
                .description("Combinatorial test data for matrix testing")
                .isBuiltIn(true).usageCount(200).isRecommended(true).difficultyLevel("INTERMEDIATE")
                .columnNames(List.of("testCaseId", "browser", "platform", "environment", "dataSet"))
                .columnTypes(List.of("STRING", "STRING", "STRING", "STRING", "STRING"))
                .tags(List.of("testing", "matrix", "combinatorial"))
                .build());

        templates.add(DatasetTemplateResponse.builder()
                .templateId("config-values").name("Configuration Values").category("CONFIG")
                .description("Application configuration key-value pairs")
                .isBuiltIn(true).usageCount(80).isRecommended(true).difficultyLevel("BEGINNER")
                .columnNames(List.of("key", "value", "environment", "category", "description"))
                .columnTypes(List.of("STRING", "STRING", "STRING", "STRING", "STRING"))
                .tags(List.of("config", "settings", "key-value"))
                .build());

        templates.add(DatasetTemplateResponse.builder()
                .templateId("api-requests").name("API Request Data").category("TEST_DATA")
                .description("API test data with endpoints and payloads")
                .isBuiltIn(true).usageCount(110).isRecommended(false).difficultyLevel("ADVANCED")
                .columnNames(List.of("requestId", "method", "endpoint", "headers", "payload", "expectedStatus"))
                .columnTypes(List.of("STRING", "STRING", "URL", "STRING", "STRING", "NUMBER"))
                .tags(List.of("api", "testing", "rest"))
                .build());

        templates.add(DatasetTemplateResponse.builder()
                .templateId("db-records").name("Database Records").category("TEST_DATA")
                .description("Sample database records for integration testing")
                .isBuiltIn(true).usageCount(75).isRecommended(false).difficultyLevel("ADVANCED")
                .columnNames(List.of("id", "name", "email", "createdAt", "updatedAt", "status", "metadata"))
                .columnTypes(List.of("NUMBER", "STRING", "EMAIL", "DATETIME", "DATETIME", "STRING", "STRING"))
                .tags(List.of("database", "integration", "records"))
                .build());

        if (category != null && !category.isEmpty()) {
            return templates.stream().filter(t -> t.getCategory().equalsIgnoreCase(category)).collect(Collectors.toList());
        }
        return templates;
    }

    // ==================== Dataset Sharing ====================

    @Transactional
    public DatasetSharingResponse shareDataset(DatasetSharingRequest request) {
        TestDataset source = datasetRepository.findByIdAndArchivedFalse(request.getSourceDatasetId())
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", request.getSourceDatasetId()));

        String newName = request.getTargetDatasetName() != null ?
                request.getTargetDatasetName() : source.getName() + " (Shared)";

        CreateDatasetRequest createRequest = CreateDatasetRequest.builder()
                .projectId(request.getTargetProjectId())
                .name(newName)
                .description("Shared from: " + source.getName())
                .columnNames(source.getColumnNames())
                .columnTypes(source.getColumnTypes())
                .rows(parseJsonToRows(source.getData()))
                .build();

        DatasetResponse shared = createDataset(createRequest);

        // Copy bindings if requested
        if (Boolean.TRUE.equals(request.getIncludeBindings())) {
            List<TestDatasetBinding> bindings = bindingRepository.findByDatasetId(request.getSourceDatasetId());
            for (TestDatasetBinding binding : bindings) {
                BindDatasetRequest bindRequest = BindDatasetRequest.builder()
                        .testId(binding.getTestId())
                        .datasetId(shared.getId())
                        .columnMappings(parseJsonToMap(binding.getColumnMappings()))
                        .build();
                try {
                    bindToTest(bindRequest);
                } catch (Exception e) {
                    log.warn("Failed to copy binding: {}", e.getMessage());
                }
            }
        }

        return DatasetSharingResponse.builder()
                .success(true)
                .sourceDatasetId(source.getId())
                .targetDatasetId(shared.getId())
                .targetDatasetName(shared.getName())
                .targetProjectId(request.getTargetProjectId())
                .sharedAt(LocalDateTime.now())
                .appliedRoles(request.getTargetRoles())
                .isPublic(request.getMakePublic())
                .totalRows(source.getRowCount())
                .bindingsIncluded(request.getIncludeBindings())
                .build();
    }

    // ==================== Multi-Format Export ====================

    public String exportDataset(UUID datasetId, DatasetExportRequest request) {
        TestDataset dataset = datasetRepository.findByIdAndArchivedFalse(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        List<List<String>> rows = parseJsonToRows(dataset.getData());
        List<String> columns = dataset.getColumnNames() != null ? dataset.getColumnNames() : new ArrayList<>();

        String format = request.getFormat() != null ? request.getFormat().toUpperCase() : "CSV";

        switch (format) {
            case "CSV":
                return exportToCSV(rows, columns, request);
            case "JSON":
                return exportToJSON(rows, columns, request);
            case "XML":
                return exportToXML(dataset, rows, columns, request);
            case "SQL":
                return exportToSQL(dataset, rows, columns, request);
            case "YAML":
                return exportToYAML(dataset, rows, columns, request);
            default:
                return exportToCSV(rows, columns, request);
        }
    }

    private String exportToCSV(List<List<String>> rows, List<String> columns, DatasetExportRequest request) {
        StringBuilder sb = new StringBuilder();
        String delim = request.getDelimiter() != null ? request.getDelimiter() : ",";
        String quote = request.getQuoteChar() != null ? request.getQuoteChar() : "\"";

        if (request.getColumnsToInclude() != null && !request.getColumnsToInclude().isEmpty()) {
            List<Integer> indices = new ArrayList<>();
            for (String col : request.getColumnsToInclude()) {
                int idx = columns.indexOf(col);
                if (idx >= 0) indices.add(idx);
            }
            sb.append(indices.stream().map(columns::get).collect(Collectors.joining(delim))).append("\n");

            int limit = request.getRowLimit() != null ? request.getRowLimit() : rows.size();
            for (int r = 0; r < Math.min(limit, rows.size()); r++) {
                List<String> row = rows.get(r);
                sb.append(indices.stream().map(i -> i < row.size() ? quote + row.get(i) + quote : "").
                        collect(Collectors.joining(delim))).append("\n");
            }
        } else {
            if (request.getIncludeRowNumbers()) {
                sb.append("row_num").append(delim);
            }
            sb.append(String.join(delim, columns)).append("\n");

            int offset = request.getOffset() != null ? request.getOffset() : 0;
            int limit = request.getRowLimit() != null ? request.getRowLimit() : rows.size();
            for (int r = offset; r < Math.min(offset + limit, rows.size()); r++) {
                if (request.getIncludeRowNumbers()) {
                    sb.append(r + 1).append(delim);
                }
                sb.append(rows.get(r).stream().map(v -> quote + (v != null ? v : "") + quote).
                        collect(Collectors.joining(delim))).append("\n");
            }
        }
        return sb.toString();
    }

    private String exportToJSON(List<List<String>> rows, List<String> columns, DatasetExportRequest request) {
        List<Map<String, String>> data = new ArrayList<>();

        int offset = request.getOffset() != null ? request.getOffset() : 0;
        int limit = request.getRowLimit() != null ? request.getRowLimit() : rows.size();

        for (int r = offset; r < Math.min(offset + limit, rows.size()); r++) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < columns.size() && c < rows.get(r).size(); c++) {
                if (request.getColumnsToInclude() == null || request.getColumnsToInclude().contains(columns.get(c))) {
                    row.put(columns.get(c), rows.get(r).get(c));
                }
            }
            data.add(row);
        }

        try {
            if (Boolean.TRUE.equals(request.getPrettyPrint())) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            }
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to export JSON: " + e.getMessage());
        }
    }

    private String exportToXML(TestDataset dataset, List<List<String>> rows, List<String> columns,
                                DatasetExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<dataset name=\"").append(escapeXml(dataset.getName())).append("\">\n");

        int offset = request.getOffset() != null ? request.getOffset() : 0;
        int limit = request.getRowLimit() != null ? request.getRowLimit() : rows.size();

        for (int r = offset; r < Math.min(offset + limit, rows.size()); r++) {
            sb.append("  <row");
            if (request.getIncludeRowNumbers()) {
                sb.append(" index=\"").append(r + 1).append("\"");
            }
            sb.append(">\n");
            for (int c = 0; c < columns.size() && c < rows.get(r).size(); c++) {
                if (request.getColumnsToInclude() == null || request.getColumnsToInclude().contains(columns.get(c))) {
                    sb.append("    <").append(escapeXml(columns.get(c))).append(">");
                    sb.append(escapeXml(rows.get(r).get(c)));
                    sb.append("</").append(escapeXml(columns.get(c))).append(">\n");
                }
            }
            sb.append("  </row>\n");
        }
        sb.append("</dataset>");
        return sb.toString();
    }

    private String exportToSQL(TestDataset dataset, List<List<String>> rows, List<String> columns,
                               DatasetExportRequest request) {
        String tableName = dataset.getName().replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        StringBuilder sb = new StringBuilder();

        sb.append("-- Export of dataset: ").append(dataset.getName()).append("\n");
        sb.append("DROP TABLE IF EXISTS ").append(tableName).append(";\n\n");
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");

        List<String> colDefs = new ArrayList<>();
        for (String col : columns) {
            colDefs.add("    " + col.replaceAll("[^a-zA-Z0-9_]", "_") + " VARCHAR(255)");
        }
        sb.append(String.join(",\n", colDefs)).append("\n);\n\n");

        for (List<String> row : rows) {
            List<String> values = row.stream().map(v -> "'" + (v != null ? v.replace("'", "''") : "") + "'").collect(Collectors.toList());
            sb.append("INSERT INTO ").append(tableName).append(" VALUES (").
                    append(String.join(", ", values)).append(");\n");
        }
        return sb.toString();
    }

    private String exportToYAML(TestDataset dataset, List<List<String>> rows, List<String> columns,
                               DatasetExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Dataset: ").append(dataset.getName()).append("\n");
        sb.append("dataset:\n");
        sb.append("  name: ").append(dataset.getName()).append("\n");
        sb.append("  columns:\n");
        for (String col : columns) {
            sb.append("    - ").append(col).append("\n");
        }
        sb.append("  rows:\n");

        int offset = request.getOffset() != null ? request.getOffset() : 0;
        int limit = request.getRowLimit() != null ? request.getRowLimit() : rows.size();

        for (int r = offset; r < Math.min(offset + limit, rows.size()); r++) {
            sb.append("    - ");
            List<String> entry = new ArrayList<>();
            for (int c = 0; c < columns.size() && c < rows.get(r).size(); c++) {
                entry.add(columns.get(c) + ": \"" + (rows.get(r).get(c) != null ? rows.get(r).get(c).replace("\"", "\\\"") : "") + "\"");
            }
            sb.append("{").append(String.join(", ", entry)).append("}\n");
        }
        return sb.toString();
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
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