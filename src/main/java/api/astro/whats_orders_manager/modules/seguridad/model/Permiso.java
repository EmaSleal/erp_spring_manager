package api.astro.whats_orders_manager.modules.seguridad.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un permiso del sistema.
 * Migrado desde el enum api.astro.whats_orders_manager.enums.Permiso
 * para permitir gestión dinámica desde base de datos.
 */
@Entity
@Table(name = "permiso")
@Data
@EqualsAndHashCode(exclude = {"roles", "usuarioPermisos"})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Long idPermiso;

    /**
     * Código único del permiso (ej: "FACTURA_VER", "CLIENTE_CREAR")
     */
    @Column(name = "codigo", unique = true, nullable = false, length = 100)
    private String codigo;

    /**
     * Nombre descriptivo del permiso (ej: "Ver facturas")
     */
    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    /**
     * Descripción detallada del permiso
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Categoría del permiso (ej: "Facturación", "Clientes", "Productos")
     */
    @Column(name = "categoria", nullable = false, length = 50)
    private String categoria;

    /**
     * Indica si el permiso es crítico (solo para ADMIN)
     */
    @Column(name = "es_critico", nullable = false)
    private Boolean esCritico = false;

    /**
     * Indica si el permiso está activo en el sistema
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private LocalDateTime createDate;

    @CreatedDate
    @Column(name = "updateDate")
    private LocalDateTime updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @CreatedBy
    @Column(name = "updateBy")
    private Integer updateBy;

    /**
     * Relación con roles que tienen este permiso
     */
    @ManyToMany(mappedBy = "permisos")
    private Set<Rol> roles = new HashSet<>();

    /**
     * Relación con usuarios que tienen este permiso asignado directamente
     */
    @OneToMany(mappedBy = "permiso", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UsuarioPermiso> usuarioPermisos = new HashSet<>();

    // Métodos helper para compatibilidad con código existente

    public boolean esCritico() {
        return this.esCritico != null && this.esCritico;
    }

    public String getCategoria() {
        return this.categoria;
    }

    @Override
    public String toString() {
        return "Permiso{" +
                "codigo='" + codigo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", categoria='" + categoria + '\'' +
                ", critico=" + esCritico +
                '}';
    }
}
