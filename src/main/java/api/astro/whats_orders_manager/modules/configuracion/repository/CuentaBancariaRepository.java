package api.astro.whats_orders_manager.modules.configuracion.repository;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Integer> {

    List<CuentaBancaria> findByEmpresaIdEmpresaAndActivaTrue(Integer empresaId);

    List<CuentaBancaria> findByEmpresaIdEmpresaOrderByOrdenAsc(Integer empresaId);
}
