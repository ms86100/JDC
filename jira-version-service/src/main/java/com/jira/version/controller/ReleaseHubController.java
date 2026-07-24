package com.jira.version.controller;

import com.jira.version.service.ReleaseHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/versions")
@RequiredArgsConstructor
@Slf4j
public class ReleaseHubController {

    private final ReleaseHubService releaseHubService;

    @GetMapping("/{versionId}/release-hub")
    public ResponseEntity<Map<String, Object>> getReleaseStatus(@PathVariable UUID versionId) {
        return ResponseEntity.ok(releaseHubService.getReleaseStatus(versionId));
    }

    @GetMapping("/{versionId}/release-hub/warnings")
    public ResponseEntity<Map<String, Object>> getReleaseWarnings(@PathVariable UUID versionId) {
        return ResponseEntity.ok(releaseHubService.getReleaseWarnings(versionId));
    }
}
