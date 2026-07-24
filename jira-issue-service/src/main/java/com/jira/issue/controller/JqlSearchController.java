package com.jira.issue.controller;

import com.jira.issue.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jql")
@RequiredArgsConstructor
@Tag(name = "JQL Search", description = "Unified JQL search contract (GET and POST)")
public class JqlSearchController {

    private final IssueService issueService;

    @PostMapping("/search")
    @Operation(summary = "JQL search (POST)", description = "Same response shape as GET /api/issues/search?jql= — for large queries")
    public ResponseEntity<Map<String, Object>> searchPost(
            @RequestBody JqlSearchBody body,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        String jql = body != null && body.getJql() != null ? body.getJql() : "";
        int page = body != null && body.getPage() != null ? body.getPage() : 0;
        int pageSize = body != null && body.getPageSize() != null ? body.getPageSize() : 50;
        return ResponseEntity.ok(issueService.searchByJql(jql, page, pageSize));
    }

    @lombok.Data
    public static class JqlSearchBody {
        private String jql;
        private Integer page;
        private Integer pageSize;
    }
}
