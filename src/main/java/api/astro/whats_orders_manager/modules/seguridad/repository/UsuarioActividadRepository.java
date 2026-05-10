package api.astro.whats_orders_manager.modules.seguridad.repository;

import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioActividad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * USUARIO ACTIVIDAD REPOSITORY
 * ERP Orders Manager
 * ============================================================================
 * Repository para gestión de actividades de usuarios.
 * Proporciona queries para auditoría y reportes.
 * 
 * @version 1.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */
@Repository
public interface UsuarioActividadRepository extends JpaRepository<UsuarioActividad, Long> {
    
    // ==================== BÚSQUEDAS BÁSICAS ====================
    
    /**
     * Busca todas las actividades de un usuario
     */
    List<UsuarioActividad> findByUsuario_IdUsuario(Integer idUsuario);
    
    /**
     * Busca actividades de un usuario con paginación
     */
    Page<UsuarioActividad> findByUsuario_IdUsuario(Integer idUsuario, Pageable pageable);
    
    /**
     * Busca actividades por tipo
     */
    List<UsuarioActividad> findByTipoActividad(String tipoActividad);
    
    /**
     * Busca actividades por nivel de importancia
     */
    List<UsuarioActividad> findByNivel(String nivel);
    
    /**
     * Busca actividades por resultado
     */
    List<UsuarioActividad> findByResultado(String resultado);
    
    // ==================== BÚSQUEDAS POR ENTIDAD ====================
    
    /**
     * Busca actividades relacionadas a una entidad específica
     */
    List<UsuarioActividad> findByEntidadAndIdEntidad(String entidad, Integer idEntidad);
    
    /**
     * Busca actividades de un usuario sobre una entidad específica
     */
    @Query("SELECT ua FROM UsuarioActividad ua WHERE ua.usuario.idUsuario = :idUsuario " +
           "AND ua.entidad = :entidad AND ua.idEntidad = :idEntidad")
    List<UsuarioActividad> findByUsuarioAndEntidad(
            @Param("idUsuario") Integer idUsuario,
            @Param("entidad") String entidad,
            @Param("idEntidad") Integer idEntidad
    );
    
    // ==================== BÚSQUEDAS POR FECHA ====================
    
    /**
     * Busca actividades de un usuario entre fechas
     */
    @Query("SELECT ua FROM UsuarioActividad ua WHERE ua.usuario.idUsuario = :idUsuario " +
           "AND ua.fechaActividad BETWEEN :fechaInicio AND :fechaFin")
    List<UsuarioActividad> findByUsuarioAndFechas(
            @Param("idUsuario") Integer idUsuario,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * Busca actividades entre fechas con paginación
     */
    @Query("SELECT ua FROM UsuarioActividad ua WHERE ua.fechaActividad BETWEEN :fechaInicio AND :fechaFin")
    Page<UsuarioActividad> findByFechas(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );
    
    // ==================== BÚSQUEDAS DE SEGURIDAD ====================
    
    /**
     * Busca actividades críticas
     */
    @Query("SELECT ua FROM UsuarioActividad ua WHERE ua.nivel = 'CRITICAL' ORDER BY ua.fechaActividad DESC")
    List<UsuarioActividad> findActividadesCriticas();
    
    /**
     * Busca actividades fallidas de un usuario
     */
    @Query("SELECT ua FROM UsuarioActividad ua WHERE ua.usuario.idUsuario = :idUsuario " +
           "AND ua.resultado = 'FAILURE' ORDER BY ua.fechaActividad DESC")
    List<UsuarioActividad> findActividadesFallidasByUsuario(@Param("idUsuario") Integer idUsuario);
    
    /**
     * Busca actividades sospechosas (múltiples intentos fallidos)
     */
    @Query("SELECT ua FROM UsuarioActividad ua WHERE ua.resultado = 'FAILURE' " +
           "AND ua.tipoActividad = 'LOGIN' AND ua.fechaActividad > :fechaLimite " +
           "GROUP BY ua.usuario.idUsuario HAVING COUNT(ua) >= :intentosMinimos")
    List<UsuarioActividad> findActividadesSospechosas(
            @Param("fechaLimite") LocalDateTime fechaLimite,
            @Param("intentosMinimos") Long intentosMinimos
    );
    
    // ==================== BÚSQUEDAS POR IP ====================
    
    /**
     * Busca actividades por dirección IP
     */
    List<UsuarioActividad> findByIpAddress(String ipAddress);
    
    /**
     * Busca actividades de un usuario desde una IP específica
     */
    @Query("SELECT ua FROM UsuarioActividad ua WHERE ua.usuario.idUsuario = :idUsuario AND ua.ipAddress = :ipAddress")
    List<UsuarioActividad> findByUsuarioAndIp(
            @Param("idUsuario") Integer idUsuario,
            @Param("ipAddress") String ipAddress
    );
    
    // ==================== ESTADÍSTICAS ====================
    
    /**
     * Cuenta actividades de un usuario
     */
    long countByUsuario_IdUsuario(Integer idUsuario);
    
    /**
     * Cuenta actividades por tipo
     */
    long countByTipoActividad(String tipoActividad);
    
    /**
     * Cuenta actividades por nivel
     */
    long countByNivel(String nivel);
    
    /**
     * Cuenta actividades por resultado
     */
    long countByResultado(String resultado);
    
    /**
     * Cuenta actividades de un usuario por tipo
     */
    @Query("SELECT COUNT(ua) FROM UsuarioActividad ua WHERE ua.usuario.idUsuario = :idUsuario " +
           "AND ua.tipoActividad = :tipoActividad")
    long countByUsuarioAndTipo(
            @Param("idUsuario") Integer idUsuario,
            @Param("tipoActividad") String tipoActividad
    );
    
    // ==================== REPORTES ====================
    
    /**
     * Obtiene las últimas N actividades de un usuario
     */
    @Query("SELECT ua FROM UsuarioActividad ua WHERE ua.usuario.idUsuario = :idUsuario " +
           "ORDER BY ua.fechaActividad DESC LIMIT :limite")
    List<UsuarioActividad> findUltimasActividadesByUsuario(
            @Param("idUsuario") Integer idUsuario,
            @Param("limite") int limite
    );
    
    /**
     * Obtiene actividades recientes del sistema
     */
    @Query("SELECT ua FROM UsuarioActividad ua ORDER BY ua.fechaActividad DESC LIMIT :limite")
    List<UsuarioActividad> findActividadesRecientes(@Param("limite") int limite);
}
