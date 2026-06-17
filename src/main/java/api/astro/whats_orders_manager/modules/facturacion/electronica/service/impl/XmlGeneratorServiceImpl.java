package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Implementación del servicio de generación de XML.
 * Genera XML según especificación v4.4 de Hacienda de Costa Rica.
 */
@Service
@Slf4j
public class XmlGeneratorServiceImpl implements XmlGeneratorService {

    private static final String NAMESPACE = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final ZoneOffset CR_ZONE_OFFSET = ZoneOffset.of("-06:00");

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
        xml.append("<NumeroConsecutivo>").append(extraerConsecutivoDeClave(comprobante.getClaveNumerica())).append("</NumeroConsecutivo>");
        xml.append("<FechaEmision>").append(formatearFecha(comprobante.getFechaEmision())).append("</FechaEmision>");

        agregarEmisor(xml, comprobante);
        agregarReceptor(xml, comprobante);

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

        xml.append("</FacturaElectronica>");

        String xmlGenerado = xml.toString();

        if (!XmlValidator.validarXml(xmlGenerado)) {
            log.error("XML generado no es válido");
            throw new IllegalStateException("XML generado no es válido");
        }

        return xmlGenerado;
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

    @Override
    public String generarXmlNotaCredito(ComprobanteElectronico comprobante) {
        log.info("Generando XML de nota de crédito: {}", comprobante.getClaveNumerica());

        String nsNC = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/notaCreditoElectronica";
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<NotaCreditoElectronica xmlns=\"").append(nsNC).append("\">");

        xml.append("<Clave>").append(comprobante.getClaveNumerica()).append("</Clave>");
        xml.append("<CodigoActividadEmisor>").append(obtenerActividadEconomica(comprobante)).append("</CodigoActividadEmisor>");
        xml.append("<NumeroConsecutivo>").append(extraerConsecutivoDeClave(comprobante.getClaveNumerica())).append("</NumeroConsecutivo>");
        xml.append("<FechaEmision>").append(formatearFecha(comprobante.getFechaEmision())).append("</FechaEmision>");

        xml.append("<InformacionReferencia>");
        xml.append("<TipoDoc>01</TipoDoc>");
        xml.append("<Numero>").append(comprobante.getFactura() != null ? comprobante.getFactura().getNumeroFactura() : "").append("</Numero>");
        xml.append("<FechaEmision>").append(formatearFecha(comprobante.getFechaEmision())).append("</FechaEmision>");
        xml.append("<Codigo>01</Codigo>");
        xml.append("<Razon>Anulación</Razon>");
        xml.append("</InformacionReferencia>");

        agregarEmisor(xml, comprobante);
        agregarReceptor(xml, comprobante);
        xml.append("<CondicionVenta>01</CondicionVenta>");
        agregarDetalleFactura(xml, comprobante);
        agregarResumen(xml, comprobante);

        xml.append("</NotaCreditoElectronica>");
        return xml.toString();
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
        agregarReceptor(xml, comprobante);
        xml.append("<CondicionVenta>01</CondicionVenta>");
        agregarDetalleFactura(xml, comprobante);
        agregarResumen(xml, comprobante);

