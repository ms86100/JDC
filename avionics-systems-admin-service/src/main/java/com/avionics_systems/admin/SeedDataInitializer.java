package com.avionics_systems.admin;

import com.avionics_systems.admin.entity.*;
import com.avionics_systems.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeedDataInitializer implements CommandLineRunner {

    private final SystemSettingsRepository settingsRepository;
    private final AppearanceRepository appearanceRepository;
    private final LicenseRepository licenseRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeSystemSettings();
        initializeAppearance();
        initializeLicense();
    }

    private void initializeSystemSettings() {
        if (settingsRepository.count() > 0) return;

        log.info("Initializing default system settings...");

        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("application.title").settingValue("Avionics Systems Platform").description("Application title").category("general").dataType("string").isSensitive(false).isSystem(true).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("application.baseUrl").settingValue("http://localhost:8080").description("Base URL").category("general").dataType("string").isSensitive(false).isSystem(true).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("application.timeZone").settingValue("UTC").description("Default time zone").category("general").dataType("string").isSensitive(false).isSystem(false).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("application.dateFormat").settingValue("MMM dd, yyyy").description("Date format").category("general").dataType("string").isSensitive(false).isSystem(false).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("security.allowSignUp").settingValue("true").description("Allow user registration").category("security").dataType("boolean").isSensitive(false).isSystem(true).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("security.passwordMinLength").settingValue("8").description("Minimum password length").category("security").dataType("number").isSensitive(false).isSystem(false).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("security.sessionTimeout").settingValue("30").description("Session timeout in minutes").category("security").dataType("number").isSensitive(false).isSystem(false).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("api.enabled").settingValue("true").description("Enable REST API").category("api").dataType("boolean").isSensitive(false).isSystem(true).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("logging.level").settingValue("INFO").description("Log level").category("logging").dataType("string").isSensitive(false).isSystem(false).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("logging.audit").settingValue("true").description("Enable audit logging").category("logging").dataType("boolean").isSensitive(false).isSystem(false).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("attachments.maxSize").settingValue("10485760").description("Max attachment size in bytes").category("attachments").dataType("number").isSensitive(false).isSystem(false).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("mail.smtp.host").settingValue("localhost").description("SMTP host").category("mail").dataType("string").isSensitive(false).isSystem(false).build());
        settingsRepository.save(SystemSettingsEntity.builder()
                .settingKey("mail.smtp.port").settingValue("25").description("SMTP port").category("mail").dataType("number").isSensitive(false).isSystem(false).build());

        log.info("Initialized {} system settings", settingsRepository.count());
    }

    private void initializeAppearance() {
        if (appearanceRepository.count() > 0) return;

        log.info("Initializing default appearance settings...");

        AppearanceEntity appearance = AppearanceEntity.builder()
                .appName("Avionics Systems Platform")
                .theme("light")
                .colorScheme("blue")
                .logoUrl("")
                .faviconUrl("")
                .build();
        appearanceRepository.save(appearance);

        log.info("Initialized default appearance settings");
    }

    private void initializeLicense() {
        if (licenseRepository.count() > 0) return;

        log.info("Initializing default license...");

        LicenseEntity license = LicenseEntity.builder()
                .licenseType("evaluation")
                .licenseKey("EVL-AVIONICS-SYSTEMS-2026-XXXX-XXXX-XXXX")
                .expiryDate(LocalDateTime.now().plusDays(30))
                .maxUsers(100)
                .build();
        licenseRepository.save(license);

        log.info("Initialized default license");
    }
}