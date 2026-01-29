package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Implementación del servicio de generación de XML.
 * Genera XML según especificación v4.4 de Hacienda de Costa Rica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Service
@Slf4j
public class XmlGeneratorServiceImpl implements XmlGeneratorService {
    
    private static final String NAMESPACE = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    
    @Override
    public String generarXmlFactura(ComprobanteElectronico comprobante) {
        log.info("Generando XML de factura electrónica: {}", comprobante.getClaveNumerica());
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<FacturaElectronica xmlns=\"").append(NAMESPACE).append("\" ");
        xml.append("xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" ");
        xml.append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n");
        
        // Clave
        xml.append("  <Clave>").append(comprobante.getClaveNumerica()).append("</Clave>\n");
        
        // Actividad económica
        xml.append("  <CodigoActividad>").append(obtenerActividadEconomica(comprobante)).append("</CodigoActividad>\n");
        
        // Número de consecutivo
        xml.append("  <NumeroConsecutivo>").append(comprobante.getConsecutivo()).append("</NumeroConsecutivo>\n");
        
        // Fecha de emisión
        xml.append("  <FechaEmision>").append(comprobante.getFechaEmision().format(DATE_FORMATTER)).append("</FechaEmision>\n");
        
        // Emisor
        agregarEmisor(xml, comprobante);
        
        // Receptor
        agregarReceptor(xml, comprobante);
        
        // Condición de venta
        xml.append("  <CondicionVenta>").append(obtenerCondicionVenta(comprobante)).append("</CondicionVenta>\n");
        
        // Plazo de crédito
        xml.append("  <PlazoCredito>0</PlazoCredito>\n");
        
        // Medio de pago
        xml.append("  <MedioPago>01</MedioPago>\n");
        
        // Detalle de la factura
        agregarDetalleFactura(xml, comprobante);
        
        // Resumen
        agregarResumen(xml, comprobante);
        
        // Normativa (opcional)
        xml.append("  <Otros>\n");
        xml.append("    <OtroTexto>Factura generada por Sistema ERP</OtroTexto>\n");
        xml.append("  </Otros>\n");
        
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
        
        // Similar a factura pero con estructura simplificada
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<TiqueteElectronico xmlns=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/tiqueteElectronico\">\n");
        
        xml.append("  <Clave>").append(comprobante.getClaveNumerica()).append("</Clave>\n");
        xml.append("  <NumeroConsecutivo>").append(comprobante.getConsecutivo()).append("</NumeroConsecutivo>\n");
        xml.append("  <FechaEmision>").append(comprobante.getFechaEmision().format(DATE_FORMATTER)).append("</FechaEmision>\n");
        
        agregarEmisor(xml, comprobante);
        agregarDetalleFactura(xml, comprobante);
        agregarResumen(xml, comprobante);
        
        xml.append("</TiqueteElectronico>");
        
