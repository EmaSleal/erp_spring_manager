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
        
        // Condición de venta desde Factura
        var factura = comprobante.getFactura();
        String condicionVenta = (factura != null && factura.getCondicionVentaFE() != null) 
            ? factura.getCondicionVentaFE().getCodigo() 
            : "01"; // Contado por defecto
        xml.append("  <CondicionVenta>").append(condicionVenta).append("</CondicionVenta>\n");
        
        // Plazo de crédito (obligatorio si condición = CREDITO)
        Integer plazoCredito = (factura != null && factura.getPlazoCredito() != null) 
            ? factura.getPlazoCredito() 
            : 0;
        xml.append("  <PlazoCredito>").append(plazoCredito).append("</PlazoCredito>\n");
        
        // Medio de pago desde Factura
        String medioPago = (factura != null && factura.getMedioPagoFE() != null) 
            ? factura.getMedioPagoFE().getCodigo() 
            : "01"; // Efectivo por defecto
        xml.append("  <MedioPago>").append(medioPago).append("</MedioPago>\n");
        
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
        
        // Tipo de identificación desde enum (default 02=Jurídica)
        String tipoId = empresa.getTipoIdentificacion() != null 
            ? empresa.getTipoIdentificacion().getCodigo() 
            : "02";
        xml.append("      <Tipo>").append(tipoId).append("</Tipo>\n");
        xml.append("      <Numero>").append(empresa.getRuc().replaceAll("[^0-9]", "")).append("</Numero>\n");
        xml.append("    </Identificacion>\n");
        
        // Nombre comercial FE (nuevo campo específico para FE)
        String nombreComercial = empresa.getNombreComercialFe() != null 
            ? empresa.getNombreComercialFe() 
            : (empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombreEmpresa());
        xml.append("    <NombreComercial>").append(escaparXml(nombreComercial)).append("</NombreComercial>\n");
        
        // Ubicación completa con códigos oficiales
        xml.append("    <Ubicacion>\n");
        xml.append("      <Provincia>").append(empresa.getProvincia() != null ? empresa.getProvincia().getCodigo() : "1").append("</Provincia>\n");
        xml.append("      <Canton>").append(empresa.getCanton() != null ? empresa.getCanton() : "01").append("</Canton>\n");
        xml.append("      <Distrito>").append(empresa.getDistrito() != null ? empresa.getDistrito() : "01").append("</Distrito>\n");
        
        // Barrio es opcional
        if (empresa.getBarrio() != null && !empresa.getBarrio().isEmpty()) {
            xml.append("      <Barrio>").append(escaparXml(empresa.getBarrio())).append("</Barrio>\n");
        }
        
        // Otras señas (dirección descriptiva)
        String otrasSenas = empresa.getOtrasSenas() != null ? empresa.getOtrasSenas() : empresa.getDireccion();
        xml.append("      <OtrasSenas>").append(escaparXml(otrasSenas)).append("</OtrasSenas>\n");
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
        String numeroIdentificacion = cliente.getNumeroIdentificacion() != null && !cliente.getNumeroIdentificacion().isBlank()
            ? cliente.getNumeroIdentificacion()
            : cliente.getIdentificacion();
        
        xml.append("  <Receptor>\n");
        xml.append("    <Nombre>").append(escaparXml(cliente.getNombre())).append("</Nombre>\n");
        xml.append("    <Identificacion>\n");
        
        // Tipo de identificación desde enum (o determinar automáticamente)
        String tipoId = cliente.getTipoIdentificacion() != null 
            ? cliente.getTipoIdentificacion().getCodigo() 
            : determinarTipoIdentificacion(numeroIdentificacion);
        xml.append("      <Tipo>").append(tipoId).append("</Tipo>\n");
        xml.append("      <Numero>").append(numeroIdentificacion.replaceAll("[^0-9]", "")).append("</Numero>\n");
        xml.append("    </Identificacion>\n");
        
        // Ubicación del cliente (si existe)
        if (cliente.getProvincia() != null || cliente.getOtrasSenas() != null) {
            xml.append("    <Ubicacion>\n");
            if (cliente.getProvincia() != null) {
                xml.append("      <Provincia>").append(cliente.getProvincia().getCodigo()).append("</Provincia>\n");
            }
            if (cliente.getCanton() != null) {
                xml.append("      <Canton>").append(cliente.getCanton()).append("</Canton>\n");
            }
            if (cliente.getDistrito() != null) {
                xml.append("      <Distrito>").append(cliente.getDistrito()).append("</Distrito>\n");
            }
            if (cliente.getOtrasSenas() != null) {
                xml.append("      <OtrasSenas>").append(escaparXml(cliente.getOtrasSenas())).append("</OtrasSenas>\n");
            }
            xml.append("    </Ubicacion>\n");
        }
        
        // Teléfono del cliente
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
            var producto = linea.getProducto();
            
            xml.append("    <LineaDetalle>\n");
            xml.append("      <NumeroLinea>").append(numeroLinea++).append("</NumeroLinea>\n");
            
            // Código CABYS (OBLIGATORIO en v4.4)
            if (producto.getCodigoCabys() != null && !producto.getCodigoCabys().isEmpty()) {
                xml.append("      <CodigoCabys>").append(producto.getCodigoCabys()).append("</CodigoCabys>\n");
            } else {
                log.warn("Producto {} sin código CABYS. Usando código genérico.", producto.getCodigo());
                xml.append("      <CodigoCabys>8522000000000</CodigoCabys>\n"); // Código genérico servicios
            }
            
            xml.append("      <Codigo>").append(producto.getCodigo()).append("</Codigo>\n");
            xml.append("      <Cantidad>").append(linea.getCantidad()).append("</Cantidad>\n");
            
            // Unidad de medida desde presentación
            String unidadMedida = "Unid";
            if (producto.getPresentacion() != null && producto.getPresentacion().getCodigoUnidadFE() != null) {
                unidadMedida = producto.getPresentacion().getCodigoUnidadFE();
            }
            xml.append("      <UnidadMedida>").append(unidadMedida).append("</UnidadMedida>\n");
            
            // Descripción del producto
            xml.append("      <Detalle>").append(escaparXml(producto.getDescripcion())).append("</Detalle>\n");
            
            xml.append("      <PrecioUnitario>").append(linea.getPrecioUnitario()).append("</PrecioUnitario>\n");
            xml.append("      <MontoTotal>").append(linea.getSubtotal()).append("</MontoTotal>\n");
            xml.append("      <SubTotal>").append(linea.getSubtotal()).append("</SubTotal>\n");
            
            // Impuestos según configuración del producto
            boolean esGravado = producto.getGravado() != null && producto.getGravado();
            
            if (esGravado) {
                // Calcular impuesto según tarifa del producto
                BigDecimal porcentajeImpuesto = producto.getPorcentajeImpuesto() != null 
                    ? producto.getPorcentajeImpuesto() 
                    : new BigDecimal("13.00");
                BigDecimal impuesto = linea.getSubtotal().multiply(porcentajeImpuesto.divide(new BigDecimal("100")));
                
                if (impuesto.compareTo(BigDecimal.ZERO) > 0) {
                    xml.append("      <Impuesto>\n");
                    xml.append("        <Codigo>01</Codigo>\n"); // IVA
                    
                    // Código de tarifa: 08 = 13%, 07 = 4%, 06 = 2%, 05 = 1%
                    String codigoTarifa = "08"; // 13% por defecto
                    if (porcentajeImpuesto.compareTo(new BigDecimal("4")) == 0) {
                        codigoTarifa = "07";
                    } else if (porcentajeImpuesto.compareTo(new BigDecimal("2")) == 0) {
                        codigoTarifa = "06";
                    } else if (porcentajeImpuesto.compareTo(new BigDecimal("1")) == 0) {
                        codigoTarifa = "05";
                    }
                    
                    xml.append("        <CodigoTarifa>").append(codigoTarifa).append("</CodigoTarifa>\n");
                    xml.append("        <Tarifa>").append(porcentajeImpuesto).append("</Tarifa>\n");
                    xml.append("        <Monto>").append(impuesto).append("</Monto>\n");
                    xml.append("      </Impuesto>\n");
                }
                
                BigDecimal totalLinea = linea.getSubtotal().add(impuesto);
                xml.append("      <MontoTotalLinea>").append(totalLinea).append("</MontoTotalLinea>\n");
            } else {
                // Producto exento
                xml.append("      <MontoTotalLinea>").append(linea.getSubtotal()).append("</MontoTotalLinea>\n");
            }
            
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
        
        // Moneda desde Factura
        String codigoMoneda = (factura.getMonedaFE() != null) 
            ? factura.getMonedaFE().getCodigo() 
            : "CRC";
        xml.append("      <CodigoMoneda>").append(codigoMoneda).append("</CodigoMoneda>\n");
        
        // Tipo de cambio (obligatorio si moneda != CRC)
        BigDecimal tipoCambio = (factura.getTipoCambio() != null) 
            ? factura.getTipoCambio() 
            : new BigDecimal("1.00000");
        xml.append("      <TipoCambio>").append(String.format("%.5f", tipoCambio)).append("</TipoCambio>\n");
        xml.append("    </CodigoTipoMoneda>\n");
        
        // Totales por tipo de producto (servicios vs mercancías)
        // Por ahora asumimos todo como servicios gravados
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
        var empresa = comprobante.getEmpresa();
        if (empresa != null && empresa.getCodigoActividad() != null) {
            return empresa.getCodigoActividad();
        }
        log.warn("Empresa sin código de actividad económica configurado. Usando código genérico.");
        return "620100"; // Programación informática (código genérico)
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
