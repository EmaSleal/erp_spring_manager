package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;

/**
 * Entidad que representa permisos personalizados asignados directamente a usuarios.
 * Permite sobrescribir permisos del rol con permisos específicos concedidos o denegados.
 */
@Entity
@Table(name = "usuario_permiso")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UsuarioPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_permiso")
    private Long idUsuarioPermiso;

    /**
     * Usuario al que se le asigna el permiso personalizado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /**
     * Permiso que se concede o deniega
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_permiso", nullable = false)
    private Permiso permiso;

    /**
     * Tipo de asignación: CONCEDIDO o DENEGADO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPermisoUsuario tipo;

    /**
     * Usuario que concedió/denegó el permiso
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concedido_por")
    private Usuario concedidoPor;

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
     * Enum para tipo de permiso de usuario
     */
    public enum TipoPermisoUsuario {
        CONCEDIDO,  // El usuario tiene este permiso aunque su rol no lo tenga
        DENEGADO    // El usuario NO tiene este permiso aunque su rol lo tenga
    }

    @Override
    public String toString() {
        return "UsuarioPermiso{" +
                "usuario=" + (usuario != null ? usuario.getNombre() : "null") +
                ", permiso=" + (permiso != null ? permiso.getCodigo() : "null") +
                ", tipo=" + tipo +
                '}';
    }
}
