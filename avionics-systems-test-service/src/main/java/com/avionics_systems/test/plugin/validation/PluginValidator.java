package com.avionics_systems.test.plugin.validation;

import com.avionics_systems.test.plugin.entity.PluginManifest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validator for plugin manifests, security scanning, permission checks,
 * and compatibility verification.
 */
@Component
@Slf4j
public class PluginValidator {

    private static final Pattern PLUGIN_ID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9-_]{2,50}$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$");
    private static final Pattern PERMISSION_PATTERN = Pattern.compile("^[A-Z_]+$");

    private static final Set<String> ALLOWED_PERMISSIONS = Set.of(
            "READ_TESTS",
            "WRITE_TESTS",
            "DELETE_TESTS",
            "EXECUTE_TESTS",
            "READ_RESULTS",
            "WRITE_RESULTS",
            "READ_ENVIRONMENTS",
            "WRITE_ENVIRONMENTS",
            "READ_REQUIREMENTS",
            "WRITE_REQUIREMENTS",
            "READ_DATASETS",
            "WRITE_DATASETS",
            "READ_CONFIGS",
            "WRITE_CONFIGS",
            "EXECUTE_HOOKS",
            "READ_AUDIT",
            "WEBHOOK_ACCESS",
            "NETWORK_ACCESS",
            "FILE_ACCESS"
    );

    private static final Set<String> DANGEROUS_PATTERNS = Set.of(
            "Runtime.getRuntime().exec(",
            "ProcessBuilder(",
            "FileInputStream(",
            "FileOutputStream(",
            "Socket(",
            "ServerSocket(",
            "URLConnection",
            "HttpURLConnection",
            "javax.script.ScriptEngineManager",
            "java.lang.reflect",
            "com.sun.",
            "sun.misc.",
            "java.io.File.delete",
            "System.exit(",
            "Runtime.halt(",
            "Class.forName(",
            "java.lang.Thread.stop"
    );

    private static final String MIN_COMPATIBLE_VERSION = "10.0.0";
    private static final String MAX_COMPATIBLE_VERSION = "99.0.0";

    /**
     * Validate a plugin manifest.
     */
    public ValidationResult validateManifest(PluginManifest manifest) {
        ValidationResult result = new ValidationResult();

        if (manifest == null) {
            result.addError("Manifest cannot be null");
            return result;
        }

        validatePluginId(manifest.getPluginId(), result);
        validateName(manifest.getName(), result);
        validateVersion(manifest.getVersion(), result);
        validateEntryPoint(manifest.getEntryPoint(), result);
        validatePermissions(manifest.getPermissions(), result);
        validateVendor(manifest.getVendor(), result);
        validateAuthor(manifest.getAuthor(), result);

        return result;
    }

    /**
     * Validate plugin ID format.
     */
    private void validatePluginId(String pluginId, ValidationResult result) {
        if (pluginId == null || pluginId.isBlank()) {
            result.addError("Plugin ID is required");
            return;
        }

        if (!PLUGIN_ID_PATTERN.matcher(pluginId).matches()) {
            result.addError("Plugin ID must match pattern ^[a-zA-Z][a-zA-Z0-9-_]{2,50}$");
        }

        if (pluginId.contains("--")) {
            result.addWarning("Plugin ID contains consecutive hyphens");
        }
    }

    /**
     * Validate plugin name.
     */
    private void validateName(String name, ValidationResult result) {
        if (name == null || name.isBlank()) {
            result.addError("Plugin name is required");
            return;
        }

        if (name.length() < 3) {
            result.addError("Plugin name must be at least 3 characters");
        }

        if (name.length() > 100) {
            result.addError("Plugin name must not exceed 100 characters");
        }
    }

    /**
     * Validate version format.
     */
    private void validateVersion(String version, ValidationResult result) {
        if (version == null || version.isBlank()) {
            result.addError("Version is required");
            return;
        }

        if (!VERSION_PATTERN.matcher(version).matches()) {
            result.addError("Version must follow semver format (e.g., 1.0.0 or 1.0.0-beta)");
        }
    }

    /**
     * Validate entry point class.
     */
    private void validateEntryPoint(String entryPoint, ValidationResult result) {
        if (entryPoint == null || entryPoint.isBlank()) {
            result.addError("Entry point is required");
            return;
        }

        if (!entryPoint.contains(".")) {
            result.addError("Entry point must be a fully qualified class name");
        }

        String[] parts = entryPoint.split("\\.");
        if (parts.length < 3) {
            result.addWarning("Entry point should be a fully qualified class name");
        }

        for (String part : parts) {
            if (!Character.isJavaIdentifierStart(part.charAt(0))) {
                result.addError("Invalid identifier in entry point: " + part);
                break;
            }
        }
    }

    /**
     * Validate permissions string.
     */
    private void validatePermissions(String permissions, ValidationResult result) {
        if (permissions == null || permissions.isBlank()) {
            result.addWarning("No permissions declared - plugin will have minimal access");
            return;
        }

        String[] perms = permissions.split(",");
        Set<String> unknownPerms = new HashSet<>();

        for (String perm : perms) {
            String trimmed = perm.trim().toUpperCase();
            if (!ALLOWED_PERMISSIONS.contains(trimmed)) {
                unknownPerms.add(trimmed);
            }
        }

        if (!unknownPerms.isEmpty()) {
            result.addWarning("Unknown permissions (will be ignored): " + unknownPerms);
        }
    }

    /**
     * Validate vendor information.
     */
    private void validateVendor(String vendor, ValidationResult result) {
        if (vendor == null || vendor.isBlank()) {
            result.addWarning("No vendor specified");
            return;
        }

        if (vendor.length() > 100) {
            result.addWarning("Vendor name is very long");
        }
    }

    /**
     * Validate author information.
     */
    private void validateAuthor(String author, ValidationResult result) {
        if (author == null || author.isBlank()) {
            result.addWarning("No author specified");
            return;
        }

        if (author.contains("<") || author.contains(">")) {
            result.addWarning("Author contains potentially dangerous characters");
        }
    }

    /**
     * Scan plugin code for security issues.
     */
    public SecurityScanResult scanForSecurityIssues(String codeContent) {
        SecurityScanResult result = new SecurityScanResult();

        if (codeContent == null || codeContent.isBlank()) {
            return result;
        }

        for (String pattern : DANGEROUS_PATTERNS) {
            if (codeContent.contains(pattern)) {
                result.addFinding(new SecurityFinding(
                        SecurityFinding.Severity.HIGH,
                        "Dangerous pattern detected: " + pattern,
                        "Code contains a potentially dangerous pattern that could be used for code injection or file system access",
                        SecurityFinding.Category.INJECTION
                ));
            }
        }

        scanForCodeInjection(codeContent, result);
        scanForPathTraversal(codeContent, result);
        scanForCommandInjection(codeContent, result);
        scanForSQLInjection(codeContent, result);

        if (codeContent.contains("password") || codeContent.contains("secret")) {
            result.addFinding(new SecurityFinding(
                    SecurityFinding.Severity.MEDIUM,
                    "Potential credential exposure",
                    "Code may contain hardcoded credentials",
                    SecurityFinding.Category.CREDENTIALS
            ));
        }

        checkForObfuscation(codeContent, result);

        return result;
    }

    /**
     * Scan for code injection vulnerabilities.
     */
    private void scanForCodeInjection(String code, SecurityScanResult result) {
        String[] injectionPatterns = {
                "eval(",
                "groovy.lang.GroovyShell",
                "javascript:",
                "data:text/html"
        };

        for (String pattern : injectionPatterns) {
            if (code.contains(pattern)) {
                result.addFinding(new SecurityFinding(
                        SecurityFinding.Severity.CRITICAL,
                        "Code injection vulnerability",
                        "Potential code injection via " + pattern,
                        SecurityFinding.Category.INJECTION
                ));
            }
        }
    }

    /**
     * Scan for path traversal vulnerabilities.
     */
    private void scanForPathTraversal(String code, SecurityScanResult result) {
        String[] traversalPatterns = {
                "../",
                "..\\",
                "%2e%2e%2f",
                "%2e%2e/"
        };

        for (String pattern : traversalPatterns) {
            if (code.contains(pattern)) {
                result.addFinding(new SecurityFinding(
                        SecurityFinding.Severity.HIGH,
                        "Path traversal pattern detected",
                        "Potential path traversal vulnerability",
                        SecurityFinding.Category.PATH_TRAVERSAL
                ));
            }
        }
    }

    /**
     * Scan for command injection vulnerabilities.
     */
    private void scanForCommandInjection(String code, SecurityScanResult result) {
        String[] cmdPatterns = {
                "ProcessImpl",
                "getInputStream",
                "/bin/sh",
                "cmd.exe /c"
        };

        int count = 0;
        for (String pattern : cmdPatterns) {
            if (code.contains(pattern)) {
                count++;
            }
        }

        if (count >= 2) {
            result.addFinding(new SecurityFinding(
                    SecurityFinding.Severity.CRITICAL,
                    "Potential command injection",
                    "Multiple patterns suggesting command execution found",
                    SecurityFinding.Category.COMMAND_INJECTION
            ));
        }
    }

    /**
     * Scan for SQL injection patterns.
     */
    private void scanForSQLInjection(String code, SecurityScanResult result) {
        if (code.contains("executeQuery") && code.contains("+ \"")) {
            result.addFinding(new SecurityFinding(
                    SecurityFinding.Severity.HIGH,
                    "Potential SQL injection",
                    "String concatenation used in query - use parameterized queries",
                    SecurityFinding.Category.SQL_INJECTION
            ));
        }
    }

    /**
     * Check for code obfuscation indicators.
     */
    private void checkForObfuscation(String code, SecurityScanResult result) {
        long base64Count = countOccurrences(code, "base64");
        long encodedCount = countOccurrences(code, "decode");

        if (base64Count > 3 || encodedCount > 3) {
            result.addFinding(new SecurityFinding(
                    SecurityFinding.Severity.MEDIUM,
                    "Heavy encoding usage",
                    "Code contains multiple encoding operations - may be obfuscated",
                    SecurityFinding.Category.OBFUSCATION
            ));
        }

        if (code.contains("z Jep")) {
            result.addFinding(new SecurityFinding(
                    SecurityFinding.Severity.LOW,
                    "Potential obfuscation tool usage",
                    "Possible use of Java decompiler obfuscation tools",
                    SecurityFinding.Category.OBFUSCATION
            ));
        }
    }

    private long countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * Validate required permissions for a plugin operation.
     */
    public PermissionCheckResult checkPermissions(String requiredPermission, Set<String> grantedPermissions) {
        PermissionCheckResult result = new PermissionCheckResult();

        if (requiredPermission == null || requiredPermission.isBlank()) {
            result.setGranted(true);
            return result;
        }

        if (grantedPermissions == null || grantedPermissions.isEmpty()) {
            result.setGranted(false);
            result.setReason("No permissions granted");
            return result;
        }

        String normalizedRequired = requiredPermission.toUpperCase().trim();

        if (grantedPermissions.contains(normalizedRequired)) {
            result.setGranted(true);
            return result;
        }

        if (grantedPermissions.contains("ALL") || grantedPermissions.contains("ADMIN")) {
            result.setGranted(true);
            result.setReason("Admin privileges override");
            return result;
        }

        result.setGranted(false);
        result.setReason("Missing required permission: " + requiredPermission);
        return result;
    }

    /**
     * Check if plugin is compatible with the current platform version.
     */
    public CompatibilityResult checkCompatibility(String pluginVersion, String platformVersion) {
        CompatibilityResult result = new CompatibilityResult();

        if (pluginVersion == null || platformVersion == null) {
            result.setCompatible(false);
            result.setReason("Version information missing");
            return result;
        }

        try {
            VersionInfo pluginVer = parseVersion(pluginVersion);
            VersionInfo platformVer = parseVersion(platformVersion);
            VersionInfo minVer = parseVersion(MIN_COMPATIBLE_VERSION);
            VersionInfo maxVer = parseVersion(MAX_COMPATIBLE_VERSION);

            if (compareVersions(pluginVer, minVer) < 0) {
                result.setCompatible(false);
                result.setReason("Plugin requires platform version >= " + MIN_COMPATIBLE_VERSION);
                return result;
            }

            if (compareVersions(pluginVer, maxVer) > 0) {
                result.setCompatible(false);
                result.setReason("Plugin targets future platform version > " + MAX_COMPATIBLE_VERSION);
                return result;
            }

            if (pluginVer.major != platformVer.major) {
                result.setCompatible(false);
                result.setReason("Plugin major version (" + pluginVer.major +
                        ") differs from platform (" + platformVer.major + ")");
                return result;
            }

            result.setCompatible(true);
            result.addNote("Major versions match, plugin should be compatible");

        } catch (Exception e) {
            result.setCompatible(false);
            result.setReason("Could not parse version: " + e.getMessage());
        }

        return result;
    }

    /**
     * Parse version string into components.
     */
    private VersionInfo parseVersion(String version) {
        String[] parts = version.split("[-.]");
        return new VersionInfo(
                Integer.parseInt(parts[0]),
                parts.length > 1 ? Integer.parseInt(parts[1]) : 0,
                parts.length > 2 ? Integer.parseInt(parts[2]) : 0
        );
    }

    /**
     * Compare two version info objects.
     */
    private int compareVersions(VersionInfo v1, VersionInfo v2) {
        if (v1.major != v2.major) return Integer.compare(v1.major, v2.major);
        if (v1.minor != v2.minor) return Integer.compare(v1.minor, v2.minor);
        return Integer.compare(v1.patch, v2.patch);
    }

    /**
     * Perform complete validation of a plugin.
     */
    public CompleteValidationResult validatePlugin(PluginManifest manifest, String codeContent,
                                                   String platformVersion, Set<String> permissions) {
        CompleteValidationResult result = new CompleteValidationResult();

        ValidationResult manifestResult = validateManifest(manifest);
        result.setManifestValid(manifestResult.isValid());
        result.setManifestErrors(manifestResult.getErrors());
        result.setManifestWarnings(manifestResult.getWarnings());

        SecurityScanResult securityResult = scanForSecurityIssues(codeContent);
        result.setSecurityScanPassed(securityResult.isClean());
        result.setSecurityFindings(securityResult.getFindings());

        CompatibilityResult compatibilityResult = checkCompatibility(manifest.getVersion(), platformVersion);
        result.setCompatible(compatibilityResult.isCompatible());
        result.setCompatibilityReason(compatibilityResult.getReason());

        if (permissions != null) {
            for (String required : manifest.getPermissions().split(",")) {
                String trimmed = required.trim();
                if (!trimmed.isEmpty()) {
                    PermissionCheckResult permResult = checkPermissions(trimmed, permissions);
                    result.addPermissionCheck(trimmed, permResult.isGranted());
                }
            }
        }

        result.setOverallValid(
                manifestResult.isValid() &&
                securityResult.isClean() &&
                compatibilityResult.isCompatible()
        );

        return result;
    }

    // Inner classes

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public String getSummary() {
            if (errors.isEmpty() && warnings.isEmpty()) {
                return "Validation passed";
            }
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("Errors: ").append(String.join(", ", errors));
            }
            if (!warnings.isEmpty()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append("Warnings: ").append(String.join(", ", warnings));
            }
            return sb.toString();
        }
    }

    public static class SecurityScanResult {
        private final List<SecurityFinding> findings = new ArrayList<>();

        public void addFinding(SecurityFinding finding) {
            findings.add(finding);
        }

        public boolean isClean() {
            return findings.stream()
                    .noneMatch(f -> f.severity == SecurityFinding.Severity.CRITICAL ||
                                   f.severity == SecurityFinding.Severity.HIGH);
        }

        public List<SecurityFinding> getFindings() {
            return new ArrayList<>(findings);
        }

        public List<SecurityFinding> getCriticalFindings() {
            return findings.stream()
                    .filter(f -> f.severity == SecurityFinding.Severity.CRITICAL)
                    .collect(Collectors.toList());
        }
    }

    public static class SecurityFinding {
        public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
        public enum Category {
            INJECTION, PATH_TRAVERSAL, COMMAND_INJECTION, SQL_INJECTION,
            CREDENTIALS, OBFUSCATION, OTHER
        }

        public final Severity severity;
        public final String title;
        public final String description;
        public final Category category;

        public SecurityFinding(Severity severity, String title, String description, Category category) {
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.category = category;
        }
    }

    public static class PermissionCheckResult {
        private boolean granted;
        private String reason;

        public void setGranted(boolean granted) {
            this.granted = granted;
        }

        public boolean isGranted() {
            return granted;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

    public static class CompatibilityResult {
        private boolean compatible;
        private String reason;
        private List<String> notes = new ArrayList<>();

        public void setCompatible(boolean compatible) {
            this.compatible = compatible;
        }

        public boolean isCompatible() {
            return compatible;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }

        public void addNote(String note) {
            notes.add(note);
        }

        public List<String> getNotes() {
            return new ArrayList<>(notes);
        }
    }

    public static class CompleteValidationResult {
        private boolean overallValid;
        private boolean manifestValid;
        private List<String> manifestErrors = new ArrayList<>();
        private List<String> manifestWarnings = new ArrayList<>();
        private boolean securityScanPassed;
        private List<SecurityFinding> securityFindings = new ArrayList<>();
        private boolean compatible;
        private String compatibilityReason;
        private Map<String, Boolean> permissionChecks = new HashMap<>();

        public void setOverallValid(boolean valid) { this.overallValid = valid; }
        public boolean isOverallValid() { return overallValid; }

        public void setManifestValid(boolean valid) { this.manifestValid = valid; }
        public boolean isManifestValid() { return manifestValid; }

        public void setManifestErrors(List<String> errors) { this.manifestErrors = errors; }
        public List<String> getManifestErrors() { return manifestErrors; }

        public void setManifestWarnings(List<String> warnings) { this.manifestWarnings = warnings; }
        public List<String> getManifestWarnings() { return manifestWarnings; }

        public void setSecurityScanPassed(boolean passed) { this.securityScanPassed = passed; }
        public boolean isSecurityScanPassed() { return securityScanPassed; }

        public void setSecurityFindings(List<SecurityFinding> findings) { this.securityFindings = findings; }
        public List<SecurityFinding> getSecurityFindings() { return securityFindings; }

        public void setCompatible(boolean compatible) { this.compatible = compatible; }
        public boolean isCompatible() { return compatible; }

        public void setCompatibilityReason(String reason) { this.compatibilityReason = reason; }
        public String getCompatibilityReason() { return compatibilityReason; }

        public void addPermissionCheck(String permission, boolean granted) {
            permissionChecks.put(permission, granted);
        }

        public Map<String, Boolean> getPermissionChecks() { return new HashMap<>(permissionChecks); }
    }

    private static class VersionInfo {
        final int major;
        final int minor;
        final int patch;

        VersionInfo(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }
    }
}