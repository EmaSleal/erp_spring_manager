package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ResultadoConsultaManualDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;

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
     * Etapa a de la emisión de una factura (ADR-043 §1a): valida el cliente y genera el comprobante (consecutivo con
     * bloqueo, clave, XML) en una transacción corta. No firma ni envía: eso lo hace
     * {@link EnvioHaciendaService#emitirFactura(Long)}.
     *
     * @param facturaId ID de la factura a emitir
     * @return el comprobante GENERADO
     */
    ComprobanteElectronicoDTO generarParaEmision(Long facturaId);

    /**
     * Etapa a del envío de un comprobante existente (ADR-043 §1a), dentro de la transacción del llamador y sobre la
     * entidad que este cargó con bloqueo: regenera y firma si hace falta, valida contra el XSD y comprueba la
     * configuración. Ante una falla local deja ERROR (ADR-030). No guarda ni transmite.
     *
     * @return id de la configuración de Hacienda con la que se envía, o null si quedó en ERROR
     */
    Long prepararParaEnvio(ComprobanteElectronico comprobante);

    /**
     * Genera un comprobante electrónico desde una factura.
     */
    ComprobanteElectronicoDTO generarDesdeFactura(Long facturaId);

    /**
     * Genera y firma el comprobante de una nota de crédito (tipo 03, ADR-037): consecutivo propio del tipo, clave nueva
     * y XML con la referencia al comprobante ACEPTADO de la factura. Se envía después con
     * {@link EnvioHaciendaService#enviar(Long)}, el mismo flujo de la factura.
     *
     * @return el comprobante vigente de la nota (FIRMADO, o ERROR si la firma falló)
     */
    ComprobanteElectronico generarYFirmarDesdeNota(NotaFactura nota);

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
    
    /** Página de comprobantes con el total que cumple el filtro (T-043, review 1). */
    record PaginaComprobantes(List<ComprobanteElectronicoDTO> contenido, long total) {
    }

    /**
     * T-043 (review 1): lista con filtros aplicados en la base de datos (estado, vigente y ambiente, cada uno opcional);
     * el total y la paginación ya traen el filtro.
     */
    PaginaComprobantes listarFiltrado(Integer empresaId, EstadoComprobante estado, Boolean vigente,
                                      api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda ambiente,
                                      int page, int size);

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
     * Consulta estado en Hacienda y actualiza comprobante según respuesta.
     * 
     * @param comprobanteId ID del comprobante a consultar
     * @return Comprobante actualizado con nuevo estado
     */
    ComprobanteElectronicoDTO consultarYActualizarEstado(Long comprobanteId);

    /**
     * T-045 (ADR-049 §3): cuenta una consulta automática del job que dejó el comprobante en ENVIO_INCIERTO
     * (incrementa consultas_error_hacienda; al llegar al máximo deja de consultarse y se registra). Transacción propia;
     * si el comprobante ya no está en ENVIO_INCIERTO no hace nada.
     */
    void registrarConsultaIncierta(Long comprobanteId);
    
    /**
     * Ya no anula: un comprobante ACEPTADO solo se anula con una nota de crédito total (ADR-037 §7,
     * POST /api/facturas/notas-credito).
     *
     * @throws IllegalStateException siempre
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
