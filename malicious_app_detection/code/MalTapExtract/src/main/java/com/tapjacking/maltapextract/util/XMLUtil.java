package com.tapjacking.maltapextract.util;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is a utility class for XML operations.
 */
public class XMLUtil {

    /**
     * Returns all tags in the XML document.
     * @param node The root node of the XML document.
     * @return A list of all tags in the XML document.
     */
    public static List<String> getAllTags(Node node) {
        List<String> tags = new ArrayList<>();
        traverseTags(node, tags);
        return tags;
    }

    private static void traverseTags(Node node, List<String> tags) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            tags.add(node.getNodeName());
        }

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            traverseTags(nodeList.item(i), tags);
        }
    }

    /**
     * Returns all value and attribute references in the XML document, i.e., all values of attributes in the XML starting with '@' or '?'.
     * @param node The root node of the XML document.
     * @param onlyConsiderAttributes A list of attribute names to consider. If null, all attributes are considered.
     * @return A list of pairs where the first element is the attribute name and the second element is the attribute value.
     * @throws RuntimeException If an error occurs while evaluating the XPath expression.
     */
    public static List<Pair<String, String>> getReferences(Node node, List<String> onlyConsiderAttributes) {
        try {
            List<Pair<String, String>> values = new ArrayList<>();
            XPath xpath = XPathFactory.newInstance().newXPath();

            // XPath to select all attributes and text nodes starting with '@' or '?'
            XPathExpression attrExpr = xpath.compile("//@*[starts-with(., '@') or starts-with(., '?')]");

            // Evaluate attribute values
            NodeList attrNodes = (NodeList) attrExpr.evaluate(node, XPathConstants.NODESET);
            for (int i = 0; i < attrNodes.getLength(); i++) {
                Node attrNode = attrNodes.item(i);
                String attributeName = attrNode.getNodeName();
                String attributeNameNoNS = attributeName.contains(":") ? attributeName.split(":")[1] : attributeName;
                if (onlyConsiderAttributes != null && !onlyConsiderAttributes.contains(attributeNameNoNS)) {
                    continue;
                }
                String attributeValue = attrNode.getNodeValue();
                if (MiscUtil.isReference(attributeValue)) {
                    // We check if it really is a reference
                    values.add(Pair.of(attributeName, attributeValue));
                }
            }
            return values;
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error evaluating XPath expression", e);
        }
    }

    /**
     * Creates a copy of the given XML document with the resolved references.
     * The resolved references are provided as a map where the keys are the attribute values starting with '@' or '?' and the values are the resolved values of the references.
     * @param node The root node of the XML document.
     * @param resolvedReferences A map of resolved references.
     * @return A copy of the XML document with the resolved references.
     */
    public static Node createDereferencedXMLs(Node node, Map<String, String> resolvedReferences) {
        try {
            // Create a deep copy of the XML document
            Document newDoc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .newDocument();
            Node copiedNode = newDoc.importNode(node, true);
            newDoc.appendChild(copiedNode);

            // Use XPath to select all attributes starting with '@' or '?'
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expression = xpath.compile("//@*[starts-with(., '@') or starts-with(., '?')]");
            NodeList attributes = (NodeList) expression.evaluate(newDoc, XPathConstants.NODESET);

            // Replace the matching attributes with resolved values
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                String attrValue = attr.getNodeValue();
                if (resolvedReferences.containsKey(attrValue)) {
                    attr.setNodeValue(resolvedReferences.get(attrValue));
                }
            }

            return newDoc;
        } catch (Exception e) {
            throw new RuntimeException("Error creating dereferenced XML document", e);
        }
    }

    /**
     * Serializes the given XML Node to a string representation.
     * @param node The XML Node to serialize.
     * @return The string representation of the XML Node.
     */
    public static String serializeXML(Node node) {
        try {
            // Create a transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();

            // Set output properties for pretty-printing
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            // Transform the Node to a String
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));

            return writer.toString();
        } catch (TransformerException e) {
            throw new RuntimeException("Error serializing XML Node", e);
        }
    }
}
