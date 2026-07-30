package com.avionics_systems.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SamlResponseHandler {

    private final MessageSource messageSource;

    /**
     * Parses and verifies a Base64-encoded SAML response.
     *
     * @param samlResponseBase64 the Base64-encoded SAML response XML
     * @param idpCertificate     the IdP's X.509 certificate (PEM format) used to verify the XML signature
     * @return the assertion result containing the parsed user identity or an error
     */
    public SamlAssertionResult parseResponse(String samlResponseBase64, String idpCertificate) {
        try {
            byte[] decoded = Base64.getDecoder().decode(samlResponseBase64);
            String xml = new String(decoded, StandardCharsets.UTF_8);
            return parseXml(xml, idpCertificate);
        } catch (SecurityException e) {
            log.error("SAML signature verification failed: {}", e.getMessage());
            return SamlAssertionResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse SAML response: {}", e.getMessage());
            return SamlAssertionResult.failure(
                    messageSource.getMessage("saml.error.parse.failed", new Object[]{e.getMessage()}, Locale.ENGLISH));
        }
    }

    private SamlAssertionResult parseXml(String xml, String idpCertificate) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        // Verify the XML digital signature BEFORE extracting any assertions
        verifySignature(doc, idpCertificate);

        String status = extractStatus(doc);
        if (!"urn:oasis:names:tc:SAML:2.0:status:Success".equals(status)) {
            return SamlAssertionResult.failure(
                    messageSource.getMessage("saml.error.auth.failed", new Object[]{status}, Locale.ENGLISH));
        }

        String nameId = extractNameId(doc);
        if (nameId == null || nameId.isBlank()) {
            return SamlAssertionResult.failure(
                    messageSource.getMessage("saml.error.no.nameid", null, Locale.ENGLISH));
        }

        Map<String, String> attributes = extractAttributes(doc);
        String issuer = extractIssuer(doc);
        String sessionIndex = extractSessionIndex(doc);

        return SamlAssertionResult.success(nameId, issuer, sessionIndex, attributes);
    }

    /**
     * Verifies the XML digital signature on the SAML response using the IdP's certificate.
     * This MUST be called before any assertion data is trusted.
     *
     * @param doc               the parsed SAML response DOM document
     * @param idpCertificateStr the IdP's X.509 certificate in PEM format
     * @throws SecurityException if the signature is missing, invalid, or verification fails
     */
    private void verifySignature(Document doc, String idpCertificateStr) throws Exception {
        // Find the Signature element in the SAML response
        NodeList signatureNodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (signatureNodes.getLength() == 0) {
            throw new SecurityException(
                    messageSource.getMessage("saml.error.not.signed", null, Locale.ENGLISH));
        }

        // Parse the IdP X.509 certificate from PEM format
        String certPem = idpCertificateStr
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] certBytes = Base64.getDecoder().decode(certPem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)
                cf.generateCertificate(new ByteArrayInputStream(certBytes));

        // Create the validation context with the IdP's public key and the Signature element
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        DOMValidateContext valContext =
                new DOMValidateContext(cert.getPublicKey(), signatureNodes.item(0));

        // Unmarshal and validate the XML signature
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);

        if (!signature.validate(valContext)) {
            log.warn("SAML signature core validation failed. Checking reference validity...");
            // Log details to aid debugging without leaking sensitive data
            boolean coreValid = signature.getSignatureValue().validate(valContext);
            log.warn("SAML signature core validity: {}", coreValid);
            throw new SecurityException(
                    messageSource.getMessage("saml.error.signature.failed", null, Locale.ENGLISH));
        }

        log.debug("SAML response signature verified successfully");
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
