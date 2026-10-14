package api.astro.whats_orders_manager.modules.contabilidad.repository;

import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ParametroContable — account-parameter configuration lookup.
 */
@Repository
public interface ParametroContableRepository extends JpaRepository<ParametroContable, Long> {

    /**
     * Finds a parameter entry by its event key.
     *
     * @param clave the accounting event key (e.g. "CPP_CUENTA_COMPRAS")
     * @return the parameter if configured
     */
    Optional<ParametroContable> findByClave(String clave);

    /**
     * Returns all parameters with their cuenta eagerly loaded, avoiding N+1 on the LAZY association.
     */
    @Query("SELECT p FROM ParametroContable p JOIN FETCH p.cuentaContable ORDER BY p.clave")
    List<ParametroContable> findAllWithCuentas();

    /**
     * Claves configuradas sobre una cuenta contable.
     *
     * @param cuentaId id de la cuenta contable
     * @return claves que apuntan a esa cuenta, ordenadas
     */
    @Query("SELECT p.clave FROM ParametroContable p WHERE p.cuentaContable.idCuenta = :cuentaId ORDER BY p.clave")
    List<String> findClavesByCuentaId(@Param("cuentaId") Long cuentaId);
}
