package com.jira.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequestDto {

    @NotNull(message = "Portal ID is required")
    private UUID portalId;

    private UUID requestTypeId;

    @NotBlank(message = "Summary is required")
    private String summary;

    private String description;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;

    private UUID customerId;
    private String priority;
    private UUID organizationId;
    private String organizationName;
    private String fields;
    private String attachments;
    private String channel = "WEB";
}