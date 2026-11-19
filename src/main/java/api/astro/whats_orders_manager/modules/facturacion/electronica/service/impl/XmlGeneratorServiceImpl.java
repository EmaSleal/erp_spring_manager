package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.DatosFeProducto;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.HoraFiscal;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.InformacionReferenciaXml;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.TelefonoFe;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.ProblemaDatoFe;
import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación del servicio de generación de XML.
 * Genera XML según especificación v4.4 de Hacienda de Costa Rica.
 */
@Service
@Slf4j
public class XmlGeneratorServiceImpl implements XmlGeneratorService {

    private static final String NAMESPACE = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica";

    /** Componente único de desglose de IVA (ADR-009): mismo cálculo que la factura y el asiento. */
    private final CalculadoraImpuestoService calculadoraImpuestoService;

    /** Validador único de datos FE (T-038): el XML nunca contradice lo que el validador declara inválido. */
    private final DatosFeValidator datosFeValidator;

    public XmlGeneratorServiceImpl(CalculadoraImpuestoService calculadoraImpuestoService,
                                   DatosFeValidator datosFeValidator) {
        this.calculadoraImpuestoService = calculadoraImpuestoService;
        this.datosFeValidator = datosFeValidator;
    }

    @Override
    public String generarXmlFactura(ComprobanteElectronico comprobante) {
        log.info("Generando XML de factura electrónica: {}", comprobante.getClaveNumerica());

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<FacturaElectronica");
        xml.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        xml.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
        xml.append(" xmlns=\"").append(NAMESPACE).append("\"");
        xml.append(" xsi:schemaLocation=\"").append(NAMESPACE).append(" ").append(NAMESPACE).append(".xsd\"");
        xml.append(">");

        xml.append("<Clave>").append(comprobante.getClaveNumerica()).append("</Clave>");
        xml.append("<ProveedorSistemas>").append(obtenerProveedorSistemas(comprobante)).append("</ProveedorSistemas>");
        xml.append("<CodigoActividadEmisor>").append(obtenerActividadEconomica(comprobante)).append("</CodigoActividadEmisor>");
        List<ProblemaDatoFe> problemasReceptor = problemasDelReceptor(comprobante);
        agregarCodigoActividadReceptor(xml, comprobante, problemasReceptor);
        xml.append("<NumeroConsecutivo>").append(extraerConsecutivoDeClave(comprobante.getClaveNumerica())).append("</NumeroConsecutivo>");
        xml.append("<FechaEmision>").append(formatearFecha(comprobante.getFechaEmision())).append("</FechaEmision>");

        agregarEmisor(xml, comprobante);
        agregarReceptor(xml, comprobante, problemasReceptor);

        var factura = comprobante.getFactura();
        String condicionVenta = (factura != null && factura.getCondicionVentaFE() != null)
            ? factura.getCondicionVentaFE().getCodigo() : "01";
        xml.append("<CondicionVenta>").append(condicionVenta).append("</CondicionVenta>");

        // PlazoCredito solo cuando la condición de venta es crédito (02)
        if ("02".equals(condicionVenta) && factura != null && factura.getPlazoCredito() != null) {
            xml.append("<PlazoCredito>").append(factura.getPlazoCredito()).append("</PlazoCredito>");
        }

        agregarDetalleFactura(xml, comprobante);
        agregarResumen(xml, comprobante);
        agregarInformacionReferenciaFactura(xml, comprobante);

        xml.append("</FacturaElectronica>");

        String xmlGenerado = xml.toString();

        if (!XmlValidator.validarXml(xmlGenerado)) {
            log.error("XML generado no es válido");
            throw new IllegalStateException("XML generado no es válido");
        }

        return xmlGenerado;
    }

