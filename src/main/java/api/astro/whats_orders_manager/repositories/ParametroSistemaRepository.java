package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.ParametroSistema;
import api.astro.whats_orders_manager.models.enums.CategoriaParametro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para gestionar parámetros del sistema
 */
@Repository
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Integer> {
    
    /**
     * Busca un parámetro por su clave única
     * @param clave Clave del parámetro
     * @return Optional con el parámetro si existe
     */
    Optional<ParametroSistema> findByClave(String clave);
    
    /**
     * Obtiene todos los parámetros de una categoría específica
     * @param categoria Categoría de parámetros
     * @return Lista de parámetros de la categoría
     */
    List<ParametroSistema> findByCategoria(CategoriaParametro categoria);
    
    /**
     * Obtiene todos los parámetros editables
     * @param editable true para obtener solo editables, false para no editables
     * @return Lista de parámetros según el criterio
     */
    List<ParametroSistema> findByEditable(Boolean editable);
    
    /**
     * Obtiene parámetros de una categoría que sean editables
     * @param categoria Categoría de parámetros
     * @param editable true para editables, false para no editables
     * @return Lista de parámetros filtrados
     */
    List<ParametroSistema> findByCategoriaAndEditable(CategoriaParametro categoria, Boolean editable);
    
    /**
     * Verifica si existe un parámetro con la clave dada
     * @param clave Clave del parámetro
     * @return true si existe
     */
    boolean existsByClave(String clave);
}
