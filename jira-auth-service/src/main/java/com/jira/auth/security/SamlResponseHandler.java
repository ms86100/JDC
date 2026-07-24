package com.jira.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SamlResponseHandler {

    public SamlAssertionResult parseResponse(String samlResponseBase64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(samlResponseBase64);
            String xml = new String(decoded, StandardCharsets.UTF_8);
            return parseXml(xml);
        } catch (Exception e) {
            log.error("Failed to parse SAML response: {}", e.getMessage());
            return SamlAssertionResult.failure("Failed to parse SAML response: " + e.getMessage());
        }
    }

    private SamlAssertionResult parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Element root = doc.getDocumentElement();

        String status = extractStatus(doc);
        if (!"urn:oasis:names:tc:SAML:2.0:status:Success".equals(status)) {
            return SamlAssertionResult.failure("SAML authentication failed with status: " + status);
        }

        String nameId = extractNameId(doc);
        if (nameId == null || nameId.isBlank()) {
            return SamlAssertionResult.failure("No NameID found in SAML assertion");
        }

        Map<String, String> attributes = extractAttributes(doc);
        String issuer = extractIssuer(doc);
        String sessionIndex = extractSessionIndex(doc);

        return SamlAssertionResult.success(nameId, issuer, sessionIndex, attributes);
    }

    private String extractStatus(Document doc) {
        NodeList statusCodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "StatusCode");
        if (statusCodes.getLength() > 0) {
            return ((Element) statusCodes.item(0)).getAttribute("Value");
        }
        return null;
    }

    private String extractNameId(Document doc) {
        NodeList nameIds = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
        if (nameIds.getLength() > 0) {
            return nameIds.item(0).getTextContent().trim();
        }
        return null;
    }

    private String extractIssuer(Document doc) {
        NodeList issuers = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
        if (issuers.getLength() > 0) {
            return issuers.item(0).getTextContent().trim();
        }
        return null;
    }

    private String extractSessionIndex(Document doc) {
        NodeList authnStatements = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "AuthnStatement");
        if (authnStatements.getLength() > 0) {
            return ((Element) authnStatements.item(0)).getAttribute("SessionIndex");
        }
        return null;
    }

    private Map<String, String> extractAttributes(Document doc) {
        Map<String, String> attrs = new LinkedHashMap<>();
        NodeList attrNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Attribute");
        for (int i = 0; i < attrNodes.getLength(); i++) {
            Element attr = (Element) attrNodes.item(i);
            String name = attr.getAttribute("Name");
            NodeList values = attr.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "AttributeValue");
            if (values.getLength() > 0) {
                attrs.put(name, values.item(0).getTextContent().trim());
            }
        }
        return attrs;
    }

    public record SamlAssertionResult(
            boolean success,
            String nameId,
            String issuer,
            String sessionIndex,
            Map<String, String> attributes,
            String errorMessage
    ) {
        public static SamlAssertionResult success(String nameId, String issuer, String sessionIndex, Map<String, String> attributes) {
            return new SamlAssertionResult(true, nameId, issuer, sessionIndex, attributes, null);
        }

        public static SamlAssertionResult failure(String errorMessage) {
            return new SamlAssertionResult(false, null, null, null, Map.of(), errorMessage);
        }
    }
}