    /**
     * InformacionReferencia de la factura (ADR-045), tras ResumenFactura (orden del XSD, máximo 10): primero la de la
     * sustituta (01, clave y fecha del comprobante ACEPTADO de la original, código 07 o 15) y después la de la
     * reemisión (10, el último RECHAZADO anterior, código 16). Todo se deduce de la base, de modo que regenerar el
     * XML con la misma clave da el mismo resultado. Una primera emisión normal no lleva referencias.
     */
    private void agregarInformacionReferenciaFactura(StringBuilder xml, ComprobanteElectronico comprobante) {
        var factura = comprobante.getFactura();
        if (factura == null) {
            return;
        }
        var original = factura.getFacturaSustituida();
        if (original != null) {
            ComprobanteElectronico origen = original.getComprobantes().stream()
                .filter(c -> c.getEstado() == EstadoComprobante.ACEPTADO)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "La factura sustituida no tiene un comprobante aceptado"));
            xml.append(InformacionReferenciaXml.construir(InformacionReferenciaXml.TIPO_DOC_FACTURA_ELECTRONICA,
                origen.getClaveNumerica(), origen.getFechaEmision(), factura.getCodigoSustitucion(),
                factura.getRazonSustitucion()));
        }
        factura.getComprobantes().stream()
            .filter(c -> c.getEstado() == EstadoComprobante.RECHAZADO)
            .filter(c -> comprobante.getId() == null || (c.getId() != null && c.getId() < comprobante.getId()))
            .max(Comparator.comparing(c -> c.getId() == null ? 0L : c.getId()))
            .ifPresent(rechazado -> {
                // T-044 (P-099): Hacienda del ambiente del comprobante nuevo (el activo al emitir) no conoce la clave de
                // un rechazado de otro ambiente; se emite sin la referencia 10/16 y queda en el log
                if (!Objects.equals(rechazado.getAmbiente(), comprobante.getAmbiente())) {
                    log.info("Referencia 10/16 omitida: el comprobante rechazado {} es del ambiente {} y el nuevo es del {}",
                        rechazado.getClaveNumerica(), rechazado.getAmbiente(), comprobante.getAmbiente());
                    return;
                }
                xml.append(InformacionReferenciaXml.construir(
                    InformacionReferenciaXml.TIPO_DOC_RECHAZADO, rechazado.getClaveNumerica(),
                    rechazado.getFechaEmision(), CodigoReferenciaNota.SUSTITUYE_COMPROBANTE_RECHAZADO.getCodigo(),
                    InformacionReferenciaXml.RAZON_REEMISION_RECHAZADO));
            });
    }

    @Override
    public String generarXmlTiquete(ComprobanteElectronico comprobante) {
        log.info("Generando XML de tiquete electrónico: {}", comprobante.getClaveNumerica());

        String nsTE = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/tiqueteElectronico";
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<TiqueteElectronico xmlns=\"").append(nsTE).append("\">");

        xml.append("<Clave>").append(comprobante.getClaveNumerica()).append("</Clave>");
        xml.append("<ProveedorSistemas>").append(obtenerProveedorSistemas(comprobante)).append("</ProveedorSistemas>");
        xml.append("<CodigoActividadEmisor>").append(obtenerActividadEconomica(comprobante)).append("</CodigoActividadEmisor>");
        xml.append("<NumeroConsecutivo>").append(extraerConsecutivoDeClave(comprobante.getClaveNumerica())).append("</NumeroConsecutivo>");
        xml.append("<FechaEmision>").append(formatearFecha(comprobante.getFechaEmision())).append("</FechaEmision>");

        agregarEmisor(xml, comprobante);
        xml.append("<CondicionVenta>01</CondicionVenta>");
        agregarDetalleFactura(xml, comprobante);
        agregarResumen(xml, comprobante);

        xml.append("</TiqueteElectronico>");
        return xml.toString();
    }

    /**
     * Nota de crédito electrónica v4.4 (T-032, ADR-037) en el orden de NotaCreditoElectronica_V4.4.xsd: Clave,
     * ProveedorSistemas, CodigoActividadEmisor, CodigoActividadReceptor, NumeroConsecutivo, FechaEmision, Emisor,
     * Receptor, CondicionVenta, CondicionVentaOtros, PlazoCredito, DetalleServicio, ResumenFactura e
     * InformacionReferencia (la firma va al final).
     *
     * Hacienda verifica que los datos informativos de la nota correspondan al comprobante referenciado (apartado d):
     * el emisor, el receptor, el código de actividad del receptor, la condición de venta, el plazo y el medio de pago
     * se copian del comprobante ACEPTADO de la factura. La moneda y el tipo de cambio son los de la factura, guardados
     * en la nota. El detalle y el resumen se calculan con las líneas de la nota.
     */
    @Override
    public String generarXmlNotaCredito(ComprobanteElectronico comprobante) {
        log.info("Generando XML de nota de crédito: {}", comprobante.getClaveNumerica());

        NotaFactura nota = comprobante.getNotaFactura();
        if (nota == null) {
            throw new IllegalStateException("El comprobante " + comprobante.getClaveNumerica()
                + " no está asociado a una nota de crédito");
        }
        ComprobanteElectronico referencia = nota.getComprobanteReferencia();
        Element original = raizDe(referencia.getXmlComprobante(), referencia.getClaveNumerica());

        String nsNC = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/notaCreditoElectronica";
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<NotaCreditoElectronica xmlns=\"").append(nsNC).append("\">");

        xml.append("<Clave>").append(comprobante.getClaveNumerica()).append("</Clave>");
        xml.append("<ProveedorSistemas>").append(obtenerProveedorSistemas(comprobante)).append("</ProveedorSistemas>");
        xml.append("<CodigoActividadEmisor>").append(obtenerActividadEconomica(comprobante)).append("</CodigoActividadEmisor>");
        copiarHijo(xml, original, "CodigoActividadReceptor");
        xml.append("<NumeroConsecutivo>").append(extraerConsecutivoDeClave(comprobante.getClaveNumerica())).append("</NumeroConsecutivo>");
        xml.append("<FechaEmision>").append(formatearFecha(comprobante.getFechaEmision())).append("</FechaEmision>");

        if (!copiarHijo(xml, original, "Emisor")) {
            throw new IllegalStateException("El comprobante referenciado " + referencia.getClaveNumerica()
                + " no tiene Emisor en su XML");
        }
        copiarHijo(xml, original, "Receptor");
        if (!copiarHijo(xml, original, "CondicionVenta")) {
            xml.append("<CondicionVenta>01</CondicionVenta>");
        }
        copiarHijo(xml, original, "CondicionVentaOtros");
        copiarHijo(xml, original, "PlazoCredito");

        xml.append("<DetalleServicio>");
        List<LineaResumen> lineasResumen = new ArrayList<>();
        for (LineaNotaFactura linea : nota.getLineas()) {
            DesgloseImpuesto desglose = new DesgloseImpuesto(linea.getSubtotal(), linea.getMontoIva(), linea.getTarifaIva());
            lineasResumen.add(new LineaResumen(desglose, linea.getCodigoCabys()));
            escribirLineaDetalle(xml, linea.getNumeroLinea(), linea.getCodigoCabys(), linea.getCantidad(),
                linea.getUnidadMedida(), linea.getDescripcion(), linea.getPrecioUnitario(), desglose);
        }
        xml.append("</DetalleServicio>");

        escribirResumen(xml, lineasResumen, nota.getMoneda(), nota.getTipoCambio(), medioPagoDe(original));

        xml.append(InformacionReferenciaXml.construir(InformacionReferenciaXml.TIPO_DOC_FACTURA_ELECTRONICA,
            nota.getClaveReferencia(), nota.getFechaEmisionReferencia(), nota.getCodigoReferencia(), nota.getRazon()));

        xml.append("</NotaCreditoElectronica>");

        String xmlGenerado = xml.toString();
        if (!XmlValidator.validarXml(xmlGenerado)) {
            throw new IllegalStateException("XML de nota de crédito generado no es válido");
        }
        return xmlGenerado;
    }

    /** Elemento raíz del XML (firmado) del comprobante referenciado. */
    private static Element raizDe(String xml, String clave) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("El comprobante referenciado " + clave + " no tiene XML");
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el XML del comprobante referenciado " + clave, e);
        }
    }

    /** Primer hijo directo del elemento con ese nombre local, o null. */
    private static Element hijo(Element padre, String nombre) {
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && nombre.equals(e.getLocalName())) {
                return e;
            }
        }
        return null;
    }

    /**
     * Copia el hijo directo con ese nombre (con todo su contenido) al XML de la nota, en el espacio de nombres por
     * defecto de la nota.
     *
     * @return false si el original no lo tiene
     */
    private boolean copiarHijo(StringBuilder xml, Element padre, String nombre) {
        Element e = hijo(padre, nombre);
        if (e == null) {
            return false;
        }
        copiarElemento(xml, e);
        return true;
    }

    private void copiarElemento(StringBuilder xml, Element e) {
        xml.append('<').append(e.getLocalName()).append('>');
        boolean tieneElementos = false;
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element hijo) {
                tieneElementos = true;
                copiarElemento(xml, hijo);
            }
        }
        if (!tieneElementos) {
            xml.append(escaparXml(e.getTextContent()));
        }
        xml.append("</").append(e.getLocalName()).append('>');
    }

    /** TipoMedioPago del comprobante referenciado (ResumenFactura/MedioPago), o 01 si no lo declara. */
    private static String medioPagoDe(Element original) {
        Element resumen = hijo(original, "ResumenFactura");
        Element medioPago = resumen != null ? hijo(resumen, "MedioPago") : null;
        Element tipo = medioPago != null ? hijo(medioPago, "TipoMedioPago") : null;
        return tipo != null && !tipo.getTextContent().isBlank() ? tipo.getTextContent().trim() : "01";
    }

    @Override
    public String generarXmlNotaDebito(ComprobanteElectronico comprobante) {
        log.info("Generando XML de nota de débito: {}", comprobante.getClaveNumerica());

        String nsND = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/notaDebitoElectronica";
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<NotaDebitoElectronica xmlns=\"").append(nsND).append("\">");

        xml.append("<Clave>").append(comprobante.getClaveNumerica()).append("</Clave>");
        xml.append("<ProveedorSistemas>").append(obtenerProveedorSistemas(comprobante)).append("</ProveedorSistemas>");
        xml.append("<CodigoActividadEmisor>").append(obtenerActividadEconomica(comprobante)).append("</CodigoActividadEmisor>");
        xml.append("<NumeroConsecutivo>").append(extraerConsecutivoDeClave(comprobante.getClaveNumerica())).append("</NumeroConsecutivo>");
        xml.append("<FechaEmision>").append(formatearFecha(comprobante.getFechaEmision())).append("</FechaEmision>");

        agregarEmisor(xml, comprobante);
        agregarReceptor(xml, comprobante, problemasDelReceptor(comprobante));
        xml.append("<CondicionVenta>01</CondicionVenta>");
        agregarDetalleFactura(xml, comprobante);
        agregarResumen(xml, comprobante);

        xml.append("</NotaDebitoElectronica>");
        return xml.toString();
    }

    // =====================================================================
    // Secciones comunes
    // =====================================================================

    private void agregarEmisor(StringBuilder xml, ComprobanteElectronico comprobante) {
        var empresa = comprobante.getEmpresa();

        // T-038 (ADR-044): nunca se inventan datos del emisor. La emisión ya los valida antes del consecutivo (409);
        // si aun así se llega aquí sin ellos, es un fallo local de generación (ERROR, ADR-030).
        List<ProblemaDatoFe> problemas = datosFeValidator.validarEmisor(empresa);
        if (!problemas.isEmpty()) {
            throw new IllegalStateException("Los datos de facturación electrónica de la empresa están incompletos o no "
                + "son válidos: " + problemas.stream().map(ProblemaDatoFe::campo).toList());
        }

        xml.append("<Emisor>");
        xml.append("<Nombre>").append(escaparXml(empresa.getNombreEmpresa().trim())).append("</Nombre>");

        xml.append("<Identificacion>");
        xml.append("<Tipo>").append(empresa.getTipoIdentificacion().getCodigo()).append("</Tipo>");
        xml.append("<Numero>").append(DatosFeValidator.numeroLimpio(empresa.getRuc())).append("</Numero>");
        xml.append("</Identificacion>");

        String nombreComercial = empresa.getNombreComercialFe() != null ? empresa.getNombreComercialFe()
            : (empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombreEmpresa());
        xml.append("<NombreComercial>").append(escaparXml(nombreComercial)).append("</NombreComercial>");

        xml.append("<Ubicacion>");
        xml.append("<Provincia>").append(empresa.getCodigoProvincia().trim()).append("</Provincia>");
        xml.append("<Canton>").append(empresa.getCanton().trim()).append("</Canton>");
        xml.append("<Distrito>").append(empresa.getDistrito().trim()).append("</Distrito>");
        // Barrio es opcional (5 a 50 caracteres en el XSD); la columna guarda un código corto, que no se emite
        String barrio = empresa.getBarrio() != null ? empresa.getBarrio().trim() : "";
        if (barrio.length() >= 5 && barrio.length() <= 50) {
            xml.append("<Barrio>").append(escaparXml(barrio)).append("</Barrio>");
        }
        xml.append("<OtrasSenas>").append(escaparXml(DatosFeValidator.otrasSenasEmisor(empresa))).append("</OtrasSenas>");
        xml.append("</Ubicacion>");

        agregarTelefono(xml, empresa.getTelefono());

        xml.append("<CorreoElectronico>").append(escaparXml(empresa.getEmail().trim())).append("</CorreoElectronico>");
        xml.append("</Emisor>");
    }

    /**
     * Problemas del receptor por el validador único; vacía si la factura no tiene cliente. Los obligatorios
     * (nombre, identificación) interrumpen la generación: la emisión ya los bloquea antes (409), esto es la defensa.
     */
    private List<ProblemaDatoFe> problemasDelReceptor(ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null || comprobante.getFactura().getCliente() == null) {
            return List.of();
        }
        var cliente = comprobante.getFactura().getCliente();
        List<ProblemaDatoFe> problemas = datosFeValidator.validarReceptor(cliente);
        if (problemas.stream().anyMatch(ProblemaDatoFe::obligatorio)) {
            throw new IllegalStateException("El cliente " + cliente.getIdCliente()
                + " no tiene los datos de facturación electrónica obligatorios (nombre, identificación)");
        }
        for (ProblemaDatoFe p : problemas) {
            // solo id del cliente y regla: sin datos personales (T-038, ADR-044 §3)
            log.info("Cliente {}: dato opcional omitido del XML por la regla {}", cliente.getIdCliente(), p.regla());
        }
        return problemas;
    }

    private static boolean tieneProblema(List<ProblemaDatoFe> problemas, String campo) {
        return problemas.stream().anyMatch(p -> campo.equals(p.campo()));
    }

    /** CodigoActividadReceptor va en la raíz, tras CodigoActividadEmisor; se omite si no existe o no es válido. */
    private void agregarCodigoActividadReceptor(StringBuilder xml, ComprobanteElectronico comprobante,
                                                List<ProblemaDatoFe> problemas) {
        if (comprobante.getFactura() == null || comprobante.getFactura().getCliente() == null) return;
        String actividad = comprobante.getFactura().getCliente().getCodigoActividad();
        if (actividad != null && !actividad.isBlank() && !tieneProblema(problemas, "codigoActividad")) {
            xml.append("<CodigoActividadReceptor>").append(actividad.trim()).append("</CodigoActividadReceptor>");
        }
    }

    private void agregarReceptor(StringBuilder xml, ComprobanteElectronico comprobante,
                                 List<ProblemaDatoFe> problemas) {
        if (comprobante.getFactura() == null || comprobante.getFactura().getCliente() == null) return;

        var cliente = comprobante.getFactura().getCliente();

        xml.append("<Receptor>");
        xml.append("<Nombre>").append(escaparXml(cliente.getNombre().trim())).append("</Nombre>");

        xml.append("<Identificacion>");
        xml.append("<Tipo>").append(cliente.getTipoIdentificacion().getCodigo()).append("</Tipo>");
        xml.append("<Numero>").append(DatosFeValidator.numeroIdentificacion(cliente)).append("</Numero>");
        xml.append("</Identificacion>");

        // Ubicacion es opcional en el receptor, pero si va debe estar completa: se omite entera si cualquiera de sus
        // partes es inválida (catálogo, otras señas de 5 a 160) o no hay ninguna.
        boolean hayUbicacion = cliente.getProvincia() != null || notBlank(cliente.getCanton())
            || notBlank(cliente.getDistrito()) || notBlank(cliente.getOtrasSenas());
        boolean ubicacionValida = hayUbicacion
            && problemas.stream().noneMatch(p -> DatosFeValidator.esCampoDeUbicacion(p.campo()));
        if (ubicacionValida) {
            xml.append("<Ubicacion>");
            xml.append("<Provincia>").append(cliente.getProvincia().getCodigo().trim()).append("</Provincia>");
            xml.append("<Canton>").append(cliente.getCanton().trim()).append("</Canton>");
            xml.append("<Distrito>").append(cliente.getDistrito().trim()).append("</Distrito>");
            xml.append("<OtrasSenas>").append(escaparXml(cliente.getOtrasSenas().trim())).append("</OtrasSenas>");
            xml.append("</Ubicacion>");
        }

        if (!tieneProblema(problemas, "telefono")) {
            agregarTelefono(xml, cliente.getTelefono());
        }

        if (notBlank(cliente.getEmail()) && !tieneProblema(problemas, "email")) {
            xml.append("<CorreoElectronico>").append(escaparXml(cliente.getEmail().trim())).append("</CorreoElectronico>");
        }

        xml.append("</Receptor>");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Nodo Telefono del emisor o del receptor (T-033): NumTelefono solo con dígitos y sin el código de país; si el
     * número no queda con 8 a 20 dígitos, el nodo (opcional) se omite.
     */
    private void agregarTelefono(StringBuilder xml, String telefono) {
        TelefonoFe.normalizar(telefono).ifPresent(numero -> {
            xml.append("<Telefono>");
            xml.append("<CodigoPais>").append(TelefonoFe.CODIGO_PAIS).append("</CodigoPais>");
            xml.append("<NumTelefono>").append(numero).append("</NumTelefono>");
            xml.append("</Telefono>");
        });
    }

    private void agregarDetalleFactura(StringBuilder xml, ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null) return;

        xml.append("<DetalleServicio>");
        int numeroLinea = 1;

        for (LineaFactura linea : comprobante.getFactura().getLineas()) {
            var producto = linea.getProducto();

            BigDecimal cantidad = linea.getCantidad() != null ? BigDecimal.valueOf(linea.getCantidad()) : BigDecimal.ONE;
            // ADR-009: desglose por línea con el mismo componente que la factura y el asiento.
            // Todos los montos salen del desglose a 2 decimales y se expresan con 5 decimales,
            // de modo que las sumas del resumen coinciden al céntimo con la factura.
            DesgloseImpuesto desglose = desglosarLinea(linea);
            BigDecimal base = desglose.base();
            BigDecimal precioUnitarioSinIva = cantidad.signum() > 0
                ? base.divide(cantidad, 5, RoundingMode.HALF_UP)
                : base;

            // CAByS (solo del maestro, ADR-040), unidad y Detalle (ADR-041): resolución única en DatosFeProducto
            escribirLineaDetalle(xml, numeroLinea++, DatosFeProducto.codigoCabys(producto), cantidad,
                DatosFeProducto.unidadMedida(producto), DatosFeProducto.detalle(producto), precioUnitarioSinIva,
                desglose);
        }

        xml.append("</DetalleServicio>");
    }

    /** Una LineaDetalle (mismo formato en factura, tiquete y nota de crédito). */
    private void escribirLineaDetalle(StringBuilder xml, int numeroLinea, String cabys, BigDecimal cantidad,
                                      String unidadMedida, String detalle, BigDecimal precioUnitarioSinIva,
                                      DesgloseImpuesto desglose) {
        if (cabys == null || cabys.isBlank()) {
            // Sin CAByS por defecto (T-033): la emisión se rechaza antes, en ComprobanteElectronicoServiceImpl
            throw new IllegalStateException("La línea " + numeroLinea + " (" + detalle + ") no tiene código CAByS");
        }
        BigDecimal base = desglose.base();

        xml.append("<LineaDetalle>");
        xml.append("<NumeroLinea>").append(numeroLinea).append("</NumeroLinea>");
        xml.append("<CodigoCABYS>").append(cabys).append("</CodigoCABYS>");
        xml.append("<Cantidad>").append(fmt(cantidad, 3)).append("</Cantidad>");
        xml.append("<UnidadMedida>").append(unidadMedida).append("</UnidadMedida>");
        xml.append("<Detalle>").append(escaparXml(detalle)).append("</Detalle>");

        xml.append("<PrecioUnitario>").append(fmt(precioUnitarioSinIva, 5)).append("</PrecioUnitario>");
        xml.append("<MontoTotal>").append(fmt(base, 5)).append("</MontoTotal>");
        xml.append("<SubTotal>").append(fmt(base, 5)).append("</SubTotal>");
        xml.append("<BaseImponible>").append(fmt(base, 5)).append("</BaseImponible>");

        // Impuesto siempre presente — exento usa CodigoTarifaIVA=10, Tarifa=0
        xml.append("<Impuesto>");
        xml.append("<Codigo>01</Codigo>");
        xml.append("<CodigoTarifaIVA>").append(resolverCodigoTarifaIVA(desglose.tarifa())).append("</CodigoTarifaIVA>");
        xml.append("<Tarifa>").append(fmt(desglose.tarifa(), 2)).append("</Tarifa>");
        xml.append("<Monto>").append(fmt(desglose.impuesto(), 5)).append("</Monto>");
        xml.append("</Impuesto>");

        xml.append("<ImpuestoAsumidoEmisorFabrica>0.00000</ImpuestoAsumidoEmisorFabrica>");
        xml.append("<ImpuestoNeto>").append(fmt(desglose.impuesto(), 5)).append("</ImpuestoNeto>");
        xml.append("<MontoTotalLinea>").append(fmt(desglose.total(), 5)).append("</MontoTotalLinea>");

        xml.append("</LineaDetalle>");
    }

    private void agregarResumen(StringBuilder xml, ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null) return;

        var factura = comprobante.getFactura();

        // ADR-009: totales = sumas del desglose por línea (mismo componente que la factura)
        List<LineaResumen> lineasResumen = new ArrayList<>();
        for (LineaFactura linea : factura.getLineas()) {
            lineasResumen.add(new LineaResumen(desglosarLinea(linea), DatosFeProducto.codigoCabys(linea.getProducto())));
        }
        BigDecimal totalComprobante = totalComprobante(lineasResumen);
        if (factura.getTotal() != null && factura.getTotal().compareTo(totalComprobante) != 0) {
            log.warn("Factura {}: total almacenado {} difiere del total desglosado por líneas {} — el XML declara el desglose",
                factura.getNumeroFactura(), factura.getTotal(), totalComprobante);
        }

        String codigoMoneda = (factura.getMonedaFE() != null) ? factura.getMonedaFE().getCodigo() : "CRC";
        BigDecimal tipoCambio = (factura.getTipoCambio() != null) ? factura.getTipoCambio() : BigDecimal.ONE;
        String codigoMedioPago = (factura.getMedioPagoFE() != null) ? factura.getMedioPagoFE().getCodigo() : "01";
        escribirResumen(xml, lineasResumen, codigoMoneda, tipoCambio, codigoMedioPago);
    }

    /** Desglose de una línea y su CAByS, que decide si suma como servicio o como mercancía en el resumen. */
    private record LineaResumen(DesgloseImpuesto desglose, String cabys) {
        boolean servicio() {
            if (cabys == null || cabys.isBlank()) {
                throw new IllegalStateException("Línea sin código CAByS: no se puede clasificar en el resumen");
            }
            return DatosFeProducto.esServicio(cabys);
        }
    }

    private static BigDecimal totalComprobante(List<LineaResumen> lineas) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineaResumen linea : lineas) {
            DesgloseImpuesto desglose = linea.desglose();
            total = total.add(desglose.base()).add(desglose.gravado() ? desglose.impuesto() : BigDecimal.ZERO);
        }
        return total;
    }

    /**
     * ResumenFactura a partir del desglose de cada línea (mismo formato en factura, tiquete y notas). Los totales
     * gravados y exentos se reparten entre servicios y mercancías según el CAByS de cada línea (T-033, rechazos -110 y
     * -111); los parciales en cero se omiten (son opcionales en el XSD). Exonerado y no sujeto no se usan en este
     * sistema: sus totales van en cero como hasta ahora.
     */
    private void escribirResumen(StringBuilder xml, List<LineaResumen> lineas, String codigoMoneda,
                                 BigDecimal tipoCambio, String codigoMedioPago) {
        BigDecimal totalServGravados = BigDecimal.ZERO;
        BigDecimal totalServExentos = BigDecimal.ZERO;
        BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;
        BigDecimal totalMercanciasExentas = BigDecimal.ZERO;
        BigDecimal totalImpuesto = BigDecimal.ZERO;
        Map<String, BigDecimal> impuestoPorCodigoTarifa = new LinkedHashMap<>();

        for (LineaResumen linea : lineas) {
            DesgloseImpuesto desglose = linea.desglose();
            boolean servicio = linea.servicio();
            if (desglose.gravado()) {
                if (servicio) {
                    totalServGravados = totalServGravados.add(desglose.base());
                } else {
                    totalMercanciasGravadas = totalMercanciasGravadas.add(desglose.base());
                }
                totalImpuesto = totalImpuesto.add(desglose.impuesto());
                impuestoPorCodigoTarifa.merge(resolverCodigoTarifaIVA(desglose.tarifa()), desglose.impuesto(), BigDecimal::add);
            } else if (servicio) {
                totalServExentos = totalServExentos.add(desglose.base());
            } else {
                totalMercanciasExentas = totalMercanciasExentas.add(desglose.base());
            }
        }
        if (impuestoPorCodigoTarifa.isEmpty()) {
            impuestoPorCodigoTarifa.put("10", BigDecimal.ZERO); // exento por defecto
        }

        BigDecimal totalGravado = totalServGravados.add(totalMercanciasGravadas);
        BigDecimal totalExento = totalServExentos.add(totalMercanciasExentas);
        BigDecimal totalVenta = totalGravado.add(totalExento);
        BigDecimal totalVentaNeta = totalVenta;
        BigDecimal totalComprobante = totalVentaNeta.add(totalImpuesto);

        xml.append("<ResumenFactura>");
        xml.append("<CodigoTipoMoneda>");
        xml.append("<CodigoMoneda>").append(codigoMoneda).append("</CodigoMoneda>");
        xml.append("<TipoCambio>").append(fmt(tipoCambio, 5)).append("</TipoCambio>");
        xml.append("</CodigoTipoMoneda>");

        if (totalServGravados.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("<TotalServGravados>").append(fmt(totalServGravados, 5)).append("</TotalServGravados>");
        }
        if (totalServExentos.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("<TotalServExentos>").append(fmt(totalServExentos, 5)).append("</TotalServExentos>");
        }
        if (totalMercanciasGravadas.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("<TotalMercanciasGravadas>").append(fmt(totalMercanciasGravadas, 5)).append("</TotalMercanciasGravadas>");
        }
        if (totalMercanciasExentas.compareTo(BigDecimal.ZERO) > 0) {
            xml.append("<TotalMercanciasExentas>").append(fmt(totalMercanciasExentas, 5)).append("</TotalMercanciasExentas>");
        }

        xml.append("<TotalGravado>").append(fmt(totalGravado, 5)).append("</TotalGravado>");
        xml.append("<TotalExento>").append(fmt(totalExento, 5)).append("</TotalExento>");
        xml.append("<TotalExonerado>0.00000</TotalExonerado>");
        xml.append("<TotalNoSujeto>0.00000</TotalNoSujeto>");
        xml.append("<TotalVenta>").append(fmt(totalVenta, 5)).append("</TotalVenta>");
        xml.append("<TotalVentaNeta>").append(fmt(totalVentaNeta, 5)).append("</TotalVentaNeta>");

        // TotalDesgloseImpuesto — siempre presente (con monto 0 si todo es exento)
        impuestoPorCodigoTarifa.forEach((codigoTarifa, monto) -> {
            xml.append("<TotalDesgloseImpuesto>");
            xml.append("<Codigo>01</Codigo>");
            xml.append("<CodigoTarifaIVA>").append(codigoTarifa).append("</CodigoTarifaIVA>");
            xml.append("<TotalMontoImpuesto>").append(fmt(monto, 5)).append("</TotalMontoImpuesto>");
            xml.append("</TotalDesgloseImpuesto>");
        });

        xml.append("<TotalImpuesto>").append(fmt(totalImpuesto, 5)).append("</TotalImpuesto>");

        // MedioPago dentro de ResumenFactura como elemento complejo
        xml.append("<MedioPago>");
        xml.append("<TipoMedioPago>").append(codigoMedioPago).append("</TipoMedioPago>");
        xml.append("<TotalMedioPago>").append(fmt(totalComprobante, 5)).append("</TotalMedioPago>");
        xml.append("</MedioPago>");

        xml.append("<TotalComprobante>").append(fmt(totalComprobante, 5)).append("</TotalComprobante>");
        xml.append("</ResumenFactura>");
    }

    // =====================================================================
    // FE field resolvers — null-safe navigation through ArticuloMaestro (XML-1)
    // =====================================================================

    /** Resolves aplicaOtroImpuesto: reads from master when present, falls back to deprecated Producto field. */
    @SuppressWarnings("deprecation")
    private Boolean resolveAplicaOtroImpuesto(Producto produto) {
        return Optional.ofNullable(produto.getArticuloMaestro())
                .map(ArticuloMaestro::getAplicaOtroImpuesto)
                .orElse(produto.getAplicaOtroImpuesto());
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    /** Desglose de IVA de una línea con el componente único (ADR-009). */
    private DesgloseImpuesto desglosarLinea(LineaFactura linea) {
        BigDecimal importe = linea.getSubtotal();
        if (importe == null) {
            BigDecimal precio = linea.getPrecioUnitario() != null ? linea.getPrecioUnitario() : BigDecimal.ZERO;
            BigDecimal cantidad = linea.getCantidad() != null ? BigDecimal.valueOf(linea.getCantidad()) : BigDecimal.ONE;
            importe = precio.multiply(cantidad);
        }
        return calculadoraImpuestoService.desglosarLinea(linea.getProducto(), importe);
    }

    /**
     * Resuelve el CodigoTarifaIVA (Hacienda v4.4) según el porcentaje:
     * 10=exenta(0%), 09=0,5%, 02=1%, 03=2%, 04=4%, 07=8%, 08=13% (tarifa general).
     */
    private String resolverCodigoTarifaIVA(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) == 0) return "10";
        if (porcentaje.compareTo(new BigDecimal("0.5")) == 0) return "09";
        if (porcentaje.compareTo(new BigDecimal("1")) == 0)  return "02";
        if (porcentaje.compareTo(new BigDecimal("2")) == 0)  return "03";
        if (porcentaje.compareTo(new BigDecimal("4")) == 0)  return "04";
        if (porcentaje.compareTo(new BigDecimal("8")) == 0)  return "07";
        return "08"; // 13% tarifa general
    }

    private String fmt(BigDecimal value, int decimals) {
        if (value == null) return "0." + "0".repeat(decimals);
        return value.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    /** La fecha de emisión es la hora local de Costa Rica (HoraFiscal); se expresa con su desfase real (T-033). */
    private String formatearFecha(LocalDateTime fechaEmision) {
        return HoraFiscal.formatear(fechaEmision);
    }

    private String obtenerProveedorSistemas(ComprobanteElectronico comprobante) {
        var empresa = comprobante.getEmpresa();
        if (empresa != null && empresa.getRuc() != null) {
            return empresa.getRuc().replaceAll("[^0-9]", "");
        }
        return "";
    }

    /**
     * ADR-021: el código de actividad se envía tal como está registrado (CIIU 4 de Hacienda, p. ej. 4773.0);
     * sin código, la emisión falla en lugar de usar un valor por defecto.
     */
    String obtenerActividadEconomica(ComprobanteElectronico comprobante) {
        var empresa = comprobante.getEmpresa();
        if (empresa == null || empresa.getCodigoActividad() == null || empresa.getCodigoActividad().isBlank()) {
            throw new IllegalStateException("La empresa no tiene código de actividad económica configurado");
        }
        return empresa.getCodigoActividad().trim();
    }

    /**
     * Extrae el NumeroConsecutivo (20 dígitos) de la Clave (50 dígitos).
     * Formato clave: País(3)+Día(2)+Mes(2)+Año(2)+Cédula(12)+Consecutivo(20)+Situación(1)+Seguridad(8)
     * El consecutivo vive en las posiciones 21-40 (0-indexed).
     */
    private String extraerConsecutivoDeClave(String clave) {
        if (clave != null && clave.length() == 50) {
            return clave.substring(21, 41);
        }
        log.warn("Clave inválida para extraer consecutivo: '{}' — usando valor vacío", clave);
        return "00000000000000000000";
    }

    private String escaparXml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
