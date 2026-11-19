package api.astro.whats_orders_manager.modules.facturacion.electronica.repository;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para comprobantes electrónicos usando Stored Procedures.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Repository
public interface ComprobanteElectronicoRepository extends JpaRepository<ComprobanteElectronico, Long> {
    
    /**
     * Encuentra comprobante por clave numérica.
     */
    @Procedure(name = "sp_obtener_comprobante_por_clave")
    Optional<ComprobanteElectronico> findByClaveNumerica(@Param("p_clave_numerica") String claveNumerica);
    
    /**
     * Encuentra comprobante por factura.
     */
    @Procedure(name = "sp_obtener_comprobante_por_factura")
    Optional<ComprobanteElectronico> findByFacturaId(@Param("p_factura_id") Long facturaId);
    
    /**
     * Lista comprobantes de una empresa con paginación usando SP.
     */
    @Query(value = "CALL sp_listar_comprobantes_por_empresa(:empresaId, :page, :size)", nativeQuery = true)
    List<Object[]> findAllByEmpresaIdAsDTO(
        @Param("empresaId") Integer empresaId,
        @Param("page") int page,
        @Param("size") int size
    );
    
    /**
     * Lista comprobantes por estado usando SP.
     */
    @Query(value = "CALL sp_listar_comprobantes_por_estado(:empresaId, :estado, :page, :size)", nativeQuery = true)
    List<Object[]> findByEmpresaIdAndEstadoAsDTO(
        @Param("empresaId") Integer empresaId,
        @Param("estado") String estado,
        @Param("page") int page,
        @Param("size") int size
    );
    
    /**
     * Encuentra comprobantes pendientes de envío usando SP.
     */
    @Query(value = "CALL sp_listar_comprobantes_pendientes()", nativeQuery = true)
    List<Object[]> findPendientesEnvio();
    
    /**
     * Encuentra comprobantes por rango de fechas usando SP.
     */
    @Query(value = "CALL sp_listar_comprobantes_por_fechas(:empresaId, :fechaInicio, :fechaFin, :page, :size)", nativeQuery = true)
    List<Object[]> findByEmpresaIdAndFechaEmisionBetweenAsDTO(
        @Param("empresaId") Integer empresaId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        @Param("page") int page,
        @Param("size") int size
    );
    
    /**
     * Cuenta comprobantes por estado usando SP.
     */
    @Query(value = "CALL sp_contar_comprobantes_por_estado(:empresaId, :estado)", nativeQuery = true)
    Long countByEmpresaIdAndEstado(
        @Param("empresaId") Integer empresaId, 
        @Param("estado") String estado
    );
    
    /**
     * Busca comprobantes por tipo (sin paginación - método auxiliar).
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE c.empresa.idConfiguracion = :empresaId AND c.tipoComprobante = :tipo")
    List<ComprobanteElectronico> findByEmpresaIdConfiguracionAndTipoComprobante(
        @Param("empresaId") Integer empresaId,
        @Param("tipo") TipoComprobanteElectronico tipo
    );
    
    /**
     * Verifica si existe comprobante con clave numérica.
     */
    boolean existsByClaveNumerica(String claveNumerica);
    
    /**
     * Comprobante con bloqueo de fila (SELECT ... FOR UPDATE) para la etapa de preparación del envío (T-037, ADR-043
     * §1a): dos envíos del mismo comprobante preparan uno detrás del otro, y la preparación del segundo ve el estado que
     * dejó el primero.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ComprobanteElectronico c WHERE c.id = :id")
    Optional<ComprobanteElectronico> findByIdParaEnvio(@Param("id") Long id);

    /**
     * Marca previa al POST (T-037, ADR-043 §1b y §4): pasa el comprobante vigente de un estado transmisible a
     * ENVIO_INCIERTO, con la fecha del envío y un intento más, en un único UPDATE condicional. Si devuelve 0, otro
     * proceso ya lo está enviando (o Hacienda pudo recibirlo) y no se transmite.
     *
     * @return filas actualizadas: 1 si este proceso tomó el envío, 0 si no
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ComprobanteElectronico c SET c.estado = :marca, c.fechaEnvio = :ahora, "
        + "c.intentosEnvio = COALESCE(c.intentosEnvio, 0) + 1 "
        + "WHERE c.id = :id AND c.vigente = true AND c.estado IN :transmisibles")
    int marcarEnvioEnCurso(@Param("id") Long id,
                           @Param("ahora") LocalDateTime ahora,
                           @Param("marca") EstadoComprobante marca,
                           @Param("transmisibles") java.util.Collection<EstadoComprobante> transmisibles);

    /** T-044: ambiente de cada comprobante de una página de listado (el SP del listado no lo trae). */
    @Query("SELECT c.id, c.ambiente FROM ComprobanteElectronico c WHERE c.id IN :ids")
    List<Object[]> findAmbientesByIds(@Param("ids") java.util.Collection<Long> ids);