        xml.append("</NotaDebitoElectronica>");
        return xml.toString();
    }

    @Override
    public boolean validarXml(String xml, String tipoComprobante) {
        try {
            TipoComprobanteElectronico tipo;
            if (xml.contains("<FacturaElectronica")) {
                tipo = TipoComprobanteElectronico.FACTURA_ELECTRONICA;
            } else if (xml.contains("<TiqueteElectronico")) {
                tipo = TipoComprobanteElectronico.TIQUETE_ELECTRONICO;
            } else if (xml.contains("<NotaCreditoElectronica")) {
                tipo = TipoComprobanteElectronico.NOTA_CREDITO;
            } else if (xml.contains("<NotaDebitoElectronica")) {
                tipo = TipoComprobanteElectronico.NOTA_DEBITO;
            } else {
                return XmlValidator.validarXml(xml);
            }
            return XmlValidator.validarContraXsd(xml, tipo);
        } catch (Exception e) {
            log.error("Error validando XML: {}", e.getMessage());
            return false;
        }
    }

    // =====================================================================
    // Secciones comunes
    // =====================================================================

    private void agregarEmisor(StringBuilder xml, ComprobanteElectronico comprobante) {
        var empresa = comprobante.getEmpresa();

        xml.append("<Emisor>");
        xml.append("<Nombre>").append(escaparXml(empresa.getNombreEmpresa())).append("</Nombre>");

        String tipoId = empresa.getTipoIdentificacion() != null ? empresa.getTipoIdentificacion().getCodigo() : "02";
        xml.append("<Identificacion>");
        xml.append("<Tipo>").append(tipoId).append("</Tipo>");
        xml.append("<Numero>").append(empresa.getRuc().replaceAll("[^0-9]", "")).append("</Numero>");
        xml.append("</Identificacion>");

        String nombreComercial = empresa.getNombreComercialFe() != null ? empresa.getNombreComercialFe()
            : (empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombreEmpresa());
        xml.append("<NombreComercial>").append(escaparXml(nombreComercial)).append("</NombreComercial>");

        xml.append("<Ubicacion>");
        xml.append("<Provincia>").append(empresa.getCodigoProvincia() != null ? empresa.getCodigoProvincia() : "1").append("</Provincia>");
        xml.append("<Canton>").append(empresa.getCanton() != null ? empresa.getCanton() : "01").append("</Canton>");
        xml.append("<Distrito>").append(empresa.getDistrito() != null ? empresa.getDistrito() : "01").append("</Distrito>");
        if (empresa.getBarrio() != null && !empresa.getBarrio().isEmpty()) {
            xml.append("<Barrio>").append(escaparXml(empresa.getBarrio())).append("</Barrio>");
        }
        String otrasSenas = empresa.getOtrasSenas();
        if (otrasSenas == null || otrasSenas.isBlank()) otrasSenas = empresa.getDireccion();
        if (otrasSenas == null || otrasSenas.trim().length() < 5) {
            log.warn("Empresa sin OtrasSenas válido (mínimo 5 chars) — actualizá los datos de la empresa");
            otrasSenas = "Sin especificar";
        }
        xml.append("<OtrasSenas>").append(escaparXml(otrasSenas.trim())).append("</OtrasSenas>");
        xml.append("</Ubicacion>");

        if (empresa.getTelefono() != null && !empresa.getTelefono().isBlank()) {
            xml.append("<Telefono>");
            xml.append("<CodigoPais>506</CodigoPais>");
            xml.append("<NumTelefono>").append(empresa.getTelefono().replaceAll("[^0-9]", "")).append("</NumTelefono>");
            xml.append("</Telefono>");
        }

        xml.append("<CorreoElectronico>").append(empresa.getEmail() != null ? empresa.getEmail() : "").append("</CorreoElectronico>");
        xml.append("</Emisor>");
    }

    private void agregarReceptor(StringBuilder xml, ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null || comprobante.getFactura().getCliente() == null) return;

        var cliente = comprobante.getFactura().getCliente();
        String numeroIdentificacion = cliente.getNumeroIdentificacion() != null && !cliente.getNumeroIdentificacion().isBlank()
            ? cliente.getNumeroIdentificacion() : cliente.getIdentificacion();

        xml.append("<Receptor>");
        xml.append("<Nombre>").append(escaparXml(cliente.getNombre())).append("</Nombre>");

        String tipoId = cliente.getTipoIdentificacion() != null
            ? cliente.getTipoIdentificacion().getCodigo()
            : determinarTipoIdentificacion(numeroIdentificacion);
        xml.append("<Identificacion>");
        xml.append("<Tipo>").append(tipoId).append("</Tipo>");
        xml.append("<Numero>").append(numeroIdentificacion != null ? numeroIdentificacion.replaceAll("[^0-9]", "") : "").append("</Numero>");
        xml.append("</Identificacion>");

        // OtrasSenas is required (no minOccurs="0") and minLength=5 inside UbicacionType.
        // Since Receptor.Ubicacion is optional (minOccurs="0"), skip the whole block if any
        // required field is missing or OtrasSenas is shorter than 5 chars.
        String receptorOtrasSenas = cliente.getOtrasSenas() != null ? cliente.getOtrasSenas().trim() : null;
        boolean ubicacionValida = cliente.getProvincia() != null
            && cliente.getCanton() != null
            && cliente.getDistrito() != null
            && receptorOtrasSenas != null && receptorOtrasSenas.length() >= 5;
        if (ubicacionValida) {
            xml.append("<Ubicacion>");
            xml.append("<Provincia>").append(cliente.getProvincia().getCodigo()).append("</Provincia>");
            xml.append("<Canton>").append(cliente.getCanton()).append("</Canton>");
            xml.append("<Distrito>").append(cliente.getDistrito()).append("</Distrito>");
            xml.append("<OtrasSenas>").append(escaparXml(receptorOtrasSenas)).append("</OtrasSenas>");
            xml.append("</Ubicacion>");
        }

        if (cliente.getTelefono() != null && !cliente.getTelefono().isBlank()) {
            xml.append("<Telefono>");
            xml.append("<CodigoPais>506</CodigoPais>");
            xml.append("<NumTelefono>").append(cliente.getTelefono().replaceAll("[^0-9]", "")).append("</NumTelefono>");
            xml.append("</Telefono>");
        }

        if (cliente.getEmail() != null) {
            xml.append("<CorreoElectronico>").append(cliente.getEmail()).append("</CorreoElectronico>");
        }

        xml.append("</Receptor>");
    }

    private void agregarDetalleFactura(StringBuilder xml, ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null) return;

        xml.append("<DetalleServicio>");
        int numeroLinea = 1;

        for (LineaFactura linea : comprobante.getFactura().getLineas()) {
            var producto = linea.getProducto();

            xml.append("<LineaDetalle>");
            xml.append("<NumeroLinea>").append(numeroLinea++).append("</NumeroLinea>");

            // CodigoCABYS en mayúsculas — obligatorio en v4.4
            String cabys = (producto.getCodigoCabys() != null && !producto.getCodigoCabys().isEmpty())
                ? producto.getCodigoCabys() : "8522000000000";
            xml.append("<CodigoCABYS>").append(cabys).append("</CodigoCABYS>");

            BigDecimal cantidad = linea.getCantidad() != null ? BigDecimal.valueOf(linea.getCantidad()) : BigDecimal.ONE;
            xml.append("<Cantidad>").append(fmt(cantidad, 3)).append("</Cantidad>");

            String unidadMedida = "Unid";
            if (producto.getPresentacion() != null && producto.getPresentacion().getCodigoUnidadFE() != null) {
                unidadMedida = producto.getPresentacion().getCodigoUnidadFE();
            }
            xml.append("<UnidadMedida>").append(unidadMedida).append("</UnidadMedida>");
            xml.append("<Detalle>").append(escaparXml(producto.getDescripcion())).append("</Detalle>");

            BigDecimal precioUnitario = linea.getPrecioUnitario() != null ? linea.getPrecioUnitario() : BigDecimal.ZERO;
            BigDecimal montoTotal = precioUnitario.multiply(cantidad).setScale(5, RoundingMode.HALF_UP);
            BigDecimal subtotal = linea.getSubtotal() != null ? linea.getSubtotal().setScale(5, RoundingMode.HALF_UP) : montoTotal;

            xml.append("<PrecioUnitario>").append(fmt(precioUnitario, 5)).append("</PrecioUnitario>");
            xml.append("<MontoTotal>").append(fmt(montoTotal, 5)).append("</MontoTotal>");
            xml.append("<SubTotal>").append(fmt(subtotal, 5)).append("</SubTotal>");
            xml.append("<BaseImponible>").append(fmt(subtotal, 5)).append("</BaseImponible>");

            boolean esGravado = Boolean.TRUE.equals(producto.getGravado());
            BigDecimal porcentajeIVA = esGravado && producto.getPorcentajeImpuesto() != null
                ? producto.getPorcentajeImpuesto() : BigDecimal.ZERO;
            BigDecimal montoImpuesto = subtotal.multiply(porcentajeIVA)
                .divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP);

            // Impuesto siempre presente — exento usa CodigoTarifaIVA=10, Tarifa=0
            xml.append("<Impuesto>");
            xml.append("<Codigo>01</Codigo>");
            xml.append("<CodigoTarifaIVA>").append(resolverCodigoTarifaIVA(porcentajeIVA)).append("</CodigoTarifaIVA>");
            xml.append("<Tarifa>").append(porcentajeIVA.toPlainString()).append("</Tarifa>");
            xml.append("<Monto>").append(fmt(montoImpuesto, 5)).append("</Monto>");
            xml.append("</Impuesto>");

            xml.append("<ImpuestoAsumidoEmisorFabrica>0.00000</ImpuestoAsumidoEmisorFabrica>");
            xml.append("<ImpuestoNeto>").append(fmt(montoImpuesto, 5)).append("</ImpuestoNeto>");
            xml.append("<MontoTotalLinea>").append(fmt(subtotal.add(montoImpuesto), 5)).append("</MontoTotalLinea>");

            xml.append("</LineaDetalle>");
        }

        xml.append("</DetalleServicio>");
    }

    private void agregarResumen(StringBuilder xml, ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null) return;

        var factura = comprobante.getFactura();

        // Calcular totales desde líneas
        BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;
        BigDecimal totalMercanciasExentas = BigDecimal.ZERO;
        BigDecimal totalImpuesto = BigDecimal.ZERO;
        String codigoTarifaPrincipal = "10"; // exento por defecto

        for (LineaFactura linea : factura.getLineas()) {
            var producto = linea.getProducto();
            boolean esGravado = Boolean.TRUE.equals(producto.getGravado());
            BigDecimal subtotal = linea.getSubtotal() != null ? linea.getSubtotal() : BigDecimal.ZERO;

            if (esGravado) {
                totalMercanciasGravadas = totalMercanciasGravadas.add(subtotal);
                BigDecimal porc = producto.getPorcentajeImpuesto() != null ? producto.getPorcentajeImpuesto() : new BigDecimal("13");
                totalImpuesto = totalImpuesto.add(subtotal.multiply(porc).divide(new BigDecimal("100"), 5, RoundingMode.HALF_UP));
                codigoTarifaPrincipal = resolverCodigoTarifaIVA(porc);
            } else {
                totalMercanciasExentas = totalMercanciasExentas.add(subtotal);
            }
        }

        BigDecimal totalGravado = totalMercanciasGravadas;
        BigDecimal totalExento = totalMercanciasExentas;
        BigDecimal totalVenta = totalGravado.add(totalExento);
        BigDecimal totalVentaNeta = totalVenta;
        BigDecimal totalComprobante = totalVentaNeta.add(totalImpuesto);

        String codigoMoneda = (factura.getMonedaFE() != null) ? factura.getMonedaFE().getCodigo() : "CRC";
        BigDecimal tipoCambio = (factura.getTipoCambio() != null) ? factura.getTipoCambio() : BigDecimal.ONE;

        xml.append("<ResumenFactura>");
        xml.append("<CodigoTipoMoneda>");
        xml.append("<CodigoMoneda>").append(codigoMoneda).append("</CodigoMoneda>");
        xml.append("<TipoCambio>").append(fmt(tipoCambio, 5)).append("</TipoCambio>");
        xml.append("</CodigoTipoMoneda>");

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
        xml.append("<TotalDesgloseImpuesto>");
        xml.append("<Codigo>01</Codigo>");
        xml.append("<CodigoTarifaIVA>").append(codigoTarifaPrincipal).append("</CodigoTarifaIVA>");
        xml.append("<TotalMontoImpuesto>").append(fmt(totalImpuesto, 5)).append("</TotalMontoImpuesto>");
        xml.append("</TotalDesgloseImpuesto>");

        xml.append("<TotalImpuesto>").append(fmt(totalImpuesto, 5)).append("</TotalImpuesto>");

        // MedioPago dentro de ResumenFactura como elemento complejo
        String codigoMedioPago = (factura.getMedioPagoFE() != null) ? factura.getMedioPagoFE().getCodigo() : "01";
        xml.append("<MedioPago>");
        xml.append("<TipoMedioPago>").append(codigoMedioPago).append("</TipoMedioPago>");
        xml.append("<TotalMedioPago>").append(fmt(totalComprobante, 5)).append("</TotalMedioPago>");
        xml.append("</MedioPago>");

        xml.append("<TotalComprobante>").append(fmt(totalComprobante, 5)).append("</TotalComprobante>");
        xml.append("</ResumenFactura>");
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    /**
     * Resuelve el CodigoTarifaIVA según el porcentaje:
     * 10=exento(0%), 03=1%, 04=2%, 05=4%, 06=8%, 07=13%
     */
    private String resolverCodigoTarifaIVA(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) == 0) return "10";
        if (porcentaje.compareTo(new BigDecimal("1")) == 0)  return "03";
        if (porcentaje.compareTo(new BigDecimal("2")) == 0)  return "04";
        if (porcentaje.compareTo(new BigDecimal("4")) == 0)  return "05";
        if (porcentaje.compareTo(new BigDecimal("8")) == 0)  return "06";
        return "07"; // 13% general
    }

    private String fmt(BigDecimal value, int decimals) {
        if (value == null) return "0." + "0".repeat(decimals);
        return value.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatearFecha(LocalDateTime fechaEmision) {
        if (fechaEmision == null) throw new IllegalStateException("Fecha de emisión no disponible");
        return fechaEmision.atOffset(CR_ZONE_OFFSET).format(DATE_FORMATTER);
    }

    private String obtenerProveedorSistemas(ComprobanteElectronico comprobante) {
        var empresa = comprobante.getEmpresa();
        if (empresa != null && empresa.getRuc() != null) {
            return empresa.getRuc().replaceAll("[^0-9]", "");
        }
        return "";
    }

    private String obtenerActividadEconomica(ComprobanteElectronico comprobante) {
        var empresa = comprobante.getEmpresa();
        if (empresa != null && empresa.getCodigoActividad() != null) {
            return empresa.getCodigoActividad();
        }
        log.warn("Empresa sin código de actividad económica. Usando genérico.");
        return "620100";
    }

    private String determinarTipoIdentificacion(String identificacion) {
        if (identificacion == null) return "01";
        String numerico = identificacion.replaceAll("[^0-9]", "");
        if (numerico.length() == 10) return "02"; // Jurídica
        if (numerico.length() == 9)  return "01"; // Física
        return "01";
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
