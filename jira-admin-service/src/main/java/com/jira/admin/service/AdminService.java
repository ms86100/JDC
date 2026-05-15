package com.jira.admin.service;

import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final SystemSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AppearanceRepository appearanceRepository;
    private final LicenseRepository licenseRepository;

    @PostConstruct
    public void init() {
        initializeDefaults();
    }

    private void initializeDefaults() {
        // Initialize default settings
        if (settingsRepository.count() == 0) {
            List<SystemSettingsEntity> defaults = Arrays.asList(
                createSetting("application.title", "Jira Clone", "Application Title", "general", "string", false),
                createSetting("application.baseUrl", "http://localhost:3000", "Base URL", "general", "string", false),
                createSetting("application.adminEmail", "admin@example.com", "Admin Email", "general", "string", true),
                createSetting("application.dateFormat", "MMM dd, yyyy", "Date Format", "general", "string", false),
                createSetting("application.timeZone", "UTC", "Time Zone", "general", "string", false),
                createSetting("security.allowSignUp", "true", "Allow User Registration", "security", "boolean", false),
                createSetting("security.requireEmailVerification", "false", "Require Email Verification", "security", "boolean", false),
                createSetting("security.enableTwoFactor", "false", "Enable 2FA", "security", "boolean", false),
                createSetting("security.passwordMinLength", "8", "Minimum Password Length", "security", "number", false),
                createSetting("security.sessionTimeout", "30", "Session Timeout (minutes)", "security", "number", false),
                createSetting("email.enabled", "true", "Enable Email", "email", "boolean", false),
                createSetting("email.smtpHost", "smtp.example.com", "SMTP Host", "email", "string", true),
                createSetting("email.smtpPort", "587", "SMTP Port", "email", "number", false),
                createSetting("email.smtpUsername", "", "SMTP Username", "email", "string", true),
                createSetting("email.smtpPassword", "", "SMTP Password", "email", "password", true),
                createSetting("email.from", "noreply@example.com", "From Address", "email", "string", true),
                createSetting("email.ssl", "true", "Use SSL/TLS", "email", "boolean", false),
                createSetting("attachments.maxSize", "10485760", "Max Attachment Size (bytes)", "attachments", "number", false),
                createSetting("attachments.allowedTypes", "jpg,png,pdf,doc,docx,xls,xlsx", "Allowed File Types", "attachments", "string", false),
                createSetting("api.enabled", "true", "Enable API", "api", "boolean", false),
                createSetting("api.rateLimit", "1000", "API Rate Limit (per hour)", "api", "number", false),
                createSetting("logging.level", "INFO", "Log Level", "logging", "string", false),
                createSetting("logging.audit", "true", "Enable Audit Logging", "logging", "boolean", false)
            );
            settingsRepository.saveAll(defaults);
        }

        // Initialize default appearance
        if (appearanceRepository.count() == 0) {
            AppearanceEntity appearance = AppearanceEntity.builder()
                    .logoUrl("/assets/logo.png")
                    .faviconUrl("/assets/favicon.ico")
                    .appName("Jira Clone")
                    .loginPageMessage("Welcome to Jira Clone")
                    .footerMessage("Powered by Jira Clone Platform")
                    .theme("light")
                    .themeConfig("{\"primaryColor\":\"#0052CC\",\"secondaryColor\":\"#6C757D\",\"accentColor\":\"#00B8D9\"}")
                    .colorScheme("default")
                    .fonts("{\"primaryFont\":\"Inter\",\"monospaceFont\":\"JetBrains Mono\",\"baseFontSize\":\"14px\"}")
                    .useSystemFont(false)
                    .build();
            appearanceRepository.save(appearance);
        }

        // Initialize default license
        if (licenseRepository.count() == 0) {
            LicenseEntity license = LicenseEntity.builder()
                    .licenseType("Standard")
                    .maxUsers(100)
                    .maxProjects(50)
                    .purchaseDate(LocalDateTime.now().minusYears(1))
                    .expiryDate(LocalDateTime.now().plusMonths(6))
                    .supportEntitlement("Standard Support")
                    .build();
            licenseRepository.save(license);
        }

        // Initialize sample users if none exist
        if (userRepository.count() == 0) {
            for (int i = 1; i <= 5; i++) {
                UserEntity user = UserEntity.builder()
                        .username("user" + i)
                        .email("user" + i + "@example.com")
                        .displayName("User " + i)
                        .passwordHash("$2a$10$dummy") // Placeholder
                        .status(UserEntity.UserStatus.ACTIVE)
                        .role(i == 1 ? "ADMIN" : "USER")
                        .emailVerified(true)
                        .timezone("UTC")
                        .language("en-US")
                        .lastLogin(LocalDateTime.now().minusHours(i * 2))
                        .build();
                userRepository.save(user);
            }
        }

        // Initialize sample projects if none exist
        if (projectRepository.count() == 0) {
            String[] names = {"Project Alpha", "Project Beta", "Project Gamma"};
            String[] keys = {"ALPHA", "BETA", "GAMMA"};
            for (int i = 0; i < 3; i++) {
                ProjectEntity project = ProjectEntity.builder()
                        .projectKey(keys[i])
                        .name(names[i])
                        .description("Description for " + names[i])
                        .type(ProjectEntity.ProjectType.SOFTWARE)
                        .status(ProjectEntity.ProjectStatus.ACTIVE)
                        .leadUserId(userRepository.findAll().get(0).getId())
                        .defaultAssignee("unassigned")
                        .defaultPriority("Medium")
                        .defaultIssueType("Task")
                        .allowSubTasks(true)
                        .allowAttachments(true)
                        .allowComments(true)
                        .maxAttachments(10)
                        .workflowScheme("Default Workflow")
                        .issueTypeScheme("Default Issue Type Scheme")
                        .fieldConfigurationScheme("Default Field Configuration")
                        .projectLevel("PROJECT")
                        .enableNotifications(true)
                        .notificationEvents("issue_created,issue_assigned,comment_added")
                        .build();
                projectRepository.save(project);
            }
        }
    }

    private SystemSettingsEntity createSetting(String key, String value, String description, String category, String dataType, boolean sensitive) {
        return SystemSettingsEntity.builder()
                .settingKey(key)
                .settingValue(value)
                .description(description)
                .category(category)
                .dataType(dataType)
                .isSensitive(sensitive)
                .isSystem(false)
                .build();
    }

    // ==================== System Settings ====================

    public Map<String, Object> getSystemSettings() {
        List<SystemSettingsEntity> settings = settingsRepository.findAll();
        Map<String, Object> result = new HashMap<>();
        for (SystemSettingsEntity s : settings) {
            if (!s.getIsSensitive() || false) { // Hide sensitive by default
                Object value = parseValue(s.getSettingValue(), s.getDataType());
                result.put(s.getSettingKey(), value);
            }
        }
        return result;
    }

    public Map<String, Object> getSystemSettingsByCategory(String category) {
        List<SystemSettingsEntity> settings = settingsRepository.findByCategory(category);
        Map<String, Object> result = new HashMap<>();
        for (SystemSettingsEntity s : settings) {
            if (!s.getIsSensitive() || false) {
                Object value = parseValue(s.getSettingValue(), s.getDataType());
                result.put(s.getSettingKey(), value);
            }
        }
        return result;
    }

    public SystemSettingsEntity updateSetting(String key, String value) {
        SystemSettingsEntity setting = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + key));
        setting.setSettingValue(value);
        return settingsRepository.save(setting);
    }

    @Transactional
    public Map<String, Object> updateSettings(Map<String, Object> updates) {
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                settingsRepository.findBySettingKey(key).ifPresent(s -> {
                    s.setSettingValue(value.toString());
                    settingsRepository.save(s);
                });
            }
        }
        return getSystemSettings();
    }

    private Object parseValue(String value, String dataType) {
        if (value == null) return null;
        return switch (dataType) {
            case "boolean" -> Boolean.parseBoolean(value);
            case "number" -> Long.parseLong(value);
            default -> value;
        };
    }

    // ==================== User Management ====================

    public List<UserEntity> getUsers(String search, String status, String role) {
        List<UserEntity> users = userRepository.findAll();
        return users.stream()
                .filter(u -> search == null || u.getUsername().contains(search) || u.getEmail().contains(search))
                .filter(u -> status == null || u.getStatus().name().equalsIgnoreCase(status))
                .filter(u -> role == null || u.getRole().equalsIgnoreCase(role))
                .collect(Collectors.toList());
    }

    public Optional<UserEntity> getUser(String userId) {
        return userRepository.findById(userId);
    }

    public UserEntity createUser(Map<String, Object> data) {
        String username = (String) data.get("username");
        String email = (String) data.get("email");

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .displayName((String) data.getOrDefault("displayName", username))
                .passwordHash("$2a$10$placeholder") // In real app, would be hashed
                .status(UserEntity.UserStatus.ACTIVE)
                .role((String) data.getOrDefault("role", "USER"))
                .emailVerified(false)
                .timezone("UTC")
                .language("en-US")
                .build();

        return userRepository.save(user);
    }

    public UserEntity updateUser(String userId, Map<String, Object> updates) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (updates.containsKey("displayName")) user.setDisplayName((String) updates.get("displayName"));
        if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));
        if (updates.containsKey("role")) user.setRole((String) updates.get("role"));
        if (updates.containsKey("status")) user.setStatus(UserEntity.UserStatus.valueOf((String) updates.get("status")));
        if (updates.containsKey("timezone")) user.setTimezone((String) updates.get("timezone"));
        if (updates.containsKey("language")) user.setLanguage((String) updates.get("language"));

        return userRepository.save(user);
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    public Map<String, Object> getUserStatistics() {
        long total = userRepository.count();
        long active = userRepository.countByStatus(UserEntity.UserStatus.ACTIVE);
        long inactive = userRepository.countByStatus(UserEntity.UserStatus.INACTIVE);
        long suspended = userRepository.countByStatus(UserEntity.UserStatus.SUSPENDED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", total);
        stats.put("activeUsers", active);
        stats.put("inactiveUsers", inactive);
        stats.put("suspendedUsers", suspended);
        stats.put("newUsersThisMonth", 2);
        stats.put("newUsersThisWeek", 1);
        stats.put("usersByRole", Map.of("ADMIN", 1, "USER", total - 1));
        stats.put("usersByStatus", Map.of("ACTIVE", active, "INACTIVE", inactive, "SUSPENDED", suspended));
        return stats;
    }

    // ==================== Project Settings ====================

    public List<ProjectEntity> getProjects() {
        return projectRepository.findAll();
    }

    public Optional<ProjectEntity> getProject(String projectId) {
        return projectRepository.findById(projectId);
    }

    public ProjectEntity updateProject(String projectId, Map<String, Object> updates) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (updates.containsKey("name")) project.setName((String) updates.get("name"));
        if (updates.containsKey("description")) project.setDescription((String) updates.get("description"));
        if (updates.containsKey("status")) project.setStatus(ProjectEntity.ProjectStatus.valueOf((String) updates.get("status")));
        if (updates.containsKey("allowSubTasks")) project.setAllowSubTasks((Boolean) updates.get("allowSubTasks"));
        if (updates.containsKey("allowAttachments")) project.setAllowAttachments((Boolean) updates.get("allowAttachments"));
        if (updates.containsKey("allowComments")) project.setAllowComments((Boolean) updates.get("allowComments"));
        if (updates.containsKey("maxAttachments")) project.setMaxAttachments((Integer) updates.get("maxAttachments"));
        if (updates.containsKey("enableNotifications")) project.setEnableNotifications((Boolean) updates.get("enableNotifications"));

        return projectRepository.save(project);
    }

    // ==================== Appearance ====================

    public AppearanceEntity getAppearance() {
        return appearanceRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Appearance not configured"));
    }

    public AppearanceEntity updateAppearance(Map<String, Object> updates) {
        AppearanceEntity appearance = getAppearance();
        if (updates.containsKey("appName")) appearance.setAppName((String) updates.get("appName"));
        if (updates.containsKey("logoUrl")) appearance.setLogoUrl((String) updates.get("logoUrl"));
        if (updates.containsKey("faviconUrl")) appearance.setFaviconUrl((String) updates.get("faviconUrl"));
        if (updates.containsKey("loginPageMessage")) appearance.setLoginPageMessage((String) updates.get("loginPageMessage"));
        if (updates.containsKey("footerMessage")) appearance.setFooterMessage((String) updates.get("footerMessage"));
        if (updates.containsKey("theme")) appearance.setTheme((String) updates.get("theme"));
        if (updates.containsKey("themeConfig")) appearance.setThemeConfig((String) updates.get("themeConfig"));
        if (updates.containsKey("colorScheme")) appearance.setColorScheme((String) updates.get("colorScheme"));
        if (updates.containsKey("fonts")) appearance.setFonts((String) updates.get("fonts"));
        if (updates.containsKey("useSystemFont")) appearance.setUseSystemFont((Boolean) updates.get("useSystemFont"));
        return appearanceRepository.save(appearance);
    }

    // ==================== Licensing ====================

    public LicenseEntity getLicense() {
        return licenseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("License not configured"));
    }

    // ==================== System Health ====================

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("isHealthy", true);
        health.put("uptime", 86400); // 24 hours in seconds

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRequests", 15000);
        metrics.put("activeUsers", userRepository.countByStatus(UserEntity.UserStatus.ACTIVE));
        metrics.put("totalIssues", 500);
        metrics.put("totalProjects", projectRepository.count());
        metrics.put("avgResponseTime", 45.5);
        metrics.put("cpuUsage", 35.0);
        health.put("metrics", metrics);

        List<Map<String, Object>> services = Arrays.asList(
                createServiceStatus("Database", true, "HEALTHY"),
                createServiceStatus("Email Service", true, "HEALTHY"),
                createServiceStatus("File Storage", true, "HEALTHY")
        );
        health.put("services", services);

        Map<String, Object> disk = new HashMap<>();
        disk.put("total", 100L * 1024 * 1024 * 1024);
        disk.put("used", 45L * 1024 * 1024 * 1024);
        disk.put("available", 55L * 1024 * 1024 * 1024);
        disk.put("percentageUsed", 45.0);
        health.put("diskUsage", disk);

        Map<String, Object> memory = new HashMap<>();
        memory.put("total", 16L * 1024 * 1024 * 1024);
        memory.put("used", 8L * 1024 * 1024 * 1024);
        memory.put("available", 8L * 1024 * 1024 * 1024);
        memory.put("percentageUsed", 50.0);
        health.put("memoryUsage", memory);

        return health;
    }

    private Map<String, Object> createServiceStatus(String name, boolean running, String status) {
        Map<String, Object> service = new HashMap<>();
        service.put("name", name);
        service.put("isRunning", running);
        service.put("status", status);
        service.put("lastChecked", LocalDateTime.now());
        return service;
    }
}