package api.astro.whats_orders_manager.modules.configuracion.repository;

import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
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
     * Lista las empresas activas.
     * Solo debe existir un registro con activo = true (ADR-019); se devuelve una lista para que
     * quien la consume detecte con un mensaje claro cuando hay más de una, en lugar de un
     * NonUniqueResultException.
     *
     * @return empresas activas, ordenadas por id
     */
    @Query("SELECT e FROM Empresa e WHERE e.activo = true ORDER BY e.idEmpresa")
    List<Empresa> findEmpresasActivas();

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
