package api.astro.whats_orders_manager.modules.facturacion.electronica.repository;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para configuraciones de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Repository
public interface ConfiguracionHaciendaRepository extends JpaRepository<ConfiguracionHacienda, Long> {
    
    /**
     * Encuentra la configuración activa de una empresa.
     */
    @Query("SELECT c FROM ConfiguracionHacienda c WHERE c.empresa.idConfiguracion = :empresaId AND c.activa = true")
    Optional<ConfiguracionHacienda> findActivaByEmpresaId(@Param("empresaId") Integer empresaId);
    
    /**
     * Encuentra configuración por empresa y ambiente.
     */
    @Query("SELECT c FROM ConfiguracionHacienda c WHERE c.empresa.idConfiguracion = :empresaId AND c.ambiente = :ambiente")
    Optional<ConfiguracionHacienda> findByEmpresaIdAndAmbiente(
        @Param("empresaId") Integer empresaId,
        @Param("ambiente") AmbienteHacienda ambiente
    );
    
    /**
     * Verifica si existe configuración activa para una empresa.
     */
    @Query("SELECT COUNT(c) > 0 FROM ConfiguracionHacienda c WHERE c.empresa.idConfiguracion = :empresaId AND c.activa = true")
    boolean existsActivaByEmpresaId(@Param("empresaId") Integer empresaId);
    
    /**
     * Encuentra todas las configuraciones de una empresa.
     */
    @Query("SELECT c FROM ConfiguracionHacienda c JOIN FETCH c.empresa e WHERE e.idConfiguracion = :empresaId "
        + "ORDER BY c.activa DESC, c.createdAt DESC")
    java.util.List<ConfiguracionHacienda> findAllByEmpresaId(@Param("empresaId") Integer empresaId);

    /** T-041: una configuración con su empresa cargada, para mapearla fuera de una transacción. */
    @Query("SELECT c FROM ConfiguracionHacienda c JOIN FETCH c.empresa WHERE c.id = :id")
    Optional<ConfiguracionHacienda> findConEmpresaById(@Param("id") Long id);

    /** T-041: las configuraciones de una empresa sin leer las columnas cifradas (mismo orden que findAllByEmpresaId). */
    @Query("SELECT c.id AS id, e AS empresa, c.ambiente AS ambiente, c.usuarioHacienda AS usuarioHacienda, "
        + "c.rutaCertificado AS rutaCertificado, c.activa AS activa, c.consecutivoFactura AS consecutivoFactura, "
        + "c.consecutivoTiquete AS consecutivoTiquete, c.consecutivoNotaCredito AS consecutivoNotaCredito, "
        + "c.consecutivoNotaDebito AS consecutivoNotaDebito, c.createdAt AS createdAt, c.updatedAt AS updatedAt "
        + "FROM ConfiguracionHacienda c JOIN c.empresa e WHERE e.idConfiguracion = :empresaId "
        + "ORDER BY c.activa DESC, c.createdAt DESC")
    java.util.List<ConfiguracionHaciendaSinCredenciales> findSinCredencialesByEmpresaId(
        @Param("empresaId") Integer empresaId);

    /** T-041: una configuración sin leer las columnas cifradas. */
    @Query("SELECT c.id AS id, e AS empresa, c.ambiente AS ambiente, c.usuarioHacienda AS usuarioHacienda, "
        + "c.rutaCertificado AS rutaCertificado, c.activa AS activa, c.consecutivoFactura AS consecutivoFactura, "
        + "c.consecutivoTiquete AS consecutivoTiquete, c.consecutivoNotaCredito AS consecutivoNotaCredito, "
        + "c.consecutivoNotaDebito AS consecutivoNotaDebito, c.createdAt AS createdAt, c.updatedAt AS updatedAt "
        + "FROM ConfiguracionHacienda c JOIN c.empresa e WHERE c.id = :id")
    Optional<ConfiguracionHaciendaSinCredenciales> findSinCredencialesById(@Param("id") Long id);

    /**
     * T-041: vuelve a guardar una configuración cuyas credenciales no se descifran, sin cargar la entidad (cargarla
     * descifraría las cuatro columnas y fallaría). Contraseña y PIN nuevos se cifran con la clave actual al escribir
     * (CredencialCifradaConverter); los tokens guardados, que tampoco se pueden leer, se descartan y se piden de nuevo.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ConfiguracionHacienda c SET c.ambiente = :ambiente, c.usuarioHacienda = :usuario, "
        + "c.passwordHacienda = :password, c.rutaCertificado = :ruta, c.pinCertificado = :pin, "
        + "c.accessToken = NULL, c.refreshToken = NULL, c.tokenExpira = NULL, "
        + "c.consecutivoFactura = :consecutivoFactura, c.consecutivoTiquete = :consecutivoTiquete, "
        + "c.consecutivoNotaCredito = :consecutivoNotaCredito, c.consecutivoNotaDebito = :consecutivoNotaDebito, "
        + "c.updatedAt = :ahora WHERE c.id = :id")
    int restablecerCredenciales(@Param("id") Long id, @Param("ambiente") AmbienteHacienda ambiente,
                                @Param("usuario") String usuario, @Param("password") String password,
                                @Param("ruta") String ruta, @Param("pin") String pin,
                                @Param("consecutivoFactura") Long consecutivoFactura,
                                @Param("consecutivoTiquete") Long consecutivoTiquete,
                                @Param("consecutivoNotaCredito") Long consecutivoNotaCredito,
                                @Param("consecutivoNotaDebito") Long consecutivoNotaDebito,
                                @Param("ahora") java.time.LocalDateTime ahora);

    /**
     * Encuentra la primera configuración activa (independiente de empresa).
     */
    /**
     * Configuraciones activas de cualquier empresa, ordenadas por id (ADR-021: debe haber exactamente una).
     */
    java.util.List<ConfiguracionHacienda> findByActivaTrueOrderByIdAsc();
}