    /**
     * Encuentra comprobantes vigentes pendientes de reintento: los que Hacienda no tiene (GENERADO, FIRMADO, ERROR
     * o NO_TRANSMITIDO, ADR-030) y no alcanzaron el límite de intentos. Nunca ENVIO_INCIERTO, ENVIADO, PROCESANDO
     * ni ACEPTADO: reenviarlos podría duplicar un documento que Hacienda ya tiene.
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE " +
           "c.estado IN ('GENERADO', 'FIRMADO', 'ERROR', 'NO_TRANSMITIDO') " +
           "AND c.vigente = true " +
           "AND c.intentosEnvio < 5 " +
           "AND c.ambiente = :ambiente " +
           "ORDER BY c.createdAt ASC")
    List<ComprobanteElectronico> findPendientesReintento(@Param("ambiente") AmbienteHacienda ambiente);

    /**
     * Los mismos pendientes sin filtrar por ambiente: solo para cuando la configuración activa no se puede leer y el
     * job conserva su comportamiento de T-041 (cada reenvío termina en ERROR de configuración).
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE " +
           "c.estado IN ('GENERADO', 'FIRMADO', 'ERROR', 'NO_TRANSMITIDO') " +
           "AND c.vigente = true " +
           "AND c.intentosEnvio < 5 " +
           "ORDER BY c.createdAt ASC")
    List<ComprobanteElectronico> findPendientesReintentoSinFiltroDeAmbiente();

    /**
     * T-044: los que {@link #findPendientesReintento} deja fuera por ambiente (misma condición, ambiente distinto o
     * nulo), para el log del job. No se cargan ni se les cuentan intentos.
     */
    @Query("SELECT COUNT(c) FROM ComprobanteElectronico c WHERE " +
           "c.estado IN ('GENERADO', 'FIRMADO', 'ERROR', 'NO_TRANSMITIDO') " +
           "AND c.vigente = true " +
           "AND c.intentosEnvio < 5 " +
           "AND (c.ambiente IS NULL OR c.ambiente <> :ambiente)")
    long countPendientesReintentoDeOtroAmbiente(@Param("ambiente") AmbienteHacienda ambiente);

    /**
     * Comprobante vigente de una factura (ADR-030 §4: N comprobantes, uno vigente).
     */
    Optional<ComprobanteElectronico> findFirstByFactura_IdFacturaAndVigenteTrue(Integer idFactura);

    /** Comprobante vigente de una nota de crédito o débito (ADR-037 §2). */
    Optional<ComprobanteElectronico> findFirstByNotaFactura_IdAndVigenteTrue(Long idNota);

    /** Comprobantes vigentes de varias notas, en una sola consulta (listado de notas, T-042). */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE c.notaFactura.id IN :ids AND c.vigente = true")
    List<ComprobanteElectronico> findVigentesByNotaIds(@Param("ids") java.util.Collection<Long> ids);

    /** Todos los comprobantes de una nota, vigente e historial, en orden de creación (T-042). */
    List<ComprobanteElectronico> findByNotaFactura_IdOrderByIdAsc(Long idNota);

    /**
     * Todos los comprobantes de una factura, vigente e historial, en orden de creación.
     */
    List<ComprobanteElectronico> findByFactura_IdFacturaOrderByIdAsc(Integer idFactura);

    /**
     * Comprobantes en alguno de los estados indicados enviados antes de la fecha límite.
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE " +
           "c.estado IN :estados AND c.fechaEnvio < :fechaLimite")
    List<ComprobanteElectronico> findByEstadoInAndFechaEnvioLessThan(
        @Param("estados") List<EstadoComprobante> estados,
        @Param("fechaLimite") LocalDateTime fechaLimite
    );
    
    /**
     * Comprobantes vigentes que el sondeo debe consultar: ENVIADO o PROCESANDO, y ERROR_HACIENDA mientras no llegue
     * al máximo de consultas seguidas con ind-estado "error"; enviados antes de la fecha límite.
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE c.vigente = true AND c.fechaEnvio < :fechaLimite AND (" +
           "c.estado IN (api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ENVIADO, " +
           "api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.PROCESANDO) " +
           "OR (c.estado = api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ERROR_HACIENDA " +
           "AND c.consultasErrorHacienda < :maxConsultasError)) " +
           "AND c.ambiente = :ambiente")
    List<ComprobanteElectronico> findParaSondeo(
        @Param("fechaLimite") LocalDateTime fechaLimite,
        @Param("maxConsultasError") int maxConsultasError,
        @Param("ambiente") AmbienteHacienda ambiente
    );

    /** El sondeo sin filtrar por ambiente: solo con la configuración activa ilegible (comportamiento de T-041). */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE c.vigente = true AND c.fechaEnvio < :fechaLimite AND (" +
           "c.estado IN (api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ENVIADO, " +
           "api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.PROCESANDO) " +
           "OR (c.estado = api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ERROR_HACIENDA " +
           "AND c.consultasErrorHacienda < :maxConsultasError))")
    List<ComprobanteElectronico> findParaSondeoSinFiltroDeAmbiente(
        @Param("fechaLimite") LocalDateTime fechaLimite,
        @Param("maxConsultasError") int maxConsultasError
    );

