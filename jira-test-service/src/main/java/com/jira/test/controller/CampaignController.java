package com.jira.test.controller;

import com.jira.test.dto.CampaignCreationResponse;
import com.jira.test.service.CampaignAutomationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaign Automation", description = "Test campaign creation from LTR/CSV")
public class CampaignController {

    private final CampaignAutomationService campaignService;

    @PostMapping("/create-from-csv/{testPlanId}")
    @Operation(summary = "Create test campaign from LTR CSV file")
    public ResponseEntity<CampaignCreationResponse> createFromCsv(
            @PathVariable UUID testPlanId,
            @RequestBody String csvContent) {
        return ResponseEntity.ok(campaignService.createCampaignFromCsv(testPlanId, csvContent));
    }
}
