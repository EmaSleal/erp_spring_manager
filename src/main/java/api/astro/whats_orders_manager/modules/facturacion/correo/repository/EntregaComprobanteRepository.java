package api.astro.whats_orders_manager.modules.facturacion.correo.repository;

import api.astro.whats_orders_manager.modules.facturacion.correo.model.EntregaComprobante;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.EstadoEntrega;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Las horas se reciben como parámetros (calculadas con el Clock): no se usa el reloj de la base (ArquitecturaRelojBase). */
public interface EntregaComprobanteRepository extends JpaRepository<EntregaComprobante, Long> {

    boolean existsByClaveAutomatica(Long claveAutomatica);

    @Query("SELECT e FROM EntregaComprobante e WHERE e.comprobante.id = :idComprobante ORDER BY e.id DESC")
    List<EntregaComprobante> findPorComprobante(@Param("idComprobante") Long idComprobante);

    /** Ids de las PENDIENTE vencidas, las más antiguas primero. */
    @Query("SELECT e.id FROM EntregaComprobante e WHERE e.estado = :estado AND e.proximoIntento <= :ahora "
        + "ORDER BY e.proximoIntento ASC, e.id ASC")
    List<Long> idsVencidas(@Param("estado") EstadoEntrega estado, @Param("ahora") LocalDateTime ahora, Pageable lote);

    /**
     * Reclamo sin duplicar: solo una corrida logra mover el próximo intento de una PENDIENTE vencida (1 fila = reclamada).
     */
    @Modifying
    @Query("UPDATE EntregaComprobante e SET e.proximoIntento = :hasta WHERE e.id = :id AND e.estado = :estado "
        + "AND e.proximoIntento <= :ahora")
    int reclamar(@Param("id") Long id, @Param("estado") EstadoEntrega estado, @Param("ahora") LocalDateTime ahora,
                 @Param("hasta") LocalDateTime hasta);

    /** Difiere las PENDIENTE vencidas (tope diario o 429) sin consumir intentos. */
    @Modifying
    @Query("UPDATE EntregaComprobante e SET e.proximoIntento = :hasta WHERE e.estado = :estado AND e.proximoIntento <= :ahora")
    int diferirVencidas(@Param("estado") EstadoEntrega estado, @Param("ahora") LocalDateTime ahora,
                        @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(e) FROM EntregaComprobante e WHERE e.estado = :estado AND e.fechaEnvio >= :desde AND e.fechaEnvio < :hasta")
    long contarEnviadasEntre(@Param("estado") EstadoEntrega estado, @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta);

    /** Contador del panel; {@code ambiente} null = todos los ambientes. */
    @Query("SELECT COUNT(e) FROM EntregaComprobante e WHERE e.estado = :estado "
        + "AND (:ambiente IS NULL OR e.comprobante.ambiente = :ambiente)")
    long contarPorEstado(@Param("estado") EstadoEntrega estado, @Param("ambiente") AmbienteHacienda ambiente);

    @Query("SELECT COUNT(e) FROM EntregaComprobante e WHERE e.estado = :estado AND e.proximoIntento <= :ahora "
        + "AND (:ambiente IS NULL OR e.comprobante.ambiente = :ambiente)")
    long contarVencidas(@Param("estado") EstadoEntrega estado, @Param("ahora") LocalDateTime ahora,
                        @Param("ambiente") AmbienteHacienda ambiente);

    /** Para la página de entregas: FALLIDO o PENDIENTE vencida; el comprobante y su documento vienen en el mismo SELECT. */
    @Query("SELECT e FROM EntregaComprobante e JOIN FETCH e.comprobante c WHERE e.estado = :estado "
        + "AND (:soloVencidas = false OR e.proximoIntento <= :ahora) "
        + "AND (:ambiente IS NULL OR c.ambiente = :ambiente) ORDER BY e.fechaCreacion DESC, e.id DESC")
    List<EntregaComprobante> listar(@Param("estado") EstadoEntrega estado, @Param("soloVencidas") boolean soloVencidas,
                                    @Param("ahora") LocalDateTime ahora, @Param("ambiente") AmbienteHacienda ambiente,
                                    Pageable limite);
}