    /** T-044: los que {@link #findParaSondeo} deja fuera por ambiente (misma condición, ambiente distinto o nulo). */
    @Query("SELECT COUNT(c) FROM ComprobanteElectronico c WHERE c.vigente = true AND c.fechaEnvio < :fechaLimite AND (" +
           "c.estado IN (api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ENVIADO, " +
           "api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.PROCESANDO) " +
           "OR (c.estado = api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ERROR_HACIENDA " +
           "AND c.consultasErrorHacienda < :maxConsultasError)) " +
           "AND (c.ambiente IS NULL OR c.ambiente <> :ambiente)")
    long countParaSondeoDeOtroAmbiente(
        @Param("fechaLimite") LocalDateTime fechaLimite,
        @Param("maxConsultasError") int maxConsultasError,
        @Param("ambiente") AmbienteHacienda ambiente
    );

    /**
     * T-045 (ADR-049): los vigentes en ENVIO_INCIERTO que el job horario también consulta: último envío anterior a la
     * fecha límite (ahora menos MINUTOS_ANTES_DE_VERIFICAR) y consultas automáticas sin resolver por debajo del máximo.
     * Hermana de {@link #findParaSondeo}, que conserva su ventana de 5 minutos para los demás estados.
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE c.vigente = true AND c.estado = api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ENVIO_INCIERTO AND c.fechaEnvio < :fechaLimite AND c.consultasErrorHacienda < :maxConsultasError AND c.ambiente = :ambiente")
    List<ComprobanteElectronico> findInciertosParaSondeo(
        @Param("fechaLimite") LocalDateTime fechaLimite,
        @Param("maxConsultasError") int maxConsultasError,
        @Param("ambiente") AmbienteHacienda ambiente
    );

    /** T-045: ENVIO_INCIERTO sin filtrar por ambiente, solo con la configuración activa ilegible (como T-041). */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE c.vigente = true AND c.estado = api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ENVIO_INCIERTO AND c.fechaEnvio < :fechaLimite AND c.consultasErrorHacienda < :maxConsultasError")
    List<ComprobanteElectronico> findInciertosParaSondeoSinFiltroDeAmbiente(
        @Param("fechaLimite") LocalDateTime fechaLimite,
        @Param("maxConsultasError") int maxConsultasError
    );

    /** T-045: los que {@link #findInciertosParaSondeo} deja fuera por ambiente (ambiente distinto o nulo). */
    @Query("SELECT COUNT(c) FROM ComprobanteElectronico c WHERE c.vigente = true AND c.estado = api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante.ENVIO_INCIERTO AND c.fechaEnvio < :fechaLimite AND c.consultasErrorHacienda < :maxConsultasError AND (c.ambiente IS NULL OR c.ambiente <> :ambiente)")
    long countInciertosParaSondeoDeOtroAmbiente(
        @Param("fechaLimite") LocalDateTime fechaLimite,
        @Param("maxConsultasError") int maxConsultasError,
        @Param("ambiente") AmbienteHacienda ambiente
    );

    /**
     * Encuentra comprobantes enviados sin respuesta después de cierto tiempo.
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE " +
           "c.estado = :estado AND c.fechaEnvio < :fechaLimite")
    List<ComprobanteElectronico> findByEstadoAndFechaEnvioLessThan(
        @Param("estado") EstadoComprobante estado,
        @Param("fechaLimite") LocalDateTime fechaLimite
    );
    
    /**
     * Encuentra comprobantes antiguos por estado.
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE " +
           "c.estado = :estado AND c.createdAt < :fechaLimite")
    List<ComprobanteElectronico> findByEstadoAndCreatedAtLessThan(
        @Param("estado") EstadoComprobante estado,
        @Param("fechaLimite") LocalDateTime fechaLimite
    );
}
