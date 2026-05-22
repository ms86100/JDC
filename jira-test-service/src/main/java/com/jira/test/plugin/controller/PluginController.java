package com.jira.test.plugin.controller;

import com.jira.test.plugin.entity.PluginManifest;
import com.jira.test.plugin.hook.PluginHook;
import com.jira.test.plugin.hook.PluginHook.HookResult;
import com.jira.test.plugin.hook.PluginHook.HookType;
import com.jira.test.plugin.listener.PluginLifecycleListener;
import com.jira.test.plugin.listener.PluginLifecycleListener.PluginStatusReport;
import com.jira.test.plugin.service.PluginRegistry;
import com.jira.test.plugin.service.PluginRegistry.PluginStats;
import com.jira.test.plugin.validation.PluginValidator;
import com.jira.test.plugin.validation.PluginValidator.CompleteValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for plugin management operations.
 * Provides endpoints for uploading, configuring, testing, and managing plugins.
 */
@RestController
@RequestMapping("/api/plugins")
@Slf4j
@Tag(name = "Plugin Management", description = "APIs for plugin lifecycle management")
public class PluginController {

    private final PluginRegistry pluginRegistry;
    private final PluginValidator pluginValidator;
    private final PluginLifecycleListener lifecycleListener;
    private final Path uploadDirectory;

