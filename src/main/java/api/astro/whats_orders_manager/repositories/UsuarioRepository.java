package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.Usuario;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    // ==================== BÚSQUEDAS BÁSICAS ====================
    
    Optional<Usuario> findByNombre(String nombre);
    
    Optional<Usuario> findByTelefono(String telefono);
    
    Optional<Usuario> findByEmail(String email);
    
    // ==================== BÚSQUEDAS POR ESTADO ====================
    
    List<Usuario> findByRol(String rol);
    
    List<Usuario> findByActivo(Boolean activo);
    
    List<Usuario> findByBloqueado(Boolean bloqueado);
    
    // ==================== CONTADORES ====================
    
    long countByActivo(Boolean activo);
    
    long countByBloqueado(Boolean bloqueado);
    
    long countByRol(String rol);
    
    /**
     * Consulta especial para usar en AuditorAware que evita el auto-flush
     * y previene la recursión infinita durante operaciones de auditoría
     */
    @Query("SELECT u FROM Usuario u WHERE u.telefono = :telefono")
    @QueryHints(value = {
        @QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT")
    })
    Optional<Usuario> findByTelefonoWithoutFlush(@Param("telefono") String telefono);
}
