package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Entidad Empresa
 * Representa los datos de la empresa que se mostrarán en facturas y documentos.
 * Solo debe existir un registro activo en la base de datos.
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 2
 */
@Entity
@Table(name = "empresa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Empresa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Integer idEmpresa;

    /**
     * Nombre legal/razón social de la empresa
     */
    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    @Column(name = "nombre_empresa", nullable = false, length = 200)
    private String nombreEmpresa;

    /**
     * Nombre comercial (opcional, si es diferente al legal)
     */
    @Size(max = 200, message = "El nombre comercial no puede exceder 200 caracteres")
    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;

    /**
     * RUC / NIT / CUIT / RFC según el país
     */
    @Size(max = 20, message = "El RUC no puede exceder 20 caracteres")
    @Column(name = "ruc", length = 20)
    private String ruc;

    /**
     * Dirección fiscal de la empresa
     */
    @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
    @Column(name = "direccion", length = 300)
    private String direccion;

    /**
     * Teléfono principal de contacto
     */
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Column(name = "telefono", length = 20)
    private String telefono;

    /**
     * Email de contacto principal
     */
    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    @Column(name = "email", length = 100)
    private String email;

    /**
     * Sitio web de la empresa (opcional)
     */
    @Size(max = 200, message = "El sitio web no puede exceder 200 caracteres")
    @Column(name = "sitio_web", length = 200)
    private String sitioWeb;

    /**
     * Ruta del archivo del logo (guardado en /uploads/empresa/)
     */
    @Size(max = 255, message = "La ruta del logo no puede exceder 255 caracteres")
    @Column(name = "logo", length = 255)
    private String logo;

    /**
     * Ruta del archivo del favicon (guardado en /uploads/empresa/)
     */
    @Size(max = 255, message = "La ruta del favicon no puede exceder 255 caracteres")
    @Column(name = "favicon", length = 255)
    private String favicon;

    /**
     * Indica si este registro está activo
     * Solo debe haber un registro activo en la BD
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Campos de auditoría - Spring Data JPA
     */
    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private Timestamp createDate;

    @LastModifiedDate
    @Column(name = "updateDate")
    private Timestamp updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @LastModifiedBy
    @Column(name = "updateBy")
    private Integer updateBy;

    /**
     * Callback que se ejecuta antes de persistir la entidad
     * Establece el estado activo por defecto
     */
    @PrePersist
    protected void onCreate() {
        if (this.activo == null) {
            this.activo = true;
        }
    }

    /**
     * Método helper para obtener el nombre a mostrar
     * Retorna el nombre comercial si existe, sino el nombre legal
     */
    public String getNombreParaMostrar() {
        return (nombreComercial != null && !nombreComercial.isEmpty()) 
            ? nombreComercial 
            : nombreEmpresa;
    }

    /**
     * Método helper para verificar si tiene logo
     */
    public boolean tieneLogo() {
        return logo != null && !logo.isEmpty();
    }

    /**
     * Método helper para verificar si tiene favicon
     */
    public boolean tieneFavicon() {
        return favicon != null && !favicon.isEmpty();
    }

    /**
     * Override toString para logging y debugging
     */
    @Override
    public String toString() {
        return "Empresa{" +
                "idEmpresa=" + idEmpresa +
                ", nombreEmpresa='" + nombreEmpresa + '\'' +
                ", ruc='" + ruc + '\'' +
                ", activo=" + activo +
                '}';
    }
}