    public PluginController(PluginRegistry pluginRegistry, PluginValidator pluginValidator,
                          PluginLifecycleListener lifecycleListener) {
        this.pluginRegistry = pluginRegistry;
        this.pluginValidator = pluginValidator;
        this.lifecycleListener = lifecycleListener;
        this.uploadDirectory = Path.of(System.getProperty("java.io.tmpdir"), "jira-plugins");
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create plugin upload directory", e);
        }
    }

    /**
     * Upload and install a new plugin JAR.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Upload and install a new plugin JAR")
    public ResponseEntity<PluginUploadResponse> uploadPlugin(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") String projectId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new PluginUploadResponse(false, "File is empty", null, null, null));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".jar")) {
            return ResponseEntity.badRequest()
                    .body(new PluginUploadResponse(false, "Only JAR files are supported", null, null, null));
        }

        try {
            String pluginId = generatePluginId(filename);

            Path destinationFile = uploadDirectory.resolve(pluginId + ".jar");
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            PluginManifest manifest = parseManifest(destinationFile);
            if (manifest == null) {
                manifest = createDefaultManifest(pluginId, filename, projectId);
            }
            manifest.setProjectId(projectId);

            CompleteValidationResult validation = pluginValidator.validatePlugin(
                    manifest, null, "11.0.0", null);

            if (!validation.isManifestValid()) {
                return ResponseEntity.badRequest()
                        .body(new PluginUploadResponse(false,
                                "Manifest validation failed: " + String.join(", ", validation.getManifestErrors()),
                                null, null, null));
            }

            PluginHook hook = loadPluginHook(destinationFile, pluginId);

            PluginManifest registered = pluginRegistry.registerPlugin(pluginId, manifest, hook);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new PluginUploadResponse(true, "Plugin uploaded successfully",
                            registered.getPluginId(), registered.getStatus().name(),
                            registered.getName()));

        } catch (IOException e) {
            log.error("Failed to save plugin file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PluginUploadResponse(false, "Failed to save plugin: " + e.getMessage(), null, null, null));
        } catch (Exception e) {
            log.error("Failed to load plugin", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new PluginUploadResponse(false, "Failed to load plugin: " + e.getMessage(), null, null, null));
        }
    }

    /**
     * List all installed plugins.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all installed plugins")
    public ResponseEntity<PluginListResponse> listPlugins(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) Boolean enabled) {

        List<PluginManifest> plugins;
        if (projectId != null) {
            plugins = pluginRegistry.getPlugins(projectId);
        } else {
            plugins = pluginRegistry.getAllPlugins();
        }

        if (enabled != null) {
            plugins = plugins.stream()
                    .filter(p -> enabled.equals(p.getEnabled()))
                    .collect(Collectors.toList());
        }

        List<PluginInfo> pluginInfos = plugins.stream()
                .map(this::toPluginInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new PluginListResponse(pluginInfos, pluginInfos.size()));
    }

    /**
     * Get available marketplace plugins.
     */
    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List available marketplace plugins")
    public ResponseEntity<MarketplaceResponse> getAvailablePlugins() {
        List<MarketplacePlugin> available = new ArrayList<>();

        available.add(new MarketplacePlugin(
                "jira-test-reporter",
                "Advanced Test Reporter",
                "1.2.0",
                "Generates comprehensive test execution reports with charts and trends",
                "Jira Team",
                "Atlassian",
                List.of("READ_TESTS", "READ_RESULTS", "READ_AUDIT"),
                true
        ));

        available.add(new MarketplacePlugin(
                "jira-test-notifier",
                "Slack Notifier",
                "2.0.0",
                "Sends test execution results to Slack channels",
                "DevTools Inc",
                "DevTools Inc",
                List.of("READ_RESULTS", "WEBHOOK_ACCESS", "NETWORK_ACCESS"),
                true
        ));

        available.add(new MarketplacePlugin(
                "jira-test-automation",
                "CI/CD Integration",
                "1.5.0",
                "Integrates with Jenkins, GitHub Actions, and Azure DevOps",
                "CI Tools Co",
                "CI Tools Co",
                List.of("READ_TESTS", "EXECUTE_TESTS", "READ_RESULTS", "WEBHOOK_ACCESS"),
                false
        ));

        available.add(new MarketplacePlugin(
                "jira-test-analytics",
                "Test Analytics Dashboard",
                "3.0.0",
                "Advanced analytics and predictions for test stability",
                "Analytics Pro",
                "Analytics Pro",
                List.of("READ_TESTS", "READ_RESULTS", "READ_AUDIT"),
                true
        ));

        return ResponseEntity.ok(new MarketplaceResponse(available, available.size()));
    }

    /**
     * Install a plugin from marketplace.
     */
    @PostMapping("/{id}/install")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Install a plugin from marketplace")
    public ResponseEntity<PluginInstallResponse> installFromMarketplace(
            @PathVariable("id") String pluginId,
            @RequestParam String projectId,
            @RequestParam String version) {

        Map<String, MarketplacePlugin> marketplace = getMarketplaceMap();
        MarketplacePlugin marketplacePlugin = marketplace.get(pluginId);

        if (marketplacePlugin == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PluginInstallResponse(false, "Plugin not found in marketplace", null));
        }

        if (!marketplacePlugin.compatible()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new PluginInstallResponse(false, "Plugin not compatible with current platform version", null));
        }

        try {
            String internalPluginId = pluginId + "_marketplace_" + System.currentTimeMillis();

            PluginManifest manifest = new PluginManifest();
            manifest.setPluginId(internalPluginId);
            manifest.setName(marketplacePlugin.name());
            manifest.setVersion(version);
            manifest.setDescription(marketplacePlugin.description());
            manifest.setAuthor(marketplacePlugin.author());
            manifest.setVendor(marketplacePlugin.vendor());
            manifest.setPermissions(String.join(",", marketplacePlugin.permissions()));
            manifest.setEntryPoint("com.marketplace." + pluginId + ".Plugin");
            manifest.setProjectId(projectId);

            PluginManifest registered = pluginRegistry.registerPlugin(internalPluginId, manifest, null);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new PluginInstallResponse(true, "Plugin installed successfully", registered.getPluginId()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PluginInstallResponse(false, "Installation failed: " + e.getMessage(), null));
        }
    }

    private Map<String, MarketplacePlugin> getMarketplaceMap() {
        Map<String, MarketplacePlugin> map = new HashMap<>();
        map.put("jira-test-reporter", new MarketplacePlugin(
                "jira-test-reporter", "Advanced Test Reporter", "1.2.0",
                "Generates comprehensive test execution reports",
                "Jira Team", "Atlassian", List.of("READ_TESTS"), true));
        map.put("jira-test-notifier", new MarketplacePlugin(
                "jira-test-notifier", "Slack Notifier", "2.0.0",
                "Sends test results to Slack", "DevTools Inc", "DevTools Inc",
                List.of("WEBHOOK_ACCESS"), true));
        return map;
    }

    /**
     * Get plugin details by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get plugin details by ID")
    public ResponseEntity<PluginDetailResponse> getPluginDetails(
            @PathVariable("id") String pluginId,
            @RequestParam(required = false) String projectId) {

        PluginManifest manifest = pluginRegistry.getPlugin(pluginId);
        if (manifest == null) {
            return ResponseEntity.notFound().build();
        }

        if (projectId != null && !projectId.equals(manifest.getProjectId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        Set<HookType> hooks = pluginRegistry.getPluginHooks(pluginId);
        Set<String> dependencies = pluginRegistry.getDependencies(pluginId);
        PluginRegistry.PluginVersion version = pluginRegistry.getVersion(pluginId);
        List<HookResult> history = pluginRegistry.getHookHistory(pluginId);

        return ResponseEntity.ok(new PluginDetailResponse(
                toPluginInfo(manifest),
                hooks.stream().map(HookType::getEventName).collect(Collectors.toList()),
                new ArrayList<>(dependencies),
                version != null ? version.getVersion() : manifest.getVersion(),
                history.size(),
                (int) history.stream().filter(HookResult::isSuccess).count()
        ));
    }

    /**
     * Enable a plugin.
     */
    @PutMapping("/{id}/enable")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Enable a plugin")
    public ResponseEntity<PluginEnableResponse> enablePlugin(
            @PathVariable("id") String pluginId,
            @RequestParam String projectId) {

        try {
            PluginManifest manifest = pluginRegistry.getPlugin(pluginId);
            if (manifest == null) {
                return ResponseEntity.notFound().build();
            }

            if (!projectId.equals(manifest.getProjectId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            PluginManifest enabled = pluginRegistry.enablePlugin(pluginId);
            return ResponseEntity.ok(new PluginEnableResponse(true, "Plugin enabled", pluginId));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new PluginEnableResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Disable a plugin.
     */
    @PutMapping("/{id}/disable")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Disable a plugin")
    public ResponseEntity<PluginEnableResponse> disablePlugin(
            @PathVariable("id") String pluginId,
            @RequestParam String projectId) {

        try {
            PluginManifest manifest = pluginRegistry.getPlugin(pluginId);
            if (manifest == null) {
                return ResponseEntity.notFound().build();
            }

            if (!projectId.equals(manifest.getProjectId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            PluginManifest disabled = pluginRegistry.disablePlugin(pluginId);
            return ResponseEntity.ok(new PluginEnableResponse(true, "Plugin disabled", pluginId));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new PluginEnableResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Uninstall a plugin.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Uninstall a plugin")
    public ResponseEntity<Void> uninstallPlugin(
            @PathVariable("id") String pluginId,
            @RequestParam String projectId) {

        try {
            PluginManifest manifest = pluginRegistry.getPlugin(pluginId);
            if (manifest == null) {
                return ResponseEntity.notFound().build();
            }

            if (!projectId.equals(manifest.getProjectId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            pluginRegistry.unregisterPlugin(pluginId);

            Path pluginFile = uploadDirectory.resolve(pluginId + ".jar");
            Files.deleteIfExists(pluginFile);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get hooks implemented by a plugin.
     */
    @GetMapping("/{id}/hooks")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get hooks implemented by a plugin")
    public ResponseEntity<PluginHooksResponse> getPluginHooks(
            @PathVariable("id") String pluginId,
            @RequestParam(required = false) String projectId) {

        PluginManifest manifest = pluginRegistry.getPlugin(pluginId);
        if (manifest == null) {
            return ResponseEntity.notFound().build();
        }

        if (projectId != null && !projectId.equals(manifest.getProjectId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Set<HookType> hookTypes = pluginRegistry.getPluginHooks(pluginId);
        List<String> hooks = hookTypes.stream()
                .map(HookType::getEventName)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new PluginHooksResponse(pluginId, hooks, hooks.size()));
    }

    /**
     * Test hook execution for a specific plugin.
     */
    @GetMapping("/{id}/hooks/test")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Test hook execution for a specific plugin")
    public ResponseEntity<HookTestResponse> testHookExecution(
            @PathVariable("id") String pluginId,
            @RequestParam(required = false) String hookType,
            @RequestParam(required = false) String projectId) {

        try {
            PluginManifest manifest = pluginRegistry.getPlugin(pluginId);
            if (manifest == null) {
                return ResponseEntity.notFound().build();
            }

            if (projectId != null && !projectId.equals(manifest.getProjectId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            HookResult result;
            long startTime = System.currentTimeMillis();

            if (hookType != null) {
                HookType type = HookType.valueOf(hookType.toUpperCase());
                Map<String, Object> payload = createTestPayload();
                result = pluginRegistry.invokePluginHook(pluginId, type, payload);
            } else {
                result = pluginRegistry.testPlugin(pluginId);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(new HookTestResponse(
                    pluginId,
                    hookType != null ? hookType : "ALL",
                    result.isSuccess(),
                    result.getMessage(),
                    result.getData(),
                    executionTime
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new HookTestResponse(pluginId, hookType, false, e.getMessage(), null, 0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HookTestResponse(pluginId, hookType, false, "Test failed: " + e.getMessage(), null, 0));
        }
    }

    /**
     * Get plugin statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get plugin usage statistics")
    public ResponseEntity<PluginStatsResponse> getPluginStats() {
        PluginStats stats = pluginRegistry.getStats();
        PluginStatusReport statusReport = lifecycleListener.getStatusReport();

        List<HookTypeStats> hookTypeStats = new ArrayList<>();
        for (HookType type : HookType.values()) {
            List<PluginManifest> plugins = pluginRegistry.getPluginsWithHook(type);
            hookTypeStats.add(new HookTypeStats(
                    type.getEventName(),
                    plugins.size(),
                    (int) plugins.stream()
                            .filter(PluginManifest::getEnabled)
                            .count()
            ));
        }

        return ResponseEntity.ok(new PluginStatsResponse(
                stats.getTotalPlugins(),
                stats.getEnabledPlugins(),
                stats.getErrorPlugins(),
                stats.getTotalHookExecutions(),
                stats.getTotalHookFailures(),
                stats.getSuccessRate(),
                statusReport.isApplicationReady(),
                statusReport.getTimestamp().toString(),
                hookTypeStats
        ));
    }

    /**
     * Get plugin status report.
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get detailed plugin status report")
    public ResponseEntity<PluginStatusReport> getStatusReport() {
        return ResponseEntity.ok(lifecycleListener.getStatusReport());
    }

    /**
     * Validate a plugin manifest.
     */
    @PostMapping("/validate")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Validate a plugin manifest")
    public ResponseEntity<ValidationResponse> validatePlugin(
            @RequestBody PluginManifest manifest,
            @RequestParam String projectId) {

        if (!projectId.equals(manifest.getProjectId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        CompleteValidationResult result = pluginValidator.validatePlugin(
                manifest, null, "11.0.0", null);

        return ResponseEntity.ok(new ValidationResponse(
                result.isOverallValid(),
                result.getManifestErrors(),
                result.getManifestWarnings(),
                result.isSecurityScanPassed(),
                result.isCompatible(),
                result.getCompatibilityReason()
        ));
    }

    // Helper methods

    private String generatePluginId(String filename) {
        String baseName = filename.replace(".jar", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        return baseName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp;
    }

    private PluginManifest parseManifest(Path jarFile) {
        return null;
    }

    private PluginManifest createDefaultManifest(String pluginId, String filename, String projectId) {
        PluginManifest manifest = new PluginManifest();
        manifest.setPluginId(pluginId);
        manifest.setName(filename.replace(".jar", ""));
        manifest.setVersion("1.0.0");
        manifest.setEntryPoint("com.jira.test.plugin.DefaultPlugin");
        manifest.setProjectId(projectId);
        manifest.setEnabled(false);
        manifest.setInstalledAt(LocalDateTime.now());
        return manifest;
    }

    private PluginHook loadPluginHook(Path jarFile, String pluginId) {
        return null;
    }

    private Map<String, Object> createTestPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("testMode", true);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("testId", "test-" + System.currentTimeMillis());
        payload.put("testName", "Hook Test");
        payload.put("projectId", "test-project");
        return payload;
    }

    private PluginInfo toPluginInfo(PluginManifest manifest) {
        PluginInfo info = new PluginInfo();
        info.pluginId = manifest.getPluginId();
        info.name = manifest.getName();
        info.version = manifest.getVersion();
        info.description = manifest.getDescription();
        info.author = manifest.getAuthor();
        info.vendor = manifest.getVendor();
        info.status = manifest.getStatus().name();
        info.enabled = manifest.getEnabled();
        info.installedAt = manifest.getInstalledAt();
        info.updatedAt = manifest.getUpdatedAt();
        return info;
    }

    // Response DTOs

    public static class PluginUploadResponse {
        public boolean success;
        public String message;
        public String pluginId;
        public String status;
        public String name;

        public PluginUploadResponse(boolean success, String message, String pluginId, String status, String name) {
            this.success = success;
            this.message = message;
            this.pluginId = pluginId;
            this.status = status;
            this.name = name;
        }
    }

    public static class PluginListResponse {
        public List<PluginInfo> plugins;
        public int total;

        public PluginListResponse(List<PluginInfo> plugins, int total) {
            this.plugins = plugins;
            this.total = total;
        }
    }

    public static class PluginInfo {
        public String pluginId;
        public String name;
        public String version;
        public String description;
        public String author;
        public String vendor;
        public String status;
        public Boolean enabled;
        public LocalDateTime installedAt;
        public LocalDateTime updatedAt;
    }

    public static class MarketplaceResponse {
        public List<MarketplacePlugin> plugins;
        public int total;

        public MarketplaceResponse(List<MarketplacePlugin> plugins, int total) {
            this.plugins = plugins;
            this.total = total;
        }
    }

    public record MarketplacePlugin(
            String pluginId,
            String name,
            String version,
            String description,
            String author,
            String vendor,
            List<String> permissions,
            boolean compatible
    ) {}

    public static class PluginInstallResponse {
        public boolean success;
        public String message;
        public String installedPluginId;

        public PluginInstallResponse(boolean success, String message, String installedPluginId) {
            this.success = success;
            this.message = message;
            this.installedPluginId = installedPluginId;
        }
    }

    public static class PluginDetailResponse {
        public PluginInfo plugin;
        public List<String> hooks;
        public List<String> dependencies;
        public String version;
        public int totalExecutions;
        public int successfulExecutions;

        public PluginDetailResponse(PluginInfo plugin, List<String> hooks, List<String> dependencies,
                                    String version, int totalExecutions, int successfulExecutions) {
            this.plugin = plugin;
            this.hooks = hooks;
            this.dependencies = dependencies;
            this.version = version;
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
        }
    }

    public static class PluginEnableResponse {
        public boolean success;
        public String message;
        public String pluginId;

        public PluginEnableResponse(boolean success, String message, String pluginId) {
            this.success = success;
            this.message = message;
            this.pluginId = pluginId;
        }
    }

    public static class PluginHooksResponse {
        public String pluginId;
        public List<String> hooks;
        public int count;

        public PluginHooksResponse(String pluginId, List<String> hooks, int count) {
            this.pluginId = pluginId;
            this.hooks = hooks;
            this.count = count;
        }
    }

    public static class HookTestResponse {
        public String pluginId;
        public String hookType;
        public boolean success;
        public String message;
        public Map<String, Object> data;
        public long executionTimeMs;

        public HookTestResponse(String pluginId, String hookType, boolean success,
                               String message, Map<String, Object> data, long executionTimeMs) {
            this.pluginId = pluginId;
            this.hookType = hookType;
            this.success = success;
            this.message = message;
            this.data = data;
            this.executionTimeMs = executionTimeMs;
        }
    }

    public static class PluginStatsResponse {
        public int totalPlugins;
        public int enabledPlugins;
        public int errorPlugins;
        public long totalHookExecutions;
        public long totalHookFailures;
        public double successRate;
        public boolean applicationReady;
        public String lastStatusCheck;
        public List<HookTypeStats> hookTypeStats;

        public PluginStatsResponse(int totalPlugins, int enabledPlugins, int errorPlugins,
                                   long totalHookExecutions, long totalHookFailures,
                                   double successRate, boolean applicationReady,
                                   String lastStatusCheck, List<HookTypeStats> hookTypeStats) {
            this.totalPlugins = totalPlugins;
            this.enabledPlugins = enabledPlugins;
            this.errorPlugins = errorPlugins;
            this.totalHookExecutions = totalHookExecutions;
            this.totalHookFailures = totalHookFailures;
            this.successRate = successRate;
            this.applicationReady = applicationReady;
            this.lastStatusCheck = lastStatusCheck;
            this.hookTypeStats = hookTypeStats;
        }
    }

    public static class HookTypeStats {
        public String hookType;
        public int totalPlugins;
        public int enabledPlugins;

        public HookTypeStats(String hookType, int totalPlugins, int enabledPlugins) {
            this.hookType = hookType;
            this.totalPlugins = totalPlugins;
            this.enabledPlugins = enabledPlugins;
        }
    }

    public static class ValidationResponse {
        public boolean valid;
        public List<String> errors;
        public List<String> warnings;
        public boolean securityPassed;
        public boolean compatible;
        public String compatibilityReason;

        public ValidationResponse(boolean valid, List<String> errors, List<String> warnings,
                                 boolean securityPassed, boolean compatible, String compatibilityReason) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
            this.securityPassed = securityPassed;
            this.compatible = compatible;
            this.compatibilityReason = compatibilityReason;
        }
    }
}