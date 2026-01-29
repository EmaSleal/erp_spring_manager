package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

/**
 * Servicio para firma digital de comprobantes electrónicos.
 * Implementa firma XAdES-EPES según requerimientos de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
public interface FirmaDigitalService {
    
    /**
     * Firma digitalmente un XML con certificado .p12.
     * 
     * @param xml XML a firmar
     * @param rutaCertificado Ruta al archivo .p12
     * @param pinCertificado PIN del certificado
     * @return XML firmado con firma XAdES-EPES
     */
    String firmarXml(String xml, String rutaCertificado, String pinCertificado);
    
    /**
     * Verifica si un XML está firmado correctamente.
     */
    boolean verificarFirma(String xmlFirmado);
    
    /**
     * Extrae información del certificado.
     */
    CertificadoInfo obtenerInfoCertificado(String rutaCertificado, String pin);
    
    /**
     * Información del certificado digital.
     */
    record CertificadoInfo(
        String titular,
        String cedula,
        String emisor,
        String fechaExpiracion,
        boolean vigente
    ) {}
}
