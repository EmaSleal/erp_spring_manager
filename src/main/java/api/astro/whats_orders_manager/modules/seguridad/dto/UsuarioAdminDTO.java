package api.astro.whats_orders_manager.modules.seguridad.dto;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 * USUARIO ADMIN DTO - Data Transfer Object
 * WhatsApp Orders Manager
 * ============================================================================
 * DTO para transferencia de datos de usuario en el panel de administración.
 * 
 * Funcionalidades:
 * - Exposición segura de datos de usuario
 * - Información adicional para administradores
 * - Conversión bidireccional Usuario <-> DTO
 * - Password solo para formularios (no incluida en fromEntity)
 * 
 * Casos de uso:
 * - API REST de gestión de usuarios
 * - Listado de usuarios en panel admin
 * - Creación/edición de usuarios (incluye password)
 * - Reporte de actividad de usuarios
 * 
 * @version 1.1
 * @since Sprint 4 - Fase 4
 * @date 22/12/2025
 * @updated 28/12/2025 - Agregado campo password para formularios
 * ============================================================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioAdminDTO {

    // ==================== IDENTIFICACIÓN ====================
    
    private Integer idUsuario;
    private String nombre;
    private String telefono;
    private String email;
    private String rol;
    
    // ==================== PERFIL ====================
    
    private String avatar;
    
    // ==================== ESTADO ====================
    
    private Boolean activo;
    private Boolean bloqueado;
    private Integer intentosFallidos;
    
    // ==================== BLOQUEO ====================
    
    private Timestamp fechaBloqueo;
    private String razonBloqueo;
    private Integer bloqueadoPor;
    private String nombreBloqueadoPor; // Nombre del admin que bloqueó
    
    // ==================== SEGURIDAD ====================
    
    /**
     * Password del usuario (solo para formularios de creación/edición)
     * No se incluye en fromEntity() por seguridad
     */
    private String password;
    
    private Boolean requireCambioPassword;
    private Timestamp fechaExpiracionPassword;
    
    // ==================== AUDITORÍA ====================
    
    private Timestamp ultimoAcceso;
    private Timestamp createDate;
    private Timestamp updateDate;
    private Integer createBy;
    private Integer updateBy;
    
    // ==================== INFORMACIÓN ADICIONAL ====================
    
    private Long cantidadSesionesActivas;
    private Long totalActividades;
    private String estadoDescripcion; // Descripción amigable del estado
    
    // ==================== CONSTRUCTORES ====================

    /**
     * Crea un DTO desde una entidad Usuario
     */
    public static UsuarioAdminDTO fromEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        
        return UsuarioAdminDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .telefono(usuario.getTelefono())
                .email(usuario.getEmail())
                .rol(usuario.getRolEntity() != null ? usuario.getRolEntity().getNombre() : usuario.getRol())
                .avatar(usuario.getAvatar())
                .activo(usuario.getActivo())
                .bloqueado(usuario.getBloqueado())
                .intentosFallidos(usuario.getIntentosFallidos())
                .fechaBloqueo(usuario.getFechaBloqueo())
                .razonBloqueo(usuario.getRazonBloqueo())
                .bloqueadoPor(usuario.getBloqueadoPor())
                .requireCambioPassword(usuario.getRequireCambioPassword())
                .fechaExpiracionPassword(usuario.getFechaExpiracionPassword())
                .ultimoAcceso(usuario.getUltimoAcceso())
                .createDate(usuario.getCreateDate())
                .updateDate(usuario.getUpdateDate())
                .createBy(usuario.getCreateBy())
                .updateBy(usuario.getUpdateBy())
                .estadoDescripcion(obtenerEstadoDescripcion(usuario))
                .build();
    }

    /**
     * Convierte el DTO a una entidad Usuario
     */
    public Usuario toEntity() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(this.idUsuario);
        usuario.setNombre(this.nombre);
        usuario.setTelefono(this.telefono);
        usuario.setEmail(this.email);
        usuario.setRol(this.rol);
        usuario.setAvatar(this.avatar);
        usuario.setActivo(this.activo);
        usuario.setBloqueado(this.bloqueado);
        usuario.setIntentosFallidos(this.intentosFallidos);
        usuario.setFechaBloqueo(this.fechaBloqueo);
        usuario.setRazonBloqueo(this.razonBloqueo);
        usuario.setBloqueadoPor(this.bloqueadoPor);
        usuario.setRequireCambioPassword(this.requireCambioPassword);
        usuario.setFechaExpiracionPassword(this.fechaExpiracionPassword);
        usuario.setUltimoAcceso(this.ultimoAcceso);
        
        return usuario;
    }

    /**
     * Actualiza una entidad existente con los datos del DTO
     */
    public void updateEntity(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        
        if (this.nombre != null) usuario.setNombre(this.nombre);
        if (this.telefono != null) usuario.setTelefono(this.telefono);
        if (this.email != null) usuario.setEmail(this.email);
        if (this.rol != null) usuario.setRol(this.rol);
        if (this.avatar != null) usuario.setAvatar(this.avatar);
        if (this.activo != null) usuario.setActivo(this.activo);
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Obtiene una descripción amigable del estado del usuario
     */
    private static String obtenerEstadoDescripcion(Usuario usuario) {
        if (usuario.getBloqueado() != null && usuario.getBloqueado()) {
            return "Bloqueado";
        }
        if (usuario.getActivo() == null || !usuario.getActivo()) {
            return "Inactivo";
        }
        if (usuario.getRequireCambioPassword() != null && usuario.getRequireCambioPassword()) {
            return "Requiere cambio de contraseña";
        }
        return "Activo";
    }

    /**
     * Formatea la fecha de último acceso
     */
    public String getUltimoAccesoFormateado() {
        if (this.ultimoAcceso == null) {
            return "Nunca";
        }
        
        LocalDateTime fecha = this.ultimoAcceso.toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return fecha.format(formatter);
    }

    /**
     * Formatea la fecha de creación
     */
    public String getFechaCreacionFormateada() {
        if (this.createDate == null) {
            return "-";
        }
        
        LocalDateTime fecha = this.createDate.toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return fecha.format(formatter);
    }

    /**
     * Verifica si el usuario está bloqueado
     */
    public boolean estaBloqueado() {
        return this.bloqueado != null && this.bloqueado;
    }

    /**
     * Verifica si el usuario está activo
     */
    public boolean estaActivo() {
        return this.activo != null && this.activo && !estaBloqueado();
    }

    /**
     * Obtiene el badge de estado para UI
     */
    public String getBadgeEstado() {
        if (estaBloqueado()) {
            return "danger"; // Rojo
        }
        if (!estaActivo()) {
            return "secondary"; // Gris
        }
        if (this.requireCambioPassword != null && this.requireCambioPassword) {
            return "warning"; // Amarillo
        }
        return "success"; // Verde
    }

    /**
     * Obtiene el icono de estado para UI
     */
    public String getIconoEstado() {
        if (estaBloqueado()) {
            return "fa-lock";
        }
        if (!estaActivo()) {
            return "fa-ban";
        }
        if (this.requireCambioPassword != null && this.requireCambioPassword) {
            return "fa-exclamation-triangle";
        }
        return "fa-check-circle";
    }
}