        return xml.toString();
    }
    
    @Override
    public String generarXmlNotaCredito(ComprobanteElectronico comprobante) {
        log.info("Generando XML de nota de crédito: {}", comprobante.getClaveNumerica());
        
        // TODO: Implementar generación completa de nota de crédito
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<NotaCreditoElectronica xmlns=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/notaCreditoElectronica\">\n");
        
        xml.append("  <Clave>").append(comprobante.getClaveNumerica()).append("</Clave>\n");
        xml.append("  <NumeroConsecutivo>").append(comprobante.getConsecutivo()).append("</NumeroConsecutivo>\n");
        xml.append("  <FechaEmision>").append(comprobante.getFechaEmision().format(DATE_FORMATTER)).append("</FechaEmision>\n");
        
        // Referencia al documento que se está acreditando
        xml.append("  <InformacionReferencia>\n");
        xml.append("    <TipoDoc>01</TipoDoc>\n");
        xml.append("    <Numero>").append(comprobante.getFactura() != null ? comprobante.getFactura().getNumeroFactura() : "").append("</Numero>\n");
        xml.append("    <FechaEmision>").append(comprobante.getFechaEmision().format(DATE_FORMATTER)).append("</FechaEmision>\n");
        xml.append("    <Codigo>01</Codigo>\n");
        xml.append("    <Razon>Anulación</Razon>\n");
        xml.append("  </InformacionReferencia>\n");
        
        agregarEmisor(xml, comprobante);
        agregarReceptor(xml, comprobante);
        agregarDetalleFactura(xml, comprobante);
        agregarResumen(xml, comprobante);
        
        xml.append("</NotaCreditoElectronica>");
        
        return xml.toString();
    }
    
    @Override
    public String generarXmlNotaDebito(ComprobanteElectronico comprobante) {
        log.info("Generando XML de nota de débito: {}", comprobante.getClaveNumerica());
        
        // Similar a nota de crédito
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<NotaDebitoElectronica xmlns=\"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/notaDebitoElectronica\">\n");
        
        xml.append("  <Clave>").append(comprobante.getClaveNumerica()).append("</Clave>\n");
        xml.append("  <NumeroConsecutivo>").append(comprobante.getConsecutivo()).append("</NumeroConsecutivo>\n");
        xml.append("  <FechaEmision>").append(comprobante.getFechaEmision().format(DATE_FORMATTER)).append("</FechaEmision>\n");
        
        agregarEmisor(xml, comprobante);
        agregarReceptor(xml, comprobante);
        agregarDetalleFactura(xml, comprobante);
        agregarResumen(xml, comprobante);
        
        xml.append("</NotaDebitoElectronica>");
        
        return xml.toString();
    }
    
    @Override
    public boolean validarXml(String xml, String tipoComprobante) {
        try {
            // Determinar tipo de comprobante
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
                log.warn("No se pudo determinar tipo de comprobante del XML. Validando estructura básica.");
                return XmlValidator.validarXml(xml);
            }
            
            // Validar contra XSD específico
            return XmlValidator.validarContraXsd(xml, tipo);
            
        } catch (Exception e) {
            log.error("Error validando XML: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Agrega sección de emisor al XML.
     */
    private void agregarEmisor(StringBuilder xml, ComprobanteElectronico comprobante) {
        var empresa = comprobante.getEmpresa();
        
        xml.append("  <Emisor>\n");
        xml.append("    <Nombre>").append(escaparXml(empresa.getNombreEmpresa())).append("</Nombre>\n");
        xml.append("    <Identificacion>\n");
        xml.append("      <Tipo>02</Tipo>\n"); // Cédula jurídica
        xml.append("      <Numero>").append(empresa.getRuc().replaceAll("[^0-9]", "")).append("</Numero>\n");
        xml.append("    </Identificacion>\n");
        xml.append("    <NombreComercial>").append(escaparXml(empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombreEmpresa())).append("</NombreComercial>\n");
        xml.append("    <Ubicacion>\n");
        xml.append("      <Provincia>1</Provincia>\n");
        xml.append("      <Canton>01</Canton>\n");
        xml.append("      <Distrito>01</Distrito>\n");
        xml.append("      <OtrasSenas>").append(escaparXml(empresa.getDireccion())).append("</OtrasSenas>\n");
        xml.append("    </Ubicacion>\n");
        xml.append("    <Telefono>\n");
        xml.append("      <CodigoPais>506</CodigoPais>\n");
        xml.append("      <NumTelefono>").append(empresa.getTelefono() != null ? empresa.getTelefono().replaceAll("[^0-9]", "") : "00000000").append("</NumTelefono>\n");
        xml.append("    </Telefono>\n");
        xml.append("    <CorreoElectronico>").append(empresa.getEmail()).append("</CorreoElectronico>\n");
        xml.append("  </Emisor>\n");
    }
    
    /**
     * Agrega sección de receptor al XML.
     */
    private void agregarReceptor(StringBuilder xml, ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null || comprobante.getFactura().getCliente() == null) {
            return;
        }
        
        var cliente = comprobante.getFactura().getCliente();
        
        xml.append("  <Receptor>\n");
        xml.append("    <Nombre>").append(escaparXml(cliente.getNombre())).append("</Nombre>\n");
        xml.append("    <Identificacion>\n");
        xml.append("      <Tipo>").append(determinarTipoIdentificacion(cliente.getIdentificacion())).append("</Tipo>\n");
        xml.append("      <Numero>").append(cliente.getIdentificacion().replaceAll("[^0-9]", "")).append("</Numero>\n");
        xml.append("    </Identificacion>\n");
        
        // Cliente no tiene campo telefono directo, usar telefono de usuario si existe
        if (cliente.getUsuario() != null && cliente.getUsuario().getTelefono() != null) {
            xml.append("    <Telefono>\n");
            xml.append("      <CodigoPais>506</CodigoPais>\n");
            xml.append("      <NumTelefono>").append(cliente.getUsuario().getTelefono().replaceAll("[^0-9]", "")).append("</NumTelefono>\n");
            xml.append("    </Telefono>\n");
        }
        
        if (cliente.getEmail() != null) {
            xml.append("    <CorreoElectronico>").append(cliente.getEmail()).append("</CorreoElectronico>\n");
        }
        
        xml.append("  </Receptor>\n");
    }
    
    /**
     * Agrega detalle de líneas de la factura.
     */
    private void agregarDetalleFactura(StringBuilder xml, ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null) {
            return;
        }
        
        xml.append("  <DetalleServicio>\n");
        
        int numeroLinea = 1;
        for (LineaFactura linea : comprobante.getFactura().getLineas()) {
            xml.append("    <LineaDetalle>\n");
            xml.append("      <NumeroLinea>").append(numeroLinea++).append("</NumeroLinea>\n");
            xml.append("      <Codigo>").append(linea.getProducto().getCodigo()).append("</Codigo>\n");
            xml.append("      <Cantidad>").append(linea.getCantidad()).append("</Cantidad>\n");
            xml.append("      <UnidadMedida>Unid</UnidadMedida>\n");
            xml.append("      <Detalle>").append(escaparXml(linea.getProducto().getDescripcion())).append("</Detalle>\n");
            xml.append("      <PrecioUnitario>").append(linea.getPrecioUnitario()).append("</PrecioUnitario>\n");
            xml.append("      <MontoTotal>").append(linea.getSubtotal()).append("</MontoTotal>\n");
            xml.append("      <SubTotal>").append(linea.getSubtotal()).append("</SubTotal>\n");
            
            // Impuestos (calcular 13% del subtotal)
            BigDecimal impuesto = linea.getSubtotal().multiply(new BigDecimal("0.13"));
            if (impuesto.compareTo(BigDecimal.ZERO) > 0) {
                xml.append("      <Impuesto>\n");
                xml.append("        <Codigo>01</Codigo>\n"); // IVA
                xml.append("        <CodigoTarifa>08</CodigoTarifa>\n"); // 13%
                xml.append("        <Tarifa>13</Tarifa>\n");
                xml.append("        <Monto>").append(impuesto).append("</Monto>\n");
                xml.append("      </Impuesto>\n");
            }
            
            BigDecimal totalLinea = linea.getSubtotal().add(impuesto);
            xml.append("      <MontoTotalLinea>").append(totalLinea).append("</MontoTotalLinea>\n");
            xml.append("    </LineaDetalle>\n");
        }
        
        xml.append("  </DetalleServicio>\n");
    }
    
    /**
     * Agrega resumen de totales.
     */
    private void agregarResumen(StringBuilder xml, ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null) {
            return;
        }
        
        var factura = comprobante.getFactura();
        
        xml.append("  <ResumenFactura>\n");
        xml.append("    <CodigoTipoMoneda>\n");
        xml.append("      <CodigoMoneda>CRC</CodigoMoneda>\n");
        xml.append("      <TipoCambio>1.00000</TipoCambio>\n");
        xml.append("    </CodigoTipoMoneda>\n");
        xml.append("    <TotalServGravados>").append(factura.getSubtotal()).append("</TotalServGravados>\n");
        xml.append("    <TotalServExentos>0.00</TotalServExentos>\n");
        xml.append("    <TotalMercanciasGravadas>0.00</TotalMercanciasGravadas>\n");
        xml.append("    <TotalMercanciasExentas>0.00</TotalMercanciasExentas>\n");
        xml.append("    <TotalGravado>").append(factura.getSubtotal()).append("</TotalGravado>\n");
        xml.append("    <TotalExento>0.00</TotalExento>\n");
        xml.append("    <TotalVenta>").append(factura.getSubtotal()).append("</TotalVenta>\n");
        xml.append("    <TotalDescuentos>0.00</TotalDescuentos>\n");
        xml.append("    <TotalVentaNeta>").append(factura.getSubtotal()).append("</TotalVentaNeta>\n");
        xml.append("    <TotalImpuesto>").append(factura.getImpuesto()).append("</TotalImpuesto>\n");
        xml.append("    <TotalComprobante>").append(factura.getTotal()).append("</TotalComprobante>\n");
        xml.append("  </ResumenFactura>\n");
    }
    
    private String obtenerActividadEconomica(ComprobanteElectronico comprobante) {
        // TODO: Obtener de configuración de empresa
        return "620100"; // Actividad por defecto
    }
    
    private String obtenerCondicionVenta(ComprobanteElectronico comprobante) {
        // 01=Contado, 02=Crédito, 03=Consignación, 04=Apartado, 05=Arrendamiento, 99=Otros
        return "01"; // Contado por defecto
    }
    
    private String determinarTipoIdentificacion(String identificacion) {
        // 01=Física, 02=Jurídica, 03=DIMEX, 04=NITE
        if (identificacion == null) return "01";
        String numerico = identificacion.replaceAll("[^0-9]", "");
        if (numerico.length() == 10) return "02"; // Jurídica
        if (numerico.length() == 9) return "01"; // Física
        return "01";
    }
    
    private String escaparXml(String texto) {
        if (texto == null) return "";
        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
