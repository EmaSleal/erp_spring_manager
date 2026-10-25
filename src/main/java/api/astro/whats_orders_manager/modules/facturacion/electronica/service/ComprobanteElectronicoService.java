package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ResultadoConsultaManualDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestión de comprobantes electrónicos.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
public interface ComprobanteElectronicoService {
    
    /**
     * Procesa una factura completa: genera, firma y envía a Hacienda.
     * Proceso completo integrado para facturación electrónica.
     * 
     * @param facturaId ID de la factura a procesar
     * @return Comprobante procesado
     */
    ComprobanteElectronicoDTO procesarFactura(Long facturaId);
    
    /**
     * Procesa una factura de forma asíncrona.
     * Envía a Hacienda en segundo plano y consulta estado después de 30s.
     * 
     * @param comprobanteId ID del comprobante a procesar
     */
    void procesarFacturaAsync(Long comprobanteId);
    
    /**
     * Genera un comprobante electrónico desde una factura.
     */
    ComprobanteElectronicoDTO generarDesdeFactura(Long facturaId);

    /**
     * Reemite una factura cuyo comprobante vigente fue RECHAZADO: el rechazado queda como historial inmutable y se
     * genera un comprobante nuevo, vigente, con consecutivo y clave nuevos (ADR-030).
     *
     * @param id id del comprobante RECHAZADO vigente
     */
    ComprobanteElectronicoDTO reemitir(Long id);

    /**
     * "Consultar estado en Hacienda" (ADR-031 §4) para un comprobante ENVIADO, PROCESANDO, ENVIO_INCIERTO o
     * ERROR_HACIENDA, 1 vez cada 30 s: 404 según el estado de origen; otra respuesta → el estado informado.
     */
    ResultadoConsultaManualDTO consultarEstadoManual(Long id);
    
    /**
     * Obtiene comprobante por ID.
     */
    Optional<ComprobanteElectronicoDTO> obtenerPorId(Long id);
    
    /**
     * Obtiene comprobante por clave numérica.
     */
    Optional<ComprobanteElectronicoDTO> obtenerPorClaveNumerica(String claveNumerica);
    
    /**
     * Obtiene comprobante de una factura.
     */
    Optional<ComprobanteElectronicoDTO> obtenerPorFactura(Long facturaId);
    
    /**
     * Lista comprobantes de una empresa con paginación.
     */
    List<ComprobanteElectronicoDTO> listarPorEmpresa(Integer empresaId, int page, int size);
    
    /**
     * Lista comprobantes por estado.
     */
    List<ComprobanteElectronicoDTO> listarPorEstado(Integer empresaId, EstadoComprobante estado, int page, int size);
    
    /**
     * Lista comprobantes por rango de fechas.
     */
    List<ComprobanteElectronicoDTO> listarPorRangoFechas(
        Integer empresaId, 
        LocalDateTime fechaInicio, 
        LocalDateTime fechaFin, 
        int page,
        int size
    );
    
    /**
     * Busca comprobantes pendientes de envío.
     */
    List<ComprobanteElectronico> obtenerPendientesEnvio();
    
    /**
     * Firma digitalmente un comprobante.
     */
    ComprobanteElectronicoDTO firmar(Long id);
    
    /**
     * Envía comprobante a Hacienda.
     */
    ComprobanteElectronicoDTO enviarAHacienda(Long id);
    
    /**
     * Reenvía un comprobante que falló.
     */
    ComprobanteElectronicoDTO reenviar(Long id);
    
    /**
     * Consulta estado de un comprobante en Hacienda.
     */
    ComprobanteElectronicoDTO consultarEstado(Long id);
    
    /**
     * Consulta estado en Hacienda y actualiza comprobante según respuesta.
     * 
     * @param comprobanteId ID del comprobante a consultar
     * @return Comprobante actualizado con nuevo estado
     */
    ComprobanteElectronicoDTO consultarYActualizarEstado(Long comprobanteId);
    
    /**
     * Envía comprobante por correo al cliente.
     */
    boolean enviarPorEmail(Long id, String emailAdicional);
    
    /**
     * Anula un comprobante.
     */
    ComprobanteElectronicoDTO anular(Long id, String motivo);
    
    /**
     * Descarga el XML del comprobante.
     * 
     * @param id ID del comprobante
     * @return Contenido XML como String
     */
    String descargarXml(Long id);
    
    /**
     * Guarda el XML del comprobante en el filesystem.
     * 
     * @param id ID del comprobante
     * @param xml Contenido XML a guardar
     * @return Ruta del archivo guardado
     */
    String guardarXmlEnFilesystem(Long id, String xml);
    
    /**
     * Cuenta comprobantes por estado.
     */
    long contarPorEstado(Integer empresaId, EstadoComprobante estado);
}
