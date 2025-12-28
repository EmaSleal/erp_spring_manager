package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    private String nombre;

    @Column(name = "telefono", unique = true)
    private String telefono;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    private String password; // Nueva columna para la contraseña

    private String rol; // Ejemplo: "USER" o "ADMIN" - DEPRECADO: usar idRol en su lugar

    /**
     * Relación con la tabla rol (nuevo sistema de permisos dinámicos)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol")
    private Rol rolEntity;

    /**
     * Permisos personalizados asignados directamente al usuario
     */
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioPermiso> permisosPersonalizados = new HashSet<>();

    @Column(name = "avatar", length = 255)
    private String avatar; // URL o path de la imagen de perfil

    @Column(name = "activo")
    private Boolean activo = true; // Usuario activo por defecto

    @Column(name = "bloqueado")
    private Boolean bloqueado = false; // Usuario bloqueado

    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0; // Intentos de login fallidos

    @Column(name = "fecha_bloqueo")
    private Timestamp fechaBloqueo; // Fecha de bloqueo del usuario

    @Column(name = "razon_bloqueo", length = 500)
    private String razonBloqueo; // Razón del bloqueo

    @Column(name = "bloqueado_por")
    private Integer bloqueadoPor; // ID del admin que bloqueó

    @Column(name = "require_cambio_password")
    private Boolean requireCambioPassword = false; // Forzar cambio de contraseña

    @Column(name = "fecha_expiracion_password")
    private Timestamp fechaExpiracionPassword; // Fecha de expiración de la contraseña

    @Column(name = "ultimo_acceso")
    private Timestamp ultimoAcceso; // Fecha y hora del último acceso

    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private Timestamp createDate;

    @CreatedDate
    @Column(name = "updateDate")
    private Timestamp updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @CreatedBy
    @Column(name = "updateBy")
    private Integer updateBy;

    // Método alias para compatibilidad con vistas (usuario.id)
    public Integer getId() {
        return this.idUsuario;
    }

    public void setId(Integer id) {
        this.idUsuario = id;
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Verifica si el usuario está bloqueado
     */
    public boolean estaBloqueado() {
        return this.bloqueado != null && this.bloqueado;
    }

    /**
     * Bloquea el usuario
     */
    public void bloquear(String razon, Integer adminId) {
        this.bloqueado = true;
        this.fechaBloqueo = new Timestamp(System.currentTimeMillis());
        this.razonBloqueo = razon;
        this.bloqueadoPor = adminId;
    }

    /**
     * Desbloquea el usuario
     */
    public void desbloquear() {
        this.bloqueado = false;
        this.fechaBloqueo = null;
        this.razonBloqueo = null;
        this.bloqueadoPor = null;
        this.intentosFallidos = 0;
    }

    /**
     * Incrementa los intentos fallidos de login
     */
    public void incrementarIntentosFallidos() {
        this.intentosFallidos = (this.intentosFallidos == null ? 0 : this.intentosFallidos) + 1;
    }

    /**
     * Resetea los intentos fallidos de login
     */
    public void resetearIntentosFallidos() {
        this.intentosFallidos = 0;
    }

    /**
     * Verifica si debe cambiar la contraseña
     */
    public boolean debeActualizarPassword() {
        if (this.requireCambioPassword != null && this.requireCambioPassword) {
            return true;
        }
        if (this.fechaExpiracionPassword != null) {
            return new Timestamp(System.currentTimeMillis()).after(this.fechaExpiracionPassword);
        }
        return false;
    }

    /**
     * Verifica si el usuario puede iniciar sesión
     */
    public boolean puedeIniciarSesion() {
        return this.activo != null && this.activo && !estaBloqueado();
    }

    /**
     * Actualiza el último acceso
     */
    public void actualizarUltimoAcceso() {
        this.ultimoAcceso = new Timestamp(System.currentTimeMillis());
    }
}
