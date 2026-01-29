package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;

/**
 * Servicio para generación de XML de comprobantes electrónicos.
 * Implementa la especificación v4.4 de Hacienda de Costa Rica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
public interface XmlGeneratorService {
    
    /**
     * Genera XML de factura electrónica según especificación de Hacienda.
     * 
     * @param comprobante Comprobante a convertir en XML
     * @return XML generado en formato string
     */
    String generarXmlFactura(ComprobanteElectronico comprobante);
    
    /**
     * Genera XML de tiquete electrónico.
     */
    String generarXmlTiquete(ComprobanteElectronico comprobante);
    
    /**
     * Genera XML de nota de crédito.
     */
    String generarXmlNotaCredito(ComprobanteElectronico comprobante);
    
    /**
     * Genera XML de nota de débito.
     */
    String generarXmlNotaDebito(ComprobanteElectronico comprobante);
    
    /**
     * Valida XML contra esquema XSD de Hacienda.
     */
    boolean validarXml(String xml, String tipoComprobante);
}
