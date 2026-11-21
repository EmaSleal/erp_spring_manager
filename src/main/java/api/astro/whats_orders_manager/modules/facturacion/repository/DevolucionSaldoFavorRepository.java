package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.model.DevolucionSaldoFavor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DevolucionSaldoFavorRepository extends JpaRepository<DevolucionSaldoFavor, Long> {

    /** Devolución con bloqueo de escritura (FOR UPDATE). Orden de bloqueo: siempre el saldo y luego la devolución. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DevolucionSaldoFavor d WHERE d.id = :id")
    Optional<DevolucionSaldoFavor> findByIdParaActualizar(@Param("id") Long id);

    /** Id del saldo de la devolución sin cargar entidades (para bloquear el saldo antes de leer la devolución). */
    @Query("SELECT d.saldo.id FROM DevolucionSaldoFavor d WHERE d.id = :id")
    Optional<Long> findIdSaldo(@Param("id") Long id);

    @Query("SELECT d FROM DevolucionSaldoFavor d WHERE d.saldo.id = :idSaldo ORDER BY d.id")
    List<DevolucionSaldoFavor> findPorSaldo(@Param("idSaldo") Long idSaldo);

    @Query("SELECT d FROM DevolucionSaldoFavor d WHERE d.saldo.id IN :ids ORDER BY d.id")
    List<DevolucionSaldoFavor> findPorSaldos(@Param("ids") Collection<Long> ids);
}
