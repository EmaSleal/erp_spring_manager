package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Utilidad para validación y manipulación de XML.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Component
@Slf4j
public class XmlValidator {
    
    private static final DocumentBuilderFactory documentBuilderFactory;
    private static final TransformerFactory transformerFactory;
    
    // Rutas de los esquemas XSD
    private static final String XSD_PATH_FACTURA = "xsd/FacturaElectronica_V4.4.xsd";
    private static final String XSD_PATH_TIQUETE = "xsd/TiqueteElectronico_V4.4.xsd";
    private static final String XSD_PATH_NOTA_CREDITO = "xsd/NotaCreditoElectronica_V4.4.xsd";
    private static final String XSD_PATH_NOTA_DEBITO = "xsd/NotaDebitoElectronica_V4.4.xsd";
    
    static {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        transformerFactory = TransformerFactory.newInstance();
    }
    
    /**
     * Valida si un XML está bien formado.
     */
    public static boolean validarXml(String xml) {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(xml)));
            return true;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("XML mal formado: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Valida XML contra el esquema XSD correspondiente según el tipo de comprobante.
     * 
     * @param xml XML a validar
     * @param tipoComprobante Tipo de comprobante electrónico
     * @return true si el XML es válido según el XSD, false en caso contrario
     */
    public static boolean validarContraXsd(String xml, TipoComprobanteElectronico tipoComprobante) {
        String xsdPath = obtenerRutaXsd(tipoComprobante);
        
        try {
            // Intentar cargar desde classpath primero
            ClassPathResource resource = new ClassPathResource(xsdPath);
            if (resource.exists()) {
                return validarContraXsd(xml, resource.getInputStream());
            }
            
            // Intentar cargar desde sistema de archivos
            File xsdFile = new File("src/main/resources/" + xsdPath);
            if (xsdFile.exists()) {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory.newSchema(xsdFile);
                
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(new StringReader(xml)));
                
                log.info("XML validado exitosamente contra XSD: {}", xsdPath);
                return true;
            }
            
            // XSD no encontrado - log warning pero no fallar
            log.warn("Esquema XSD no encontrado: {}. Validando solo estructura básica.", xsdPath);
            log.warn("Para habilitar validación completa, descargue los XSD según: src/main/resources/xsd/README.md");
            
            // Validar solo que esté bien formado
            return validarXml(xml);
            
        } catch (SAXException e) {
            log.error("XML no cumple con el esquema XSD {}: {}", xsdPath, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validando XML contra XSD: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Valida XML contra XSD usando InputStream.
     */
    private static boolean validarContraXsd(String xml, java.io.InputStream xsdStream) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
            
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            
            log.info("XML validado exitosamente contra XSD desde classpath");
            return true;
        } catch (SAXException e) {
            log.error("XML no cumple con el esquema XSD: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validando XML: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene la ruta del XSD según el tipo de comprobante.
     */
    private static String obtenerRutaXsd(TipoComprobanteElectronico tipo) {
        switch (tipo) {
            case FACTURA_ELECTRONICA:
                return XSD_PATH_FACTURA;
            case TIQUETE_ELECTRONICO:
                return XSD_PATH_TIQUETE;
            case NOTA_CREDITO:
                return XSD_PATH_NOTA_CREDITO;
            case NOTA_DEBITO:
                return XSD_PATH_NOTA_DEBITO;
            default:
                log.warn("Tipo de comprobante desconocido: {}. Usando XSD de factura por defecto.", tipo);
                return XSD_PATH_FACTURA;
        }
    }
    
    /**
     * Formatea XML con indentación.
     */
    public static String formatearXml(String xml) throws TransformerException {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            
            return writer.toString();
        } catch (Exception e) {
            log.error("Error formateando XML: {}", e.getMessage());
            throw new TransformerException("Error formateando XML", e);
        }
    }
    
    /**
     * Extrae valor de un nodo XML.
     */
    public static String extraerValorNodo(String xml, String nombreNodo) {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            
            var nodos = document.getElementsByTagName(nombreNodo);
            if (nodos.getLength() > 0) {
                return nodos.item(0).getTextContent();
            }
            return null;
        } catch (Exception e) {
            log.error("Error extrayendo nodo {}: {}", nombreNodo, e.getMessage());
            return null;
        }
    }
    
    /**
     * Verifica si el XML contiene firma digital.
     */
    public static boolean tieneFirmaDigital(String xml) {
        return xml != null && xml.contains("<ds:Signature");
    }
}
