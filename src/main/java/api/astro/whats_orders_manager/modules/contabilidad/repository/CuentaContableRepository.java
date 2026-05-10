package api.astro.whats_orders_manager.modules.contabilidad.repository;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad CuentaContable.
 * Gestiona el plan de cuentas contables del sistema.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Repository
public interface CuentaContableRepository extends JpaRepository<CuentaContable, Long> {
    
    /**
     * Busca una cuenta por su código único.
     * @param codigo Código de la cuenta
     * @return Cuenta contable si existe
     */
    Optional<CuentaContable> findByCodigo(String codigo);
    
    /**
     * Verifica si existe una cuenta con el código dado.
     * @param codigo Código a verificar
     * @return true si existe
     */
    boolean existsByCodigo(String codigo);
    
    /**
     * Busca cuentas por tipo.
     * @param tipo Tipo de cuenta
     * @return Lista de cuentas del tipo especificado
     */
    List<CuentaContable> findByTipo(TipoCuenta tipo);
    
    /**
     * Busca cuentas por tipo que estén activas.
     * @param tipo Tipo de cuenta
     * @param activa Estado activo
     * @return Lista de cuentas activas del tipo especificado
     */
    List<CuentaContable> findByTipoAndActiva(TipoCuenta tipo, Boolean activa);
    
    /**
     * Busca todas las cuentas activas.
     * @param activa Estado activo
     * @return Lista de cuentas activas
     */
    List<CuentaContable> findByActiva(Boolean activa);
    
    /**
     * Busca cuentas por nivel jerárquico.
     * @param nivel Nivel de la cuenta (1, 2, 3, 4...)
     * @return Lista de cuentas del nivel especificado
     */
    List<CuentaContable> findByNivel(Integer nivel);
    
    /**
     * Busca cuentas raíz (nivel 1).
     * @return Lista de cuentas de nivel 1
     */
    @Query("SELECT c FROM CuentaContable c WHERE c.nivel = 1 ORDER BY c.codigo")
    List<CuentaContable> findCuentasRaiz();
    
    /**
     * Busca subcuentas de una cuenta padre.
     * @param cuentaPadreId ID de la cuenta padre
     * @return Lista de subcuentas
     */
    @Query("SELECT c FROM CuentaContable c WHERE c.cuentaPadre.idCuenta = :cuentaPadreId ORDER BY c.codigo")
    List<CuentaContable> findSubcuentasByCuentaPadreId(@Param("cuentaPadreId") Long cuentaPadreId);
    
    /**
     * Busca cuentas que aceptan movimientos directos.
     * @param aceptaMovimientos Flag de aceptación de movimientos
     * @return Lista de cuentas que aceptan movimientos
     */
    List<CuentaContable> findByAceptaMovimientosAndActiva(Boolean aceptaMovimientos, Boolean activa);
    
    /**
     * Busca cuentas por nombre (búsqueda parcial).
     * @param nombre Nombre o parte del nombre
     * @return Lista de cuentas que coinciden
     */
    @Query("SELECT c FROM CuentaContable c WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) ORDER BY c.codigo")
    List<CuentaContable> findByNombreContaining(@Param("nombre") String nombre);
    
    /**
     * Busca cuentas por código o nombre (búsqueda parcial).
     * @param busqueda Término de búsqueda
     * @return Lista de cuentas que coinciden
     */
    @Query("SELECT c FROM CuentaContable c WHERE LOWER(c.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) ORDER BY c.codigo")
    List<CuentaContable> buscarCuentas(@Param("busqueda") String busqueda);
    
    /**
     * Busca cuentas de balance (ACTIVO, PASIVO, CAPITAL).
     * @return Lista de cuentas de balance
     */
    @Query("SELECT c FROM CuentaContable c WHERE c.tipo IN ('ACTIVO', 'PASIVO', 'CAPITAL') " +
           "AND c.activa = true ORDER BY c.codigo")
    List<CuentaContable> findCuentasDeBalance();
    
    /**
     * Busca cuentas de resultados (INGRESO, EGRESO).
     * @return Lista de cuentas de resultados
     */
    @Query("SELECT c FROM CuentaContable c WHERE c.tipo IN ('INGRESO', 'EGRESO') " +
           "AND c.activa = true ORDER BY c.codigo")
    List<CuentaContable> findCuentasDeResultados();
    
    /**
     * Cuenta el número de subcuentas de una cuenta.
     * @param cuentaPadreId ID de la cuenta padre
     * @return Número de subcuentas
     */
    @Query("SELECT COUNT(c) FROM CuentaContable c WHERE c.cuentaPadre.idCuenta = :cuentaPadreId")
    Long countSubcuentas(@Param("cuentaPadreId") Long cuentaPadreId);
    
    /**
     * Verifica si una cuenta tiene movimientos contables.
     * @param cuentaId ID de la cuenta
     * @return true si tiene movimientos
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END " +
           "FROM DetalleAsiento d WHERE d.cuenta.idCuenta = :cuentaId")
    boolean tieneMovimientos(@Param("cuentaId") Long cuentaId);
    
    /**
     * Obtiene todas las cuentas ordenadas por código.
     * @return Lista de cuentas ordenadas
     */
    @Query("SELECT c FROM CuentaContable c ORDER BY c.codigo")
    List<CuentaContable> findAllOrdenadoPorCodigo();
    
    /**
     * Busca cuentas por código que empiecen con un prefijo.
     * @param prefijo Prefijo del código (ej: "1.1" para todas las del activo corriente)
     * @return Lista de cuentas que cumplen
     */
    @Query("SELECT c FROM CuentaContable c WHERE c.codigo LIKE CONCAT(:prefijo, '%') ORDER BY c.codigo")
    List<CuentaContable> findByCodigoStartingWith(@Param("prefijo") String prefijo);

    //query para findByAceptaMovimientos
    List<CuentaContable> findByAceptaMovimientos(Boolean aceptaMovimientos);
    
}
