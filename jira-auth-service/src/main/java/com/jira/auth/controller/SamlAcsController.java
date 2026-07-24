package com.jira.auth.controller;

import com.jira.auth.security.SamlResponseHandler;
import com.jira.auth.security.SamlResponseHandler.SamlAssertionResult;
import com.jira.auth.service.SamlConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SamlAcsController {

    private final SamlResponseHandler samlResponseHandler;
    private final SamlConfigService samlConfigService;

    @PostMapping("/saml2/acs/{registrationId}")
    public ResponseEntity<?> assertionConsumerService(
            @PathVariable String registrationId,
            @RequestParam("SAMLResponse") String samlResponse) {
        log.info("SAML ACS received for registration: {}", registrationId);

        SamlAssertionResult result = samlResponseHandler.parseResponse(samlResponse);

        if (!result.success()) {
            log.error("SAML assertion failed: {}", result.errorMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "saml_authentication_failed",
                    "message", result.errorMessage()));
        }

        try {
            Map<String, Object> authResult = samlConfigService.authenticateSamlUser(
                    result.nameId(), registrationId, result.attributes());

            return ResponseEntity.ok(authResult);
        } catch (Exception e) {
            log.error("SAML user provisioning failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "saml_provisioning_failed",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/saml2/metadata/{registrationId}")
    public ResponseEntity<Map<String, Object>> getSpMetadata(@PathVariable String registrationId) {
        try {
            var config = samlConfigService.getByRegistrationId(registrationId);
            return ResponseEntity.ok(Map.of(
                    "entityId", config.getSpEntityId() != null ? config.getSpEntityId() : config.getEntityId(),
                    "acsUrl", "/saml2/acs/" + registrationId,
                    "sloUrl", "/saml2/slo/" + registrationId,
                    "nameIdFormat", "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                    "registrationId", registrationId
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/saml2/login/{registrationId}")
    public ResponseEntity<Map<String, Object>> initiateLogin(@PathVariable String registrationId) {
        try {
            var config = samlConfigService.getByRegistrationId(registrationId);
            if (!config.getEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("error", "SAML configuration is disabled"));
            }
            return ResponseEntity.ok(Map.of(
                    "idpSsoUrl", config.getIdpSsoUrl(),
                    "entityId", config.getEntityId(),
                    "acsUrl", "/saml2/acs/" + registrationId,
                    "forceAuthn", config.getForceAuthn()
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
