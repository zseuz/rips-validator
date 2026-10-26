package com.zseuz.rips.factura;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Lee la factura electrónica de la DIAN. Acepta el {@code Invoice} directo o el
 * {@code AttachedDocument} con el que se envía, que trae el Invoice como texto dentro de
 * {@code cac:Attachment/cac:ExternalReference/cbc:Description}.
 *
 * <p>Las rutas usan {@code local-name()} para no depender de los prefijos de los namespaces, que
 * cambian entre facturadores. El parser no resuelve DTD ni entidades externas (XXE).
 */
@Component
public class LectorFacturaXml {

    public FacturaElectronica leer(byte[] xml) {
        try {
            Document documento = parsear(new InputSource(new ByteArrayInputStream(xml)));
            if ("AttachedDocument".equals(documento.getDocumentElement().getLocalName())) {
                String interno = texto(documento, "//*[local-name()='Attachment']/*[local-name()='ExternalReference']/*[local-name()='Description']");
                if (interno == null) {
                    throw new FacturaIlegibleException("El AttachedDocument no trae la factura (Attachment/ExternalReference/Description)");
                }
                documento = parsear(new InputSource(new StringReader(interno)));
            }
            if (!"Invoice".equals(documento.getDocumentElement().getLocalName())) {
                throw new FacturaIlegibleException("Se esperaba un Invoice o un AttachedDocument de la DIAN, no "
                        + documento.getDocumentElement().getLocalName());
            }
            return new FacturaElectronica(
                    texto(documento, "/*[local-name()='Invoice']/*[local-name()='ID']"),
                    texto(documento, "/*/*[local-name()='AccountingSupplierParty']/*[local-name()='Party']"
                            + "/*[local-name()='PartyTaxScheme']/*[local-name()='CompanyID']"),
                    fecha(texto(documento, "/*/*[local-name()='IssueDate']")),
                    valor(texto(documento, "/*/*[local-name()='LegalMonetaryTotal']/*[local-name()='LineExtensionAmount']")));
        } catch (SAXException | IOException e) {
            throw new FacturaIlegibleException("El archivo no es un XML válido: " + e.getMessage());
        }
    }

    private static Document parsear(InputSource fuente) throws SAXException, IOException {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);
            DocumentBuilder constructor = fabrica.newDocumentBuilder();
            constructor.setErrorHandler(null);
            return constructor.parse(fuente);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String texto(Document documento, String ruta) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node nodo = (Node) xpath.evaluate(ruta, documento, XPathConstants.NODE);
            return nodo == null || nodo.getTextContent().isBlank() ? null : nodo.getTextContent().trim();
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static LocalDate fecha(String texto) {
        try {
            return texto == null ? null : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal valor(String texto) {
        try {
            return texto == null ? null : new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class FacturaIlegibleException extends RuntimeException {
        public FacturaIlegibleException(String mensaje) {
            super(mensaje);
        }
    }
}
