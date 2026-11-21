package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.model.SaldoFavorCliente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaldoFavorClienteRepository extends JpaRepository<SaldoFavorCliente, Long> {

    /** Saldo con bloqueo de escritura (SELECT ... FOR UPDATE): serializa las aplicaciones y reversiones de un saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SaldoFavorCliente s WHERE s.id = :id")
    Optional<SaldoFavorCliente> findByIdParaActualizar(@Param("id") Long id);

    @Query("SELECT s FROM SaldoFavorCliente s WHERE s.cliente.idCliente = :idCliente AND s.moneda = :moneda "
        + "AND s.estado = :estado ORDER BY s.fechaOrigen, s.id")
    List<SaldoFavorCliente> findPorClienteMonedaYEstado(@Param("idCliente") Integer idCliente,
                                                        @Param("moneda") String moneda,
                                                        @Param("estado") EstadoSaldoFavor estado);

    @Query("SELECT s FROM SaldoFavorCliente s WHERE s.cliente.idCliente = :idCliente ORDER BY s.moneda, s.fechaOrigen, s.id")
    List<SaldoFavorCliente> findTodosDelCliente(@Param("idCliente") Integer idCliente);

    /** Saldos con saldo vivo (VIGENTES) originados hasta la fecha de corte, para el reporte de antigüedad (T-049). */
    @Query("SELECT s FROM SaldoFavorCliente s JOIN FETCH s.cliente c WHERE s.estado = :estado AND s.saldoVivo > 0 "
        + "AND s.fechaOrigen <= :corte ORDER BY s.moneda, c.nombre, s.fechaOrigen, s.id")
    List<SaldoFavorCliente> findVivosHasta(@Param("estado") EstadoSaldoFavor estado, @Param("corte") java.time.LocalDate corte);

    Optional<SaldoFavorCliente> findByNota_Id(Long idNota);
}
