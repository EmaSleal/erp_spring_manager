package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Códigos válidos de unidad de medida (UnidadMedidaType) leídos del XSD v4.4 del classpath, una sola vez. No hay
 * lista copiada a mano: el XSD es la fuente (ADR-041).
 */
public final class UnidadMedidaFe {

    static final String XSD = "/xsd/FacturaElectronica_V4.4.xsd";
    private static final String NS_XS = "http://www.w3.org/2001/XMLSchema";

    private static volatile List<String> cache;

    private UnidadMedidaFe() {
    }

    /** Valores del enum en el orden del XSD (lista inmutable). */
    public static List<String> valores() {
        List<String> v = cache;
        if (v == null) {
            synchronized (UnidadMedidaFe.class) {
                if (cache == null) {
                    cache = leer();
                }
                v = cache;
            }
        }
        return v;
    }

    /** true si el código (exacto, sensible a mayúsculas) está en el enum. */
    public static boolean esValida(String codigo) {
        return codigo != null && Set.copyOf(valores()).contains(codigo);
    }

    private static List<String> leer() {
        try (InputStream in = UnidadMedidaFe.class.getResourceAsStream(XSD)) {
            if (in == null) {
                throw new IllegalStateException("No se encuentra " + XSD + " en el classpath");
            }
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            Document doc = f.newDocumentBuilder().parse(in);
            NodeList tipos = doc.getElementsByTagNameNS(NS_XS, "simpleType");
            for (int i = 0; i < tipos.getLength(); i++) {
                Element tipo = (Element) tipos.item(i);
                if ("UnidadMedidaType".equals(tipo.getAttribute("name"))) {
                    List<String> valores = new ArrayList<>();
                    NodeList enums = tipo.getElementsByTagNameNS(NS_XS, "enumeration");
                    for (int j = 0; j < enums.getLength(); j++) {
                        valores.add(((Element) enums.item(j)).getAttribute("value"));
                    }
                    return List.copyOf(valores);
                }
            }
            throw new IllegalStateException("UnidadMedidaType no está en " + XSD);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IllegalStateException("No se pudo leer " + XSD, e);
        }
    }
}
