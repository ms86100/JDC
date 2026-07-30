package com.avionics_systems.portal.controller;

import com.avionics_systems.portal.dto.*;
import com.avionics_systems.portal.service.PortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/portals")
@RequiredArgsConstructor
@Tag(name = "Portals", description = "Customer Portal management endpoints")
public class PortalController {

    private final PortalService portalService;
    private final MessageSource messageSource;

    // Portal Management
    @PostMapping
    @Operation(summary = "Create a portal", description = "Creates a new customer portal")
    public ResponseEntity<CustomerPortalResponse> createPortal(
            @Valid @RequestBody CreatePortalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.createPortal(request, actor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get portal by ID", description = "Returns a specific portal")
    public ResponseEntity<CustomerPortalResponse> getPortal(
            @Parameter(description = "Portal ID") @PathVariable UUID id) {
        return ResponseEntity.ok(portalService.getPortal(id));
    }

    @GetMapping("/key/{portalKey}")
    @Operation(summary = "Get portal by key", description = "Returns a portal by its key")
    public ResponseEntity<CustomerPortalResponse> getPortalByKey(
            @Parameter(description = "Portal Key") @PathVariable String portalKey) {
        return ResponseEntity.ok(portalService.getPortalByKey(portalKey));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get project portals", description = "Returns all portals for a project")
    public ResponseEntity<List<CustomerPortalResponse>> getProjectPortals(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {
        return ResponseEntity.ok(portalService.getPortalsByProject(projectId));
    }

    @GetMapping("/public")
    @Operation(summary = "Get public portals", description = "Returns all public portals")
    public ResponseEntity<List<CustomerPortalResponse>> getPublicPortals() {
        return ResponseEntity.ok(portalService.getPublicPortals());
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish portal", description = "Publishes a portal")
    public ResponseEntity<CustomerPortalResponse> publishPortal(
            @Parameter(description = "Portal ID") @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        return ResponseEntity.ok(portalService.publishPortal(id, actor));
    }

    // Customer Request Management
    @PostMapping("/requests")
    @Operation(summary = "Submit customer request", description = "Creates a new customer request")
    public ResponseEntity<CustomerRequestResponse> createCustomerRequest(
            @Valid @RequestBody CreateCustomerRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.createCustomerRequest(request));
    }

    @GetMapping("/requests/{id}")
    @Operation(summary = "Get request by ID", description = "Returns a specific customer request")
    public ResponseEntity<CustomerRequestResponse> getCustomerRequest(
            @Parameter(description = "Request ID") @PathVariable UUID id) {
        return ResponseEntity.ok(portalService.getCustomerRequest(id));
    }

    @GetMapping("/requests/key/{requestKey}")
    @Operation(summary = "Get request by key", description = "Returns a request by its key")
    public ResponseEntity<CustomerRequestResponse> getCustomerRequestByKey(
            @Parameter(description = "Request Key") @PathVariable String requestKey) {
        return ResponseEntity.ok(portalService.getCustomerRequestByKey(requestKey));
    }

    @GetMapping("/{portalId}/requests")
    @Operation(summary = "Get portal requests", description = "Returns all requests for a portal")
    public ResponseEntity<Page<CustomerRequestResponse>> getPortalRequests(
            @Parameter(description = "Portal ID") @PathVariable UUID portalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.portal.defaults.page-size:20}") int size) {
        return ResponseEntity.ok(portalService.getRequestsByPortal(portalId, PageRequest.of(page, size)));
    }

    @PatchMapping("/requests/{id}/status")
    @Operation(summary = "Update request status", description = "Updates the status of a customer request")
    public ResponseEntity<CustomerRequestResponse> updateRequestStatus(
            @Parameter(description = "Request ID") @PathVariable UUID id,
            @RequestParam String status,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        return ResponseEntity.ok(portalService.updateRequestStatus(id, status, actor));
    }

    // Request Type Management
    @PostMapping("/{portalId}/request-types")
    @Operation(summary = "Create request type", description = "Creates a new request type for a portal")
    public ResponseEntity<RequestTypeResponse> createRequestType(
            @Parameter(description = "Portal ID") @PathVariable UUID portalId,
            @Valid @RequestBody CreateRequestTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.createRequestType(portalId, request));
    }

    @GetMapping("/{portalId}/request-types")
    @Operation(summary = "Get portal request types", description = "Returns all request types for a portal")
    public ResponseEntity<List<RequestTypeResponse>> getRequestTypes(
            @Parameter(description = "Portal ID") @PathVariable UUID portalId) {
        return ResponseEntity.ok(portalService.getRequestTypesByPortal(portalId));
    }

    // Comment Management
    @PostMapping("/requests/{requestId}/comments")
    @Operation(summary = "Add comment", description = "Adds a comment to a customer request")
    public ResponseEntity<PortalCommentResponse> addComment(
            @Parameter(description = "Request ID") @PathVariable UUID requestId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.addComment(requestId, request));
    }

    @GetMapping("/requests/{requestId}/comments")
    @Operation(summary = "Get request comments", description = "Returns all comments for a request")
    public ResponseEntity<List<PortalCommentResponse>> getRequestComments(
            @Parameter(description = "Request ID") @PathVariable UUID requestId,
            @RequestParam(defaultValue = "false") boolean includeInternal) {
        return ResponseEntity.ok(portalService.getRequestComments(requestId, includeInternal));
    }
}