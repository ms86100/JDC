package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appearance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppearanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "logo_url")
    private String logoUrl = "/assets/logo.png";

    @Column(name = "favicon_url")
    private String faviconUrl = "/assets/favicon.ico";

    @Column(name = "app_name")
    private String appName = "Jira Clone";

    @Column(name = "login_page_message")
    private String loginPageMessage = "Welcome to Jira Clone";

    @Column(name = "footer_message")
    private String footerMessage = "Powered by Jira Clone Platform";

    private String theme = "light";

    @Column(name = "theme_config", columnDefinition = "TEXT")
    private String themeConfig;

    @Column(name = "color_scheme")
    private String colorScheme = "default";

    @Column(columnDefinition = "TEXT")
    private String fonts;

    @Column(name = "use_system_font")
    private Boolean useSystemFont = false;
}