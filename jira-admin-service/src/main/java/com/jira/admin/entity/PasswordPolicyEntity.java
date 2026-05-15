package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "password_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "min_length")
    private Integer minLength = 8;

    @Column(name = "max_length")
    private Integer maxLength = 128;

    @Column(name = "require_uppercase")
    private Boolean requireUppercase = true;

    @Column(name = "require_lowercase")
    private Boolean requireLowercase = true;

    @Column(name = "require_digit")
    private Boolean requireDigit = true;

    @Column(name = "require_special")
    private Boolean requireSpecial = false;

    @Column(name = "prevent_reuse")
    private Integer preventReuse = 5;

    @Column(name = "expire_days")
    private Integer expireDays = 90;

    @Column(name = "lockout_attempts")
    private Integer lockoutAttempts = 5;

    @Column(name = "lockout_duration")
    private Integer lockoutDuration = 30;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}