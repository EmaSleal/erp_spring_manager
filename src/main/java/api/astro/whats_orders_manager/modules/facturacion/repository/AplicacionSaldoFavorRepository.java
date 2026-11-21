package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.model.AplicacionSaldoFavor;
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
public interface AplicacionSaldoFavorRepository extends JpaRepository<AplicacionSaldoFavor, Long> {

    /** Aplicación con bloqueo de escritura: dos reversiones simultáneas se serializan y la segunda ve REVERSADA. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AplicacionSaldoFavor a WHERE a.id = :id")
    Optional<AplicacionSaldoFavor> findByIdParaActualizar(@Param("id") Long id);

    @Query("SELECT a FROM AplicacionSaldoFavor a WHERE a.saldo.id IN :idsSaldo ORDER BY a.fechaAplicacion, a.id")
    List<AplicacionSaldoFavor> findPorSaldos(@Param("idsSaldo") Collection<Long> idsSaldo);

    @Query("SELECT a FROM AplicacionSaldoFavor a WHERE a.pago.idPago IN :idsPago")
    List<AplicacionSaldoFavor> findPorPagos(@Param("idsPago") Collection<Long> idsPago);

    @Query("SELECT a FROM AplicacionSaldoFavor a WHERE a.factura.idFactura = :idFactura ORDER BY a.fechaAplicacion, a.id")
    List<AplicacionSaldoFavor> findPorFacturaDestino(@Param("idFactura") Integer idFactura);
}
