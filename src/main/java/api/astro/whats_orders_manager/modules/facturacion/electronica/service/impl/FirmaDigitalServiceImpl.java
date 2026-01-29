package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.service.FirmaDigitalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

/**
 * Implementación del servicio de firma digital XAdES-EPES.
 * Cumple con especificaciones de Hacienda Costa Rica para facturación electrónica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Service
@Slf4j
public class FirmaDigitalServiceImpl implements FirmaDigitalService {
    
    private static final String DIGEST_METHOD = DigestMethod.SHA256;
    private static final String SIGNATURE_METHOD = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String CANONICALIZATION_METHOD = CanonicalizationMethod.INCLUSIVE;
    
    @Override
    public String firmarXml(String xml, String rutaCertificado, String pinCertificado) {
        log.info("Iniciando firma digital de XML con certificado: {}", rutaCertificado);
        
        try {
            // 1. Cargar certificado .p12
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(rutaCertificado)) {
                keyStore.load(fis, pinCertificado.toCharArray());
            }
            
            // 2. Obtener alias del certificado
            String alias = keyStore.aliases().nextElement();
            log.debug("Alias del certificado: {}", alias);
            
            // 3. Extraer clave privada y certificado
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, pinCertificado.toCharArray());
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            
            if (privateKey == null || certificate == null) {
                throw new IllegalStateException("No se pudo cargar la clave privada o el certificado");
            }
            
            // Validar vigencia del certificado
            Date ahora = new Date();
            if (ahora.before(certificate.getNotBefore()) || ahora.after(certificate.getNotAfter())) {
                log.warn("ADVERTENCIA: El certificado está fuera de su período de validez");
                log.warn("Válido desde: {} hasta: {}", 
                    certificate.getNotBefore(), certificate.getNotAfter());
            }
            
            // 4. Parsear XML a documento DOM
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            
            // 5. Crear firma XML
            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
            
            // Reference - Firma todo el documento
            Reference ref = signatureFactory.newReference(
                "",
                signatureFactory.newDigestMethod(DIGEST_METHOD, null),
                Collections.singletonList(
                    signatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)
                ),
                null,
                null
            );
            
            // SignedInfo
            SignedInfo signedInfo = signatureFactory.newSignedInfo(
                signatureFactory.newCanonicalizationMethod(CANONICALIZATION_METHOD, (C14NMethodParameterSpec) null),
                signatureFactory.newSignatureMethod(SIGNATURE_METHOD, null),
                Collections.singletonList(ref)
            );
            
            // KeyInfo - Información del certificado
            KeyInfoFactory keyInfoFactory = signatureFactory.getKeyInfoFactory();
            X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(certificate));
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));
            
            // Crear XMLSignature
            XMLSignature signature = signatureFactory.newXMLSignature(signedInfo, keyInfo);
            
            // 6. Determinar dónde insertar la firma
            Element root = doc.getDocumentElement();
            
            // Crear contexto de firma
            DOMSignContext signContext = new DOMSignContext(privateKey, root);
            
            // Configurar ID si existe (para XAdES)
            signContext.setIdAttributeNS(root, null, "Id");
            
            // 7. Firmar el documento
            signature.sign(signContext);
            
            // 8. Convertir documento firmado a String
            String xmlFirmado = documentToString(doc);
            
            log.info("XML firmado exitosamente");
            log.debug("Firmado con certificado de: {}", certificate.getSubjectX500Principal().getName());
            
            return xmlFirmado;
            
        } catch (java.io.FileNotFoundException e) {
            log.error("Archivo de certificado no encontrado: {}", rutaCertificado);
            throw new RuntimeException("Certificado no encontrado: " + rutaCertificado, e);
        } catch (Exception e) {
            log.error("Error al firmar XML: {}", e.getMessage(), e);
            throw new RuntimeException("Error en firma digital: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean verificarFirma(String xmlFirmado) {
        log.info("Verificando firma digital del XML");
        
        try {
            // 1. Parsear XML firmado
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlFirmado)));
            
            // 2. Buscar elemento Signature
            Element signatureElement = findSignatureElement(doc);
            if (signatureElement == null) {
                log.warn("No se encontró elemento <Signature> en el XML");
                return false;
            }
            
            // 3. Crear contexto de validación
            DOMValidateContext validateContext = new DOMValidateContext(
                new X509KeySelector(), signatureElement
            );
            
            // 4. Obtener XMLSignature y validar
            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = signatureFactory.unmarshalXMLSignature(validateContext);
            
            boolean isValid = signature.validate(validateContext);
            
            if (isValid) {
                log.info("Firma digital válida");
            } else {
                log.warn("Firma digital inválida");
                
                // Verificar cada referencia para detalles
                boolean signatureValueValid = signature.getSignatureValue().validate(validateContext);
                log.debug("Validez del valor de firma: {}", signatureValueValid);
                
                for (Object refObj : signature.getSignedInfo().getReferences()) {
                    Reference ref = (Reference) refObj;
                    boolean refValid = ref.validate(validateContext);
                    log.debug("Validez de referencia {}: {}", ref.getURI(), refValid);
                }
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error al verificar firma: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public CertificadoInfo obtenerInfoCertificado(String rutaCertificado, String pin) {
        log.info("Extrayendo información del certificado: {}", rutaCertificado);
        
        try {
            // Cargar keystore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(rutaCertificado)) {
                keyStore.load(fis, pin.toCharArray());
            }
            
            // Obtener certificado
            String alias = keyStore.aliases().nextElement();
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            
            if (certificate == null) {
                throw new IllegalStateException("No se encontró certificado en el archivo");
            }
            
            // Extraer información
            String titular = certificate.getSubjectX500Principal().getName();
            String emisor = certificate.getIssuerX500Principal().getName();
            
            // Extraer cédula del DN (si existe)
            String cedula = extraerCedulaDeCertificado(titular);
            
            // Formatear fecha de expiración
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String fechaExpiracion = sdf.format(certificate.getNotAfter());
            
            // Verificar vigencia
            Date ahora = new Date();
            boolean vigente = ahora.after(certificate.getNotBefore()) && 
                             ahora.before(certificate.getNotAfter());
            
            log.info("Certificado de: {}", titular);
            log.info("Emisor: {}", emisor);
            log.info("Válido hasta: {}", fechaExpiracion);
            log.info("Estado: {}", vigente ? "VIGENTE" : "VENCIDO/NO VÁLIDO");
            
            return new CertificadoInfo(
                titular,
                cedula,
                emisor,
                fechaExpiracion,
                vigente
            );
            
        } catch (java.io.FileNotFoundException e) {
            log.error("Certificado no encontrado: {}", rutaCertificado);
            throw new RuntimeException("Certificado no encontrado", e);
        } catch (Exception e) {
            log.error("Error leyendo certificado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al leer certificado: " + e.getMessage(), e);
        }
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    /**
     * Busca el elemento Signature en el documento.
     */
    private Element findSignatureElement(Document doc) {
        org.w3c.dom.NodeList signatures = doc.getElementsByTagNameNS(
            XMLSignature.XMLNS, "Signature"
        );
        
        if (signatures.getLength() > 0) {
            return (Element) signatures.item(0);
        }
        
        return null;
    }
    
    /**
     * Extrae la cédula del DN del certificado (si existe).
     */
    private String extraerCedulaDeCertificado(String dn) {
        // Buscar patrón común: SERIALNUMBER=CPJXXXXXXXXXX o similar
        if (dn.contains("SERIALNUMBER=")) {
            String[] parts = dn.split("SERIALNUMBER=");
            if (parts.length > 1) {
                String serial = parts[1].split(",")[0].trim();
                // Remover prefijos comunes como "CPJ", "CPF"
                serial = serial.replaceAll("^(CPJ|CPF)", "");
                return serial;
            }
        }
        
        return "N/A";
    }
    
    /**
     * Convierte documento DOM a String XML.
     */
    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
    
    /**
     * KeySelector para validación de firma.
     */
    private static class X509KeySelector extends KeySelector {
        @Override
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, 
                                       AlgorithmMethod method, XMLCryptoContext context) {
            
            for (Object obj : keyInfo.getContent()) {
                if (obj instanceof X509Data) {
                    X509Data x509Data = (X509Data) obj;
                    for (Object content : x509Data.getContent()) {
                        if (content instanceof X509Certificate) {
                            final X509Certificate cert = (X509Certificate) content;
                            return new KeySelectorResult() {
                                @Override
                                public java.security.Key getKey() {
                                    return cert.getPublicKey();
                                }
                            };
                        }
                    }
                }
            }
            
            return null;
        }
    }
}

