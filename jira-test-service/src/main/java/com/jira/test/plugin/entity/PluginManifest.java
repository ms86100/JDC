package com.jira.test.plugin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a plugin's manifest metadata.
 * Contains all information about an installed or pending plugin.
 */
@Entity
@Table(name = "plugin_manifests")
public class PluginManifest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pluginId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Column(length = 2000)
    private String description;

    private String author;

    private String vendor;

    @Column(nullable = false)
    private String entryPoint;

    @Column(columnDefinition = "TEXT")
    private String permissions;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(nullable = false)
    private LocalDateTime installedAt;

    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PluginStatus status = PluginStatus.PENDING;

    @Column(nullable = false)
    private String projectId;

    private String minPlatformVersion;
    private String maxPlatformVersion;

    public enum PluginStatus {
        PENDING,
        INSTALLED,
        ENABLED,
        DISABLED,
        ERROR
    }

    public PluginManifest() {
        this.installedAt = LocalDateTime.now();
    }

    public PluginManifest(String pluginId, String name, String version, String entryPoint, String projectId) {
        this.pluginId = pluginId;
        this.name = name;
        this.version = version;
        this.entryPoint = entryPoint;
        this.projectId = projectId;
        this.installedAt = LocalDateTime.now();
        this.enabled = false;
        this.status = PluginStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(LocalDateTime installedAt) {
        this.installedAt = installedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.enabled = true;
        this.status = PluginStatus.ENABLED;
        markUpdated();
    }

    public void disable() {
        this.enabled = false;
        this.status = PluginStatus.DISABLED;
        markUpdated();
    }

    public void markError() {
        this.enabled = false;
        this.status = PluginStatus.ERROR;
        markUpdated();
    }

    public void markInstalled() {
        this.status = PluginStatus.INSTALLED;
        markUpdated();
    }

    public String getMinPlatformVersion() {
        return minPlatformVersion;
    }

    public void setMinPlatformVersion(String minPlatformVersion) {
        this.minPlatformVersion = minPlatformVersion;
    }

    public String getMaxPlatformVersion() {
        return maxPlatformVersion;
    }

    public void setMaxPlatformVersion(String maxPlatformVersion) {
        this.maxPlatformVersion = maxPlatformVersion;
    }
}
