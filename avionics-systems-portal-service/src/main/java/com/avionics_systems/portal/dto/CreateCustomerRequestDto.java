package com.avionics_systems.portal.dto;

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

    @NotNull(message = "{validation.request.portalId.required}")
    private UUID portalId;

    private UUID requestTypeId;

    @NotBlank(message = "{validation.request.summary.required}")
    private String summary;

    private String description;

    @NotBlank(message = "{validation.request.customerName.required}")
    private String customerName;

    @NotBlank(message = "{validation.request.customerEmail.required}")
    @Email(message = "{validation.request.customerEmail.invalid}")
    private String customerEmail;

    private UUID customerId;
    private String priority;
    private UUID organizationId;
    private String organizationName;
    private String fields;
    private String attachments;
    private String channel = "WEB";
}