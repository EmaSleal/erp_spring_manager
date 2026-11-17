package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final String XSD_PATH_XMLDSIG = "xsd/xmldsig-core-schema.xsd";
    private static final String XMLDSIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    
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
    
    /** Schema compilado por tipo de comprobante: se compila una vez y se comparte (el Schema es thread-safe). */
    private static final Map<TipoComprobanteElectronico, Schema> SCHEMAS = new ConcurrentHashMap<>();

    /**
     * Errores de validación del XML contra el XSD oficial del tipo de comprobante (lista vacía si es válido). Exige
     * que el XSD exista en el classpath: sin esquema no hay validación que reportar. El Schema se compila una sola
     * vez por tipo y se reutiliza; cada validación usa su propio Validator (no es thread-safe).
     *
     * @throws IllegalStateException si el XSD no está en el classpath o no se puede cargar
     */
    public static List<String> erroresContraXsd(String xml, TipoComprobanteElectronico tipoComprobante) {
        List<String> errores = new ArrayList<>();
        try {
            Validator validator = schemaDe(tipoComprobante).newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    // las advertencias no invalidan el documento
                }

                @Override
                public void error(SAXParseException e) {
                    errores.add("línea " + e.getLineNumber() + ", columna " + e.getColumnNumber() + ": " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) {
                    error(e);
                }
            });
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException e) {
            errores.add(e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo validar el XML contra el esquema", e);
        }
        return errores;
    }

    /** Schema compartido del tipo (package-private para las pruebas de reutilización). */
    static Schema schemaDe(TipoComprobanteElectronico tipo) {
        String xsdPath = obtenerRutaXsd(tipo);
        return SCHEMAS.computeIfAbsent(tipo, t -> {
            ClassPathResource resource = new ClassPathResource(xsdPath);
            if (!resource.exists()) {
                throw new IllegalStateException("Esquema XSD no encontrado en el classpath: " + xsdPath);
            }
            try (InputStream xsd = resource.getInputStream()) {
                return crearSchema(xsd);
            } catch (SAXException | IOException e) {
                throw new IllegalStateException("No se pudo cargar el esquema " + xsdPath, e);
            }
        });
    }

    /**
     * Esquema a partir del XSD de Hacienda. Los XSD oficiales importan la firma XML con una ruta relativa
     * ({@code ../../xmldsig-core-schema.xsd}) que no existe en el classpath: el import se resuelve con la copia local
     * {@value #XSD_PATH_XMLDSIG}, sin red. Sin acceso externo (DTD ni esquemas) y con procesamiento seguro.
     */
    private static Schema crearSchema(InputStream xsdStream) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        schemaFactory.setResourceResolver(XmlValidator::resolverRecurso);
        return schemaFactory.newSchema(new StreamSource(xsdStream));
    }

    private static LSInput resolverRecurso(String type, String namespaceURI, String publicId, String systemId,
                                           String baseURI) {
        if (!XMLDSIG_NS.equals(namespaceURI)) {
            return null;
        }
        ClassPathResource resource = new ClassPathResource(XSD_PATH_XMLDSIG);
        if (!resource.exists()) {
            log.warn("Esquema de firma XML no encontrado: {}", XSD_PATH_XMLDSIG);
            return null;
        }
        try {
            DOMImplementationLS ls = (DOMImplementationLS) DOMImplementationRegistry.newInstance()
                .getDOMImplementation("LS");
            LSInput input = ls.createLSInput();
            input.setByteStream(resource.getInputStream());
            input.setSystemId(resource.getURL().toString());
            input.setPublicId(publicId);
            return input;
        } catch (Exception e) {
            log.warn("No se pudo resolver el esquema de firma XML: {}", e.getMessage());
            return null;
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
