package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.service.FirmaDigitalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.crypto.dsig.spec.XPathFilterParameterSpec;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorResult;
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
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class FirmaDigitalServiceImpl implements FirmaDigitalService {

    private static final String XADES_NS          = "http://uri.etsi.org/01903/v1.3.2#";
    private static final String DS_NS             = "http://www.w3.org/2000/09/xmldsig#";
    private static final String SIGNATURE_METHOD  = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String XPATH_FILTER_ALG  = "http://www.w3.org/TR/1999/REC-xpath-19991116";
    private static final String SIGNED_PROPS_TYPE = "http://uri.etsi.org/01903#SignedProperties";

    @Override
    public String firmarXml(String xml, String rutaCertificado, String pinCertificado) {
        log.info("Iniciando firma XAdES-BES: {}", rutaCertificado);
        try {
            // 1. Cargar .p12
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(rutaCertificado)) {
                ks.load(fis, pinCertificado.toCharArray());
            }
            String alias      = ks.aliases().nextElement();
            PrivateKey pk     = (PrivateKey) ks.getKey(alias, pinCertificado.toCharArray());
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
            if (pk == null || cert == null) {
                throw new IllegalStateException("No se pudo cargar la clave privada o el certificado");
            }

            // 2. Validar vigencia
            Date ahora = new Date();
            if (ahora.before(cert.getNotBefore()) || ahora.after(cert.getNotAfter())) {
                log.warn("Certificado fuera de vigencia: {} — {}", cert.getNotBefore(), cert.getNotAfter());
            }

            // 3. Parsear XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            // 4. IDs únicos para esta firma
            String uid           = UUID.randomUUID().toString().replace("-", "");
            String signatureId   = "id-" + uid;
            String signedPropsId = "xades-id-" + uid;
            String refDocId      = "r-id-1";

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            // 5. Reference 1 — contenido del documento (excluye el propio ds:Signature)
            Map<String, String> dsPrefix = Collections.singletonMap("ds", DS_NS);
            List<Transform> t1 = Arrays.asList(
                fac.newTransform(XPATH_FILTER_ALG,
                    new XPathFilterParameterSpec("not(ancestor-or-self::ds:Signature)", dsPrefix)),
                fac.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null)
            );
            Reference ref1 = fac.newReference(
                "", fac.newDigestMethod(DigestMethod.SHA256, null), t1, null, refDocId
            );

            // 6. Reference 2 — xades:SignedProperties (requerido por XAdES)
            List<Transform> t2 = Collections.singletonList(
                fac.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null)
            );
            Reference ref2 = fac.newReference(
                "#" + signedPropsId,
                fac.newDigestMethod(DigestMethod.SHA256, null),
                t2, SIGNED_PROPS_TYPE, null
            );

            // 7. SignedInfo con ExcC14N y RSA-SHA256
            SignedInfo signedInfo = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SIGNATURE_METHOD, null),
                Arrays.asList(ref1, ref2)
            );

            // 8. KeyInfo con certificado X509
            KeyInfoFactory kif = fac.getKeyInfoFactory();
            KeyInfo keyInfo = kif.newKeyInfo(
                Collections.singletonList(kif.newX509Data(Collections.singletonList(cert)))
            );

            // 9. Construir xades:QualifyingProperties en DOM
            Element qProps = buildXadesQualifyingProperties(doc, cert, signatureId, signedPropsId, refDocId);

            // Marcar "Id" como atributo XML ID para que el fragmento #xades-id-... se resuelva
            Element signedPropsEl = (Element) qProps
                .getElementsByTagNameNS(XADES_NS, "SignedProperties").item(0);
            signedPropsEl.setIdAttribute("Id", true);

            // 10. XMLObject conteniendo QualifyingProperties
            XMLObject xadesObj = fac.newXMLObject(
                Collections.singletonList(new DOMStructure(qProps)), null, null, null
            );

            // 11. XMLSignature completo
            XMLSignature xmlSignature = fac.newXMLSignature(
                signedInfo, keyInfo,
                Collections.singletonList(xadesObj),
                signatureId, null
            );

            // 12. Firmar en el contexto del documento
            DOMSignContext signContext = new DOMSignContext(pk, doc.getDocumentElement());
            signContext.putNamespacePrefix(DS_NS, "ds");
            signContext.putNamespacePrefix(XADES_NS, "xades");

            xmlSignature.sign(signContext);

            log.info("XML firmado exitosamente con XAdES-BES");
            return documentToString(doc);

        } catch (java.io.FileNotFoundException e) {
            log.error("Certificado no encontrado: {}", rutaCertificado);
            throw new RuntimeException("Certificado no encontrado: " + rutaCertificado, e);
        } catch (Exception e) {
            log.error("Error al firmar XML: {}", e.getMessage(), e);
            throw new RuntimeException("Error en firma digital: " + e.getMessage(), e);
        }
    }

    /**
     * Construye el bloque xades:QualifyingProperties requerido por XAdES-BES.
     * Sigue la estructura exacta que Hacienda CR valida (ETSI TS 101 903 v1.3.2).
     */
    private Element buildXadesQualifyingProperties(Document doc, X509Certificate cert,
            String signatureId, String signedPropsId, String refDocId) throws Exception {

        // SHA-1 del certificado DER (formato que Hacienda usa en su propia firma de respuesta)
        String certDigestB64 = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest(cert.getEncoded())
        );
        String signingTime = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

        // xades:QualifyingProperties
        Element qProps = doc.createElementNS(XADES_NS, "xades:QualifyingProperties");
        qProps.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", XADES_NS);
        qProps.setAttribute("Target", "#" + signatureId);

        // xades:SignedProperties
        Element signedProps = doc.createElementNS(XADES_NS, "xades:SignedProperties");
        signedProps.setAttribute("Id", signedPropsId);
        qProps.appendChild(signedProps);

        // xades:SignedSignatureProperties
        Element signedSigProps = doc.createElementNS(XADES_NS, "xades:SignedSignatureProperties");
        signedProps.appendChild(signedSigProps);

        // xades:SigningTime
        Element timeEl = doc.createElementNS(XADES_NS, "xades:SigningTime");
        timeEl.setTextContent(signingTime);
        signedSigProps.appendChild(timeEl);

        // xades:SigningCertificate > xades:Cert
        Element signingCert = doc.createElementNS(XADES_NS, "xades:SigningCertificate");
        signedSigProps.appendChild(signingCert);

        Element certEl = doc.createElementNS(XADES_NS, "xades:Cert");
        signingCert.appendChild(certEl);

        Element certDigestEl = doc.createElementNS(XADES_NS, "xades:CertDigest");
        certEl.appendChild(certDigestEl);

        Element digestMethod = doc.createElementNS(DS_NS, "ds:DigestMethod");
        digestMethod.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#sha1");
        certDigestEl.appendChild(digestMethod);

        Element digestValue = doc.createElementNS(DS_NS, "ds:DigestValue");
        digestValue.setTextContent(certDigestB64);
        certDigestEl.appendChild(digestValue);

        Element issuerSerial = doc.createElementNS(XADES_NS, "xades:IssuerSerial");
        certEl.appendChild(issuerSerial);

        Element issuerName = doc.createElementNS(DS_NS, "ds:X509IssuerName");
        issuerName.setTextContent(cert.getIssuerX500Principal().getName());
        issuerSerial.appendChild(issuerName);

        Element serialNum = doc.createElementNS(DS_NS, "ds:X509SerialNumber");
        serialNum.setTextContent(cert.getSerialNumber().toString());
        issuerSerial.appendChild(serialNum);

        // xades:SignaturePolicyIdentifier (requerido por XAdES-EPES / Hacienda CR)
        Element policyIdentifier = doc.createElementNS(XADES_NS, "xades:SignaturePolicyIdentifier");
        Element policyImplied    = doc.createElementNS(XADES_NS, "xades:SignaturePolicyImplied");
        policyIdentifier.appendChild(policyImplied);
        signedSigProps.appendChild(policyIdentifier);

        // xades:SignedDataObjectProperties
        Element signedDataObjProps = doc.createElementNS(XADES_NS, "xades:SignedDataObjectProperties");
        signedProps.appendChild(signedDataObjProps);

        Element dataObjFormat = doc.createElementNS(XADES_NS, "xades:DataObjectFormat");
        dataObjFormat.setAttribute("ObjectReference", "#" + refDocId);
        signedDataObjProps.appendChild(dataObjFormat);

        Element mimeType = doc.createElementNS(XADES_NS, "xades:MimeType");
        mimeType.setTextContent("application/octet-stream");
        dataObjFormat.appendChild(mimeType);

        return qProps;
    }

    @Override
    public boolean verificarFirma(String xmlFirmado) {
        log.info("Verificando firma digital del XML");
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlFirmado)));

            Element signatureElement = findSignatureElement(doc);
            if (signatureElement == null) {
                log.warn("No se encontró elemento <Signature> en el XML");
                return false;
            }

            DOMValidateContext validateContext = new DOMValidateContext(
                new X509KeySelector(), signatureElement
            );

            XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = signatureFactory.unmarshalXMLSignature(validateContext);

            boolean isValid = signature.validate(validateContext);

            if (isValid) {
                log.info("Firma digital válida");
            } else {
                log.warn("Firma digital inválida");
                boolean svValid = signature.getSignatureValue().validate(validateContext);
                log.debug("Valor de firma válido: {}", svValid);
                for (Object refObj : signature.getSignedInfo().getReferences()) {
                    Reference ref = (Reference) refObj;
                    log.debug("Referencia {} válida: {}", ref.getURI(), ref.validate(validateContext));
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
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(rutaCertificado)) {
                keyStore.load(fis, pin.toCharArray());
            }

            String alias = keyStore.aliases().nextElement();
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            if (certificate == null) {
                throw new IllegalStateException("No se encontró certificado en el archivo");
            }

            String titular = certificate.getSubjectX500Principal().getName();
            String emisor  = certificate.getIssuerX500Principal().getName();
            String cedula  = extraerCedulaDeCertificado(titular);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String fechaExpiracion = sdf.format(certificate.getNotAfter());

            Date ahora = new Date();
            boolean vigente = ahora.after(certificate.getNotBefore()) &&
                              ahora.before(certificate.getNotAfter());

            log.info("Certificado de: {}", titular);
            log.info("Emisor: {}", emisor);
            log.info("Válido hasta: {}", fechaExpiracion);
            log.info("Estado: {}", vigente ? "VIGENTE" : "VENCIDO/NO VÁLIDO");

            return new CertificadoInfo(titular, cedula, emisor, fechaExpiracion, vigente);

        } catch (java.io.FileNotFoundException e) {
            log.error("Certificado no encontrado: {}", rutaCertificado);
            throw new RuntimeException("Certificado no encontrado", e);
        } catch (Exception e) {
            log.error("Error leyendo certificado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al leer certificado: " + e.getMessage(), e);
        }
    }

    // ========== AUXILIARES ==========

    private Element findSignatureElement(Document doc) {
        org.w3c.dom.NodeList signatures = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        return signatures.getLength() > 0 ? (Element) signatures.item(0) : null;
    }

    private String extraerCedulaDeCertificado(String dn) {
        if (dn.contains("SERIALNUMBER=")) {
            String[] parts = dn.split("SERIALNUMBER=");
            if (parts.length > 1) {
                return parts[1].split(",")[0].trim().replaceAll("^(CPJ|CPF)", "");
            }
        }
        return "N/A";
    }

    private String documentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private static class X509KeySelector extends KeySelector {
        @Override
        public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose,
                                        AlgorithmMethod method, XMLCryptoContext context) {
            for (Object obj : keyInfo.getContent()) {
                if (obj instanceof X509Data x509Data) {
                    for (Object content : x509Data.getContent()) {
                        if (content instanceof X509Certificate cert) {
                            return cert::getPublicKey;
                        }
                    }
                }
            }
            return null;
        }
    }
}
