package com.tapjacking.maltapanalyze.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * This class provides utility methods for XML parsing.
 */
public class XMLUtils {

    /**
     * Parses an XML string and returns the root {@link Element}.
     * @param xmlString The XML string to parse.
     * @return The root {@link Element} of the XML document.
     */
    public static Element parseXML(String xmlString) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlString.getBytes()));
            doc.getDocumentElement().normalize();
            return doc.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the value of the attribute with the given name from the given {@link Element}.
     * This method completely ignores namespaces and only considers values after the colon.
     *
     * @param element The {@link Element} to get the attribute from.
     * @param attribute The name of the attribute to get.
     * @return The value of the attribute with the given name, or null if the attribute does not exist.
     */
    public static String getAttributeIgnoreNS(Element element, String attribute) {
        NamedNodeMap map = element.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            String nodeName = map.item(i).getNodeName();
            if (nodeName.contains(":")) {
                nodeName = nodeName.split(":")[1];
            }
            if (nodeName.equals(attribute)) {
                return map.item(i).getNodeValue();
            }
        }
        return null;
    }
}
