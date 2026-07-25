package com.jira.auth.security;

import com.jira.auth.service.SamlConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SamlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final SamlConfigService samlConfigService;

    @Value("${app.defaults.default-registration-id:default}")
    private String defaultRegistrationId;

    @Value("${app.saml.error-redirect:/auth/login?error=saml_failed}")
    private String samlErrorRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        Object principal = authentication.getPrincipal();
        String nameId;
        String registrationId;
        Map<String, String> attributes = new HashMap<>();

        try {
            var samlPrincipalClass = Class.forName(
                    "org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal");
            if (samlPrincipalClass.isInstance(principal)) {
                nameId = (String) samlPrincipalClass.getMethod("getName").invoke(principal);
                registrationId = (String) samlPrincipalClass.getMethod("getRelyingPartyRegistrationId").invoke(principal);
                @SuppressWarnings("unchecked")
                Map<String, Object> rawAttrs = (Map<String, Object>) samlPrincipalClass.getMethod("getAttributes").invoke(principal);
                if (rawAttrs != null) {
                    rawAttrs.forEach((key, values) -> {
                        if (values instanceof java.util.List<?> list && !list.isEmpty()) {
                            attributes.put(key, list.get(0).toString());
                        }
                    });
                }
            } else {
                nameId = authentication.getName();
                registrationId = defaultRegistrationId;
            }
        } catch (ClassNotFoundException e) {
            nameId = authentication.getName();
            registrationId = defaultRegistrationId;
        } catch (Exception e) {
            log.error("Failed to extract SAML principal: {}", e.getMessage());
            response.sendRedirect(samlErrorRedirect);
            return;
        }

        log.info("SAML authentication success: nameId={}, registrationId={}", nameId, registrationId);

        try {
            Map<String, Object> authResult = samlConfigService.authenticateSamlUser(
                    nameId, registrationId, attributes);

            String frontendUrl = buildRedirectUrl(request, authResult);
            response.sendRedirect(frontendUrl);
        } catch (Exception e) {
            log.error("SAML user provisioning failed for nameId={}: {}", nameId, e.getMessage());
            response.sendRedirect(samlErrorRedirect);
        }
    }

    private String buildRedirectUrl(HttpServletRequest request, Map<String, Object> authResult) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            baseUrl += ":" + port;
        }
        return baseUrl + "/auth/saml/callback"
                + "?token=" + authResult.get("accessToken")
                + "&refreshToken=" + authResult.get("refreshToken");
    }
}
