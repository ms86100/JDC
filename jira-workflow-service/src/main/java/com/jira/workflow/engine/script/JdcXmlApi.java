package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@Slf4j
public class JdcXmlApi {

    private final DocumentBuilderFactory dbFactory;

    public JdcXmlApi() {
        dbFactory = DocumentBuilderFactory.newInstance();
        try {
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {}
    }

    @HostAccess.Export
    public Map<String, Object> parse(String xmlString) {
        try {
            if (xmlString == null || xmlString.isBlank()) return Map.of();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            doc.getDocumentElement().normalize();
            return elementToMap(doc.getDocumentElement());
        } catch (Exception e) {
            log.warn("XML parse failed: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    @HostAccess.Export
    public String toXml(String rootName, Map<String, Object> data) {
        try {
            if (rootName == null || data == null) return "";
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement(rootName);
            doc.appendChild(root);
            mapToElement(doc, root, data);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            log.warn("XML generation failed: {}", e.getMessage());
            return "";
        }
    }

    @HostAccess.Export
    public String xpath(String xmlString, String expression) {
        try {
            if (xmlString == null || expression == null) return null;
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
            javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
            return xpath.evaluate(expression, doc);
        } catch (Exception e) {
            log.warn("XPath evaluation failed: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> elementToMap(Element element) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("_name", element.getTagName());

        NamedNodeMap attrs = element.getAttributes();
        if (attrs.getLength() > 0) {
            Map<String, String> attrMap = new LinkedHashMap<>();
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr attr = (Attr) attrs.item(i);
                attrMap.put(attr.getName(), attr.getValue());
            }
            map.put("_attributes", attrMap);
        }

        NodeList children = element.getChildNodes();
        List<Map<String, Object>> childList = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                childList.add(elementToMap((Element) node));
            } else if (node.getNodeType() == Node.TEXT_NODE) {
                String t = node.getTextContent().trim();
                if (!t.isEmpty()) text.append(t);
            }
        }

        if (!childList.isEmpty()) map.put("_children", childList);
        if (!text.isEmpty()) map.put("_text", text.toString());

        return map;
    }

    @SuppressWarnings("unchecked")
    private void mapToElement(Document doc, Element parent, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey().startsWith("_")) continue;
            Element child = doc.createElement(entry.getKey());
            if (entry.getValue() instanceof Map) {
                mapToElement(doc, child, (Map<String, Object>) entry.getValue());
            } else {
                child.setTextContent(String.valueOf(entry.getValue()));
            }
            parent.appendChild(child);
        }
    }
}
