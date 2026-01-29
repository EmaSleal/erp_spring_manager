package api.astro.whats_orders_manager.modules.facturacion.electronica.repository;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    List<ComprobanteElectronico> findByEmpresaIdEmpresaAndTipoComprobante(
        Integer empresaId,
        TipoComprobanteElectronico tipo
    );
    
    /**
     * Verifica si existe comprobante con clave numérica.
     */
    boolean existsByClaveNumerica(String claveNumerica);
    
    /**
     * Encuentra comprobantes pendientes de reintento.
     * Busca comprobantes con estado ERROR o FIRMADO que no hayan alcanzado el límite de reintentos.
     */
    @Query("SELECT c FROM ComprobanteElectronico c WHERE " +
           "(c.estado = 'ERROR' OR c.estado = 'FIRMADO' OR c.estado = 'GENERADO') " +
           "AND c.intentosEnvio < 5 " +
           "ORDER BY c.createdAt ASC")
    List<ComprobanteElectronico> findPendientesReintento();
    
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
