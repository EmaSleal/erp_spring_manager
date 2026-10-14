package api.astro.whats_orders_manager.modules.configuracion.repository;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Integer> {

    List<CuentaBancaria> findByEmpresaIdEmpresaAndActivaTrue(Integer empresaId);

    List<CuentaBancaria> findByEmpresaIdEmpresaOrderByOrdenAsc(Integer empresaId);

    /**
     * Cuentas bancarias activas de la empresa en una moneda, para los selectores de cobros y pagos
     * a proveedor (ADR-018).
     */
    List<CuentaBancaria> findByEmpresaIdEmpresaAndActivaTrueAndMonedaOrderByOrdenAscIdCuentaBancariaAsc(
            Integer empresaId, String moneda);

    /** True si algún cobro (pagos.cuenta_bancaria_id) usa la cuenta bancaria. */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Pago p " +
           "WHERE p.cuentaBancariaDestino.idCuentaBancaria = :id")
    boolean estaReferenciadaPorPagos(@Param("id") Integer idCuentaBancaria);

    /** True si algún pago a proveedor (pagos_proveedor.cuenta_bancaria_id) usa la cuenta bancaria. */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PagoProveedor p " +
           "WHERE p.cuentaBancariaOrigen.idCuentaBancaria = :id")
    boolean estaReferenciadaPorPagosProveedor(@Param("id") Integer idCuentaBancaria);
}
