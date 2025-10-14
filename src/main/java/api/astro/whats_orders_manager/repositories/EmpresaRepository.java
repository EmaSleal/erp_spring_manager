package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Empresa
 * Maneja las operaciones de base de datos relacionadas con la configuración de la empresa.
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 2
 */
@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    /**
     * Busca la empresa activa (principal)
     * Solo debe existir un registro con activo = true
     * 
     * @return Optional con la empresa activa si existe
     */
    @Query("SELECT e FROM Empresa e WHERE e.activo = true")
    Optional<Empresa> findEmpresaActiva();

    /**
     * Verifica si existe una empresa activa
     * 
     * @return true si existe al menos una empresa activa
     */
    @Query("SELECT COUNT(e) > 0 FROM Empresa e WHERE e.activo = true")
    boolean existeEmpresaActiva();

    /**
     * Busca empresa por RUC
     * Útil para validar duplicados
     * 
     * @param ruc el RUC a buscar
     * @return Optional con la empresa si existe
     */
    Optional<Empresa> findByRuc(String ruc);

    /**
     * Busca empresa por email
     * Útil para validar duplicados
     * 
     * @param email el email a buscar
     * @return Optional con la empresa si existe
     */
    Optional<Empresa> findByEmail(String email);

    /**
     * Cuenta empresas activas
     * Debe retornar siempre 1 (validación de integridad)
     * 
     * @return número de empresas activas
     */
    @Query("SELECT COUNT(e) FROM Empresa e WHERE e.activo = true")
    long contarEmpresasActivas();
}
