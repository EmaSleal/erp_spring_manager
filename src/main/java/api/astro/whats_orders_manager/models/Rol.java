package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un rol del sistema.
 * Migrado desde api.astro.whats_orders_manager.config.MatrizPermisos
 * para permitir gestión dinámica desde base de datos.
 */
@Entity
@Table(name = "rol")
@Data
@EqualsAndHashCode(exclude = {"permisos", "usuarios"})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long idRol;

    /**
     * Código único del rol (ej: "ADMIN", "GERENTE", "VENDEDOR")
     */
    @Column(name = "codigo", unique = true, nullable = false, length = 50)
    private String codigo;

    /**
     * Nombre descriptivo del rol (ej: "Administrador")
     */
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Descripción del rol y sus responsabilidades
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Indica si el rol está activo en el sistema
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

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

    /**
     * Permisos asignados a este rol
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "id_rol"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso")
    )
    private Set<Permiso> permisos = new HashSet<>();

    /**
     * Usuarios que tienen este rol
     * LAZY para evitar problemas de performance y serialización
     */
    @OneToMany(mappedBy = "rol", fetch = FetchType.LAZY)
    private Set<Usuario> usuarios = new HashSet<>();

    // Métodos helper

    /**
     * Verifica si el rol tiene un permiso específico
     */
    public boolean tienePermiso(Permiso permiso) {
        return permisos.contains(permiso);
    }

    /**
     * Verifica si el rol tiene al menos uno de los permisos
     */
    public boolean tieneAlgunPermiso(Permiso... permisos) {
        for (Permiso permiso : permisos) {
            if (this.permisos.contains(permiso)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si el rol tiene todos los permisos
     */
    public boolean tieneTodosLosPermisos(Permiso... permisos) {
        for (Permiso permiso : permisos) {
            if (!this.permisos.contains(permiso)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Agrega un permiso al rol
     */
    public void agregarPermiso(Permiso permiso) {
        this.permisos.add(permiso);
        permiso.getRoles().add(this);
    }

    /**
     * Elimina un permiso del rol
     */
    public void eliminarPermiso(Permiso permiso) {
        this.permisos.remove(permiso);
        permiso.getRoles().remove(this);
    }

    @Override
    public String toString() {
        return "Rol{" +
                "codigo='" + codigo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", permisos=" + permisos.size() +
                '}';
    }
}
