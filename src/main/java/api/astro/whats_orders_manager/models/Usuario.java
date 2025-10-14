package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;

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

    private String rol; // Ejemplo: "USER" o "ADMIN"

    @Column(name = "avatar", length = 255)
    private String avatar; // URL o path de la imagen de perfil

    @Column(name = "activo")
    private Boolean activo = true; // Usuario activo por defecto

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
}
