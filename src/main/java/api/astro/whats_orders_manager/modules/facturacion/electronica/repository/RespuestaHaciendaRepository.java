package api.astro.whats_orders_manager.modules.facturacion.electronica.repository;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.RespuestaHacienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para respuestas de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Repository
public interface RespuestaHaciendaRepository extends JpaRepository<RespuestaHacienda, Long> {
    
    /**
     * Encuentra todas las respuestas de un comprobante con DTO.
     */
    @Query("""
        SELECT new api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO(
            r.id, r.comprobante.id, r.comprobante.claveNumerica, r.fechaRespuesta,
            r.codigoMensaje, r.mensaje, r.xmlRespuesta,
            CASE WHEN r.exitoso = true THEN 'EXITOSO' ELSE 'FALLIDO' END,
            r.exitoso, r.detalles,
            r.codigoHttp, r.tiempoRespuestaMs, false, r.createdAt
        )
        FROM RespuestaHacienda r
        WHERE r.comprobante.id = :comprobanteId
        ORDER BY r.fechaRespuesta DESC
    """)
    List<RespuestaHaciendaDTO> findByComprobanteIdAsDTO(@Param("comprobanteId") Long comprobanteId);
    
    /**
     * Encuentra la última respuesta de un comprobante.
     */
    @Query("""
        SELECT r FROM RespuestaHacienda r
        WHERE r.comprobante.id = :comprobanteId
        ORDER BY r.fechaRespuesta DESC
        LIMIT 1
    """)
    RespuestaHacienda findUltimaByComprobanteId(@Param("comprobanteId") Long comprobanteId);
    
    /**
     * Encuentra respuestas exitosas de un comprobante.
     */
    @Query("""
        SELECT r FROM RespuestaHacienda r
        WHERE r.comprobante.id = :comprobanteId AND r.exitoso = true
        ORDER BY r.fechaRespuesta DESC
    """)
    List<RespuestaHacienda> findExitosasByComprobanteId(@Param("comprobanteId") Long comprobanteId);
    
    /**
     * Encuentra respuestas con errores de un comprobante.
     */
    @Query("""
        SELECT r FROM RespuestaHacienda r
        WHERE r.comprobante.id = :comprobanteId AND r.exitoso = false
        ORDER BY r.fechaRespuesta DESC
    """)
    List<RespuestaHacienda> findErroresByComprobanteId(@Param("comprobanteId") Long comprobanteId);
    
    /**
     * Cuenta respuestas de un comprobante.
     */
    @Query("SELECT COUNT(r) FROM RespuestaHacienda r WHERE r.comprobante.id = :comprobanteId")
    long countByComprobanteId(@Param("comprobanteId") Long comprobanteId);
    
    /**
     * Calcula tiempo promedio de respuesta para una empresa.
     */
    @Query("""
        SELECT AVG(r.tiempoRespuestaMs)
        FROM RespuestaHacienda r
        WHERE r.comprobante.empresa.id = :empresaId
        AND r.tiempoRespuestaMs IS NOT NULL
    """)
    Double calcularTiempoPromedioRespuesta(@Param("empresaId") Long empresaId);
}
