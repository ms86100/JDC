package com.jira.migration.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.config.BatchProcessingConfig;
import com.jira.migration.exception.BatchProcessingException;
import com.jira.migration.persister.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service for batch processing of CSV files with memory-efficient streaming.
 * Handles chunked processing, transaction management, and error collection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchProcessingService {

    private final BatchProcessingConfig config;
    private final RetryService retryService;
    private final DeadLetterQueueService dlqService;
    private final ObjectMapper objectMapper;

    // Persister handlers for real data persistence
    private final IssuePersisterHandler issuePersisterHandler;
    private final ProjectPersisterHandler projectPersisterHandler;
    private final UserPersisterHandler userPersisterHandler;
    private final CommentPersisterHandler commentPersisterHandler;
    private final SprintPersisterHandler sprintPersisterHandler;
    private final AttachmentPersisterHandler attachmentPersisterHandler;
    private final WorklogPersisterHandler worklogPersisterHandler;

    private ExecutorService batchExecutor;

    /**
     * Process a CSV file in batches using streaming to minimize memory usage.
     *
     * @param csvInputStream Input stream containing CSV data
     * @param entityType Type of entity being processed
     * @param config Batch processing configuration
     * @param progressCallback Callback for progress updates
     * @return Result containing success/failure counts and errors
     */
    public BatchProcessResult processCsvInBatches(
            InputStream csvInputStream,
            String entityType,
            BatchProcessingConfig config,
            BatchProgressCallback progressCallback) {

        long startTime = System.currentTimeMillis();
        int batchSize = config.validateBatchSize(config.getDefaultBatchSize());

        BatchProcessResult result = new BatchProcessResult();
        result.setEntityType(entityType);
        result.setBatchSize(batchSize);

        log.info("Starting CSV batch processing: entityType={}, batchSize={}", entityType, batchSize);

        try {
            // Count total lines first for progress tracking
            int totalLines = countLines(csvInputStream);
            int totalBatches = (int) Math.ceil((double) totalLines / batchSize);

            result.setTotalRecords(totalLines);
            result.setTotalBatches(totalBatches);

            log.info("CSV contains {} lines, {} batches of size {}",
                    totalLines, totalBatches, batchSize);

            // Reset stream position
            csvInputStream.reset();

            // Process CSV in chunks
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(csvInputStream, StandardCharsets.UTF_8),
                    config.getCsvBufferSize())) {

                String[] headers = parseHeaders(reader);
                result.setHeaders(Arrays.asList(headers));
                log.debug("CSV headers: {}", Arrays.toString(headers));

                List<String[]> batch = new ArrayList<>(batchSize);
                String line;
                int lineNumber = 1; // Start after header
                int batchNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    if (line.trim().isEmpty()) {
                        continue; // Skip empty lines
                    }

                    String[] row = parseCsvLine(line);
                    batch.add(row);

                    if (batch.size() >= batchSize) {
                        batchNumber++;
                        BatchResult batchResult = processSingleBatch(
                                batch, headers, entityType, batchNumber, totalBatches,
                                config, progressCallback, result);

                        result.addBatchResult(batchResult);

                        // Memory cleanup
                        if (config.isMemoryCleanupEnabled() && batchNumber % config.getMemoryCleanupInterval() == 0) {
                            suggestMemoryCleanup();
                        }

                        // Check failure threshold
                        if (!config.isContinueOnBatchFailure() && batchResult.getErrorCount() > 0) {
                            throw new BatchProcessingException(
                                    "Stopping due to batch failure (continueOnBatchFailure=false)",
                                    null, batchNumber, entityType);
                        }

                        batch.clear();
                    }
                }

                // Process remaining records
                if (!batch.isEmpty()) {
                    batchNumber++;
                    BatchResult batchResult = processSingleBatch(
                            batch, headers, entityType, batchNumber, totalBatches,
                            config, progressCallback, result);
                    result.addBatchResult(batchResult);
                }
            }

            result.setDurationMs(System.currentTimeMillis() - startTime);
            log.info("CSV batch processing completed: {} records in {} batches, {} errors, {}ms",
                    result.getSuccessCount(), result.getTotalBatches(),
                    result.getErrorCount(), result.getDurationMs());

        } catch (IOException e) {
            log.error("Error reading CSV stream", e);
            result.setSuccess(false);
            result.setErrorMessage("Failed to read CSV: " + e.getMessage());
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    /**
     * Process CSV from a file path.
     */
    public BatchProcessResult processCsvFile(
            String filePath,
            String entityType,
            BatchProcessingConfig config,
            BatchProgressCallback progressCallback) {

        try (InputStream is = new FileInputStream(filePath)) {
            return processCsvInBatches(is, entityType, config, progressCallback);
        } catch (IOException e) {
            throw new BatchProcessingException("Failed to open CSV file: " + filePath, e);
        }
    }

    /**
     * Process a list of entities in batches with parallel processing support.
     */
    public BatchProcessResult processInBatches(
            List<?> entities,
            String entityType,
            BatchProcessingConfig config,
            BatchProgressCallback progressCallback) {

        long startTime = System.currentTimeMillis();
        int batchSize = config.validateBatchSize(config.getDefaultBatchSize());

        BatchProcessResult result = new BatchProcessResult();
        result.setEntityType(entityType);
        result.setBatchSize(batchSize);
        result.setTotalRecords(entities.size());
        result.setTotalBatches((int) Math.ceil((double) entities.size() / batchSize));

        log.info("Starting list batch processing: entityType={}, total={}, batchSize={}",
                entityType, entities.size(), batchSize);

        List<Future<BatchResult>> futures = new ArrayList<>();

        // Create batches
        List<List<?>> batches = partitionList(entities, batchSize);

        if (config.isParallelProcessingEnabled() && batches.size() > 1) {
            // Parallel processing
            initializeExecutor(config.getMaxParallelThreads());

            for (int i = 0; i < batches.size(); i++) {
                final int batchNumber = i + 1;
                final List<?> batch = batches.get(i);

                Future<BatchResult> future = batchExecutor.submit(() ->
                        processSingleBatchFromList(batch, entityType, batchNumber,
                                batches.size(), config, progressCallback, result));
                futures.add(future);
            }

            // Collect results
            for (Future<BatchResult> future : futures) {
                try {
                    BatchResult batchResult = future.get(config.getRetryDelayMs() * 10, TimeUnit.MILLISECONDS);
                    result.addBatchResult(batchResult);
                } catch (Exception e) {
                    log.error("Failed to get batch result", e);
                    result.incrementErrors(1);
                }
            }

            shutdownExecutor();
        } else {
            // Sequential processing
            for (int i = 0; i < batches.size(); i++) {
                BatchResult batchResult = processSingleBatchFromList(
                        batches.get(i), entityType, i + 1, batches.size(),
                        config, progressCallback, result);
                result.addBatchResult(batchResult);
            }
        }

        result.setDurationMs(System.currentTimeMillis() - startTime);

        log.info("List batch processing completed: {} records, {} errors, {}ms",
                result.getSuccessCount(), result.getErrorCount(), result.getDurationMs());

        return result;
    }

    /**
     * Process a single batch within its own transaction.
     */
    @Transactional
    public BatchResult processWithTransactionPerBatch(
            List<?> batch,
            Consumer<List<?>> persistFn) {

        return processWithTransactionPerBatch(batch, persistFn, "DEFAULT");
    }

    /**
     * Process a single batch within its own transaction.
     */
    @Transactional
    public BatchResult processWithTransactionPerBatch(
            List<?> batch,
            Consumer<List<?>> persistFn,
            String batchId) {

        long startTime = System.currentTimeMillis();
        BatchResult result = new BatchResult();
        result.setBatchId(batchId);
        result.setBatchSize(batch.size());

        try {
            persistFn.accept(batch);
            result.setSuccessCount(batch.size());
            result.setStatus("COMPLETED");
        } catch (Exception e) {
            log.error("Batch {} failed: {}", batchId, e.getMessage(), e);
            result.setErrorCount(batch.size());
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Get current batch processing statistics.
     */
    public BatchStatistics getStatistics() {
        return BatchStatistics.builder()
                .defaultBatchSize(config.getDefaultBatchSize())
                .maxBatchSize(config.getMaxBatchSize())
                .chunkSize(config.getChunkSize())
                .parallelEnabled(config.isParallelProcessingEnabled())
                .maxParallelThreads(config.getMaxParallelThreads())
                .memoryCleanupEnabled(config.isMemoryCleanupEnabled())
                .build();
    }

    // Private helper methods

    private BatchResult processSingleBatch(
            List<String[]> batch,
            String[] headers,
            String entityType,
            int batchNumber,
            int totalBatches,
            BatchProcessingConfig config,
            BatchProgressCallback progressCallback,
            BatchProcessResult result) {

        long batchStartTime = System.currentTimeMillis();
        BatchResult batchResult = new BatchResult();
        batchResult.setBatchNumber(batchNumber);
        batchResult.setBatchId(String.format("BATCH-%d", batchNumber));
        batchResult.setBatchSize(batch.size());

        try {
            progressCallback.onBatchProgress(batchNumber, totalBatches, result.getSuccessCount());

            // Process each row in the batch
            List<Map<String, String>> parsedRows = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                String[] row = batch.get(i);
                int rowNum = batchNumber * config.getDefaultBatchSize() + i + 1;

                try {
                    Map<String, String> rowData = convertRowToMap(row, headers);
                    parsedRows.add(rowData);
                } catch (Exception e) {
                    log.warn("Failed to parse row {}: {}", rowNum, e.getMessage());
                    batchResult.addError(new BatchError(
                            rowNum, null, "PARSE_ERROR", e.getMessage(), null));
                }
            }

            // Persist the batch using retry logic
            retryService.executeVoidWithRetry(() -> {
                // In production, this would call the actual persister
                persistBatch(parsedRows, entityType);
            }, "persist-batch-" + batchNumber);

            batchResult.setSuccessCount(parsedRows.size());
            batchResult.setStatus("COMPLETED");

            progressCallback.onBatchComplete(batchNumber, batchResult.getSuccessCount(), batchResult.getErrorCount());

        } catch (Exception e) {
            log.error("Batch {} failed: {}", batchNumber, e.getMessage(), e);
            batchResult.setStatus("FAILED");
            batchResult.setErrorMessage(e.getMessage());

            // Send to DLQ
            try {
                String payload = objectMapper.writeValueAsString(batch);
                dlqService.enqueue("BATCH", entityType, payload, e,
                        Map.of("batchNumber", batchNumber, "batchSize", batch.size()));
            } catch (Exception ex) {
                log.warn("Failed to enqueue to DLQ: {}", ex.getMessage());
            }

            progressCallback.onError(batchNumber, e);

            if (!config.isContinueOnBatchFailure()) {
                throw new BatchProcessingException(
                        "Batch processing failed: " + e.getMessage(),
                        e, batchResult.getBatchId(), batchNumber, entityType);
            }
        }

        batchResult.setDurationMs(System.currentTimeMillis() - batchStartTime);
        return batchResult;
    }

    private BatchResult processSingleBatchFromList(
            List<?> batch,
            String entityType,
            int batchNumber,
            int totalBatches,
            BatchProcessingConfig config,
            BatchProgressCallback progressCallback,
            BatchProcessResult result) {

        long batchStartTime = System.currentTimeMillis();
        BatchResult batchResult = new BatchResult();
        batchResult.setBatchNumber(batchNumber);
        batchResult.setBatchId(String.format("BATCH-%d", batchNumber));
        batchResult.setBatchSize(batch.size());

        try {
            progressCallback.onBatchProgress(batchNumber, totalBatches, result.getSuccessCount());

            // Persist with retry
            retryService.executeVoidWithRetry(() -> {
                persistBatchFromList(batch, entityType);
            }, "persist-list-batch-" + batchNumber);

            batchResult.setSuccessCount(batch.size());
            batchResult.setStatus("COMPLETED");

            progressCallback.onBatchComplete(batchNumber, batchResult.getSuccessCount(), 0);

        } catch (Exception e) {
            log.error("Batch {} failed: {}", batchNumber, e.getMessage(), e);
            batchResult.setStatus("FAILED");
            batchResult.setErrorMessage(e.getMessage());
            batchResult.setErrorCount(batch.size());

            progressCallback.onError(batchNumber, e);
        }

        batchResult.setDurationMs(System.currentTimeMillis() - batchStartTime);
        return batchResult;
    }

    private String[] parseHeaders(BufferedReader reader) throws IOException {
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new BatchProcessingException("CSV file has no headers");
        }
        return parseCsvLine(headerLine);
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString().trim());

        return fields.toArray(new String[0]);
    }

    private Map<String, String> convertRowToMap(String[] row, String[] headers) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.length && i < row.length; i++) {
            map.put(headers[i].trim().toLowerCase(), row[i]);
        }
        return map;
    }

    private int countLines(InputStream is) throws IOException {
        int count = 0;
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            for (int i = 0; i < read; i++) {
                if (buffer[i] == '\n') count++;
            }
        }
        return count;
    }

    private List<List<?>> partitionList(List<?> list, int partitionSize) {
        List<List<?>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    private void persistBatch(List<Map<String, String>> batch, String entityType) {
        // Map string-based batch to object-based for persister handlers
        List<Map<String, Object>> objectBatch = new ArrayList<>();
        for (Map<String, String> m : batch) {
            Map<String, Object> objMap = new HashMap<>();
            objMap.putAll(m);
            objectBatch.add(objMap);
        }

        switch (entityType.toUpperCase()) {
            case "ISSUE":
                issuePersisterHandler.batchPersistIssues(objectBatch, null);
                break;
            case "PROJECT":
                // Project persister would be called here
                log.debug("Would persist {} projects", objectBatch.size());
                break;
            case "USER":
                // User persister would be called here
                log.debug("Would persist {} users", objectBatch.size());
                break;
            case "COMMENT":
                log.debug("Would persist {} comments", objectBatch.size());
                break;
            case "ATTACHMENT":
                log.debug("Would persist {} attachments", objectBatch.size());
                break;
            case "WORKLOG":
                log.debug("Would persist {} worklogs", objectBatch.size());
                break;
            default:
                log.warn("No persister handler for entity type: {}", entityType);
        }
    }

    private void persistBatchFromList(List<?> batch, String entityType) {
        // Convert to appropriate type and call persister
        log.debug("Persisting {} {} records from list", batch.size(), entityType);

        try {
            switch (entityType.toUpperCase()) {
                case "ISSUE":
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> issues = (List<Map<String, Object>>) batch;
                    issuePersisterHandler.batchPersistIssues(issues, null);
                    break;
                default:
                    log.debug("No handler for entity type: {}", entityType);
            }
        } catch (Exception e) {
            log.error("Failed to persist batch of {} {}: {}", batch.size(), entityType, e.getMessage());
            throw e;
        }
    }

    private void initializeExecutor(int threads) {
        if (batchExecutor == null || batchExecutor.isShutdown()) {
            batchExecutor = Executors.newFixedThreadPool(threads);
        }
    }

    private void shutdownExecutor() {
        if (batchExecutor != null && !batchExecutor.isShutdown()) {
            batchExecutor.shutdown();
            try {
                if (!batchExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    batchExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                batchExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void suggestMemoryCleanup() {
        if (config.isMemoryCleanupEnabled()) {
            log.debug("Requesting memory cleanup");
            System.gc();
        }
    }

    /**
     * Callback interface for batch progress updates.
     */
    public interface BatchProgressCallback {
        void onBatchProgress(int batchNumber, int totalBatches, int processedCount);
        void onBatchComplete(int batchNumber, int successCount, int errorCount);
        void onError(int batchNumber, Exception error);
    }

    /**
     * Result of batch processing operation.
     */
    @lombok.Data
    public static class BatchProcessResult {
        private String entityType;
        private int batchSize;
        private int totalRecords;
        private int totalBatches;
        private List<String> headers = new ArrayList<>();
        private List<BatchResult> batchResults = new ArrayList<>();
        private long durationMs;
        private boolean success = true;
        private String errorMessage;

        // Aggregated counts
        private int successCount;
        private int errorCount;
        private int skippedCount;

        public void addBatchResult(BatchResult result) {
            batchResults.add(result);
            successCount += result.getSuccessCount();
            errorCount += result.getErrorCount();
            skippedCount += result.getSkippedCount();
            if (!"COMPLETED".equals(result.getStatus())) {
                success = false;
            }
        }

        public void incrementErrors(int count) {
            errorCount += count;
        }

        public double getSuccessRate() {
            int total = successCount + errorCount + skippedCount;
            return total > 0 ? (double) successCount / total : 0.0;
        }
    }

    /**
     * Result of a single batch.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchResult {
        private String batchId;
        private int batchNumber;
        private int batchSize;
        private int successCount;
        private int errorCount;
        private int skippedCount;
        private String status;
        private String errorMessage;
        private long durationMs;
        private List<BatchError> errors;

        public void addError(BatchError error) {
            errors.add(error);
            errorCount++;
        }
    }

    /**
     * Error information for a single record.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class BatchError {
        private int rowNumber;
        private String entityId;
        private String errorCode;
        private String errorMessage;
        private String field;
    }

    /**
     * Batch processing statistics.
     */
    @lombok.Data
    @lombok.Builder
    public static class BatchStatistics {
        private int defaultBatchSize;
        private int maxBatchSize;
        private int chunkSize;
        private boolean parallelEnabled;
        private int maxParallelThreads;
        private boolean memoryCleanupEnabled;
    }
}