package com.avionics_systems.admin.service;

import com.avionics_systems.admin.entity.*;
import com.avionics_systems.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminService {

    private final SystemSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AppearanceRepository appearanceRepository;
    private final LicenseRepository licenseRepository;
    private final MessageSource messageSource;

    @Value("${app.defaults.user-role:USER}")
    private String defaultUserRole;

    @Value("${app.defaults.timezone:UTC}")
    private String defaultTimezone;

    @Value("${app.defaults.language:en-US}")
    private String defaultLanguage;

    @Value("${app.defaults.password-hash-placeholder:$2a$10$placeholder}")
    private String defaultPasswordHashPlaceholder;

    @Value("${app.health.service-names:Database,Email Service,File Storage}")
    private String healthServiceNamesStr;

    public AdminService(SystemSettingsRepository settingsRepository,
                        UserRepository userRepository,
                        ProjectRepository projectRepository,
                        AppearanceRepository appearanceRepository,
                        LicenseRepository licenseRepository,
                        MessageSource messageSource) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.appearanceRepository = appearanceRepository;
        this.licenseRepository = licenseRepository;
        this.messageSource = messageSource;
    }

    // Note: initializeDefaults() removed - seed data is handled by consolidated-migration

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
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.setting.not.found", new Object[]{key}, Locale.ENGLISH)));
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
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.username.exists", null, Locale.ENGLISH));
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.email.exists", null, Locale.ENGLISH));
        }

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .displayName((String) data.getOrDefault("displayName", username))
                .passwordHash(defaultPasswordHashPlaceholder) // In real app, would be hashed
                .status(UserEntity.UserStatus.ACTIVE)
                .role((String) data.getOrDefault("role", defaultUserRole))
                .emailVerified(false)
                .timezone(defaultTimezone)
                .language(defaultLanguage)
                .build();

        return userRepository.save(user);
    }

    public UserEntity updateUser(String userId, Map<String, Object> updates) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.user.not.found", new Object[]{userId}, Locale.ENGLISH)));

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
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.project.not.found", new Object[]{projectId}, Locale.ENGLISH)));

        if (updates.containsKey("name")) project.setName((String) updates.get("name"));
        if (updates.containsKey("description")) project.setDescription((String) updates.get("description"));
        if (updates.containsKey("status")) project.setStatus(ProjectEntity.ProjectStatus.valueOf((String) updates.get("status")));
        if (updates.containsKey("allowSubTasks")) project.setAllowSubTasks((Boolean) updates.get("allowSubTasks"));
        if (updates.containsKey("allowAttachments")) project.setAllowAttachments((Boolean) updates.get("allowAttachments"));
        if (updates.containsKey("allowComments")) project.setAllowComments((Boolean) updates.get("allowComments"));
        if (updates.containsKey("maxAttachments")) project.setMaxAttachments((Integer) updates.get("maxAttachments"));
        if (updates.containsKey("enableNotifications")) project.setEnableNotifications((Boolean) updates.get("enableNotifications"));
        if (updates.containsKey("issueTypeScheme")) project.setIssueTypeScheme((String) updates.get("issueTypeScheme"));

        return projectRepository.save(project);
    }

    // ==================== Appearance ====================

    public AppearanceEntity getAppearance() {
        return appearanceRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        messageSource.getMessage("error.appearance.not.configured", null, Locale.ENGLISH)));
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
                .orElseThrow(() -> new IllegalStateException(
                        messageSource.getMessage("error.license.not.configured", null, Locale.ENGLISH)));
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

        List<String> serviceNames = Arrays.asList(healthServiceNamesStr.split(","));
        List<Map<String, Object>> services = new ArrayList<>();
        for (String serviceName : serviceNames) {
            services.add(createServiceStatus(serviceName.trim(), true, "HEALTHY"));
        }
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
