package com.avionics_systems.admin.controller;

import com.avionics_systems.admin.entity.ApplicationAccessEntity;
import com.avionics_systems.admin.service.ApplicationAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/application-access")
@RequiredArgsConstructor
@Tag(name = "Application Access", description = "Application Access Control API")
public class ApplicationAccessController {

    private final ApplicationAccessService applicationAccessService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get application access for a user")
    public ResponseEntity<List<ApplicationAccessEntity>> getUserAccess(@PathVariable UUID userId) {
        return ResponseEntity.ok(applicationAccessService.getUserAccess(userId));
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/{userId}")
    @Operation(summary = "Update application access for a user")
    public ResponseEntity<List<ApplicationAccessEntity>> updateUserAccess(
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> body) {
        List<String> applicationKeys = (List<String>) body.get("applicationKeys");
        UUID grantedBy = body.containsKey("grantedBy")
                ? UUID.fromString((String) body.get("grantedBy"))
                : null;

        List<ApplicationAccessEntity> updated = applicationAccessService.updateUserAccess(
                userId, applicationKeys, grantedBy);
        return ResponseEntity.ok(updated);
    }
}
