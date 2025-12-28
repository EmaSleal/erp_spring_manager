package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad que almacena la configuración general de la empresa.
 * Incluye datos legales, fiscales, de contacto y branding.
 * 
 * Solo debe existir UN registro en esta tabla.
 */
@Entity
@Table(name = "configuracion_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ConfiguracionEmpresa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Integer idConfiguracion;

    // ==================== DATOS LEGALES ====================
    
    @Column(name = "razon_social", nullable = false, length = 255)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 255)
    private String nombreComercial;

    @Column(name = "rfc", length = 13)
    private String rfc;

    @Column(name = "regimen_fiscal", length = 100)
    private String regimenFiscal;

    // ==================== DIRECCIÓN ====================
    
    @Column(name = "direccion_calle", length = 255)
    private String direccionCalle;

    @Column(name = "direccion_numero", length = 20)
    private String direccionNumero;

    @Column(name = "direccion_colonia", length = 100)
    private String direccionColonia;

    @Column(name = "direccion_ciudad", length = 100)
    private String direccionCiudad;

    @Column(name = "direccion_estado", length = 100)
    private String direccionEstado;

    @Column(name = "direccion_codigo_postal", length = 10)
    private String direccionCodigoPostal;

    @Builder.Default
    @Column(name = "direccion_pais", length = 100)
    private String direccionPais = "México";

    // ==================== CONTACTO ====================
    
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "sitio_web", length = 255)
    private String sitioWeb;

    // ==================== LOGO Y BRANDING ====================
    
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Builder.Default
    @Column(name = "color_primario", length = 7)
    private String colorPrimario = "#007bff";

    @Builder.Default
    @Column(name = "color_secundario", length = 7)
    private String colorSecundario = "#6c757d";

    // ==================== AUDITORÍA ====================
    
    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @LastModifiedBy
    @Column(name = "update_by")
    private Integer updateBy;

    @LastModifiedDate
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Obtiene la dirección completa formateada en una sola línea.
     * 
     * @return Dirección completa como String
     */
    public String getDireccionCompleta() {
        StringBuilder sb = new StringBuilder();
        
        if (direccionCalle != null && !direccionCalle.isEmpty()) {
            sb.append(direccionCalle);
        }
        
        if (direccionNumero != null && !direccionNumero.isEmpty()) {
            sb.append(" ").append(direccionNumero);
        }
        
        if (direccionColonia != null && !direccionColonia.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(direccionColonia);
        }
        
        if (direccionCiudad != null && !direccionCiudad.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(direccionCiudad);
        }
        
        if (direccionEstado != null && !direccionEstado.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(direccionEstado);
        }
        
        if (direccionCodigoPostal != null && !direccionCodigoPostal.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("C.P. ").append(direccionCodigoPostal);
        }
        
        if (direccionPais != null && !direccionPais.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(direccionPais);
        }
        
        return sb.toString();
    }

    /**
     * Obtiene el nombre para mostrar (comercial si existe, si no la razón social).
     * 
     * @return Nombre a mostrar
     */
    public String getNombreDisplay() {
        if (nombreComercial != null && !nombreComercial.isEmpty()) {
            return nombreComercial;
        }
        return razonSocial != null ? razonSocial : "";
    }

    /**
     * Verifica si la empresa tiene logo configurado.
     * 
     * @return true si tiene logo, false si no
     */
    public boolean tieneLogoConfigurado() {
        return logoUrl != null && !logoUrl.isEmpty();
    }

    /**
     * Verifica si los datos fiscales están completos.
     * 
     * @return true si RFC y régimen fiscal están configurados
     */
    public boolean tieneDatosFiscalesCompletos() {
        return rfc != null && !rfc.isEmpty() && 
               regimenFiscal != null && !regimenFiscal.isEmpty();
    }

    @Override
    public String toString() {
        return "ConfiguracionEmpresa{" +
                "idConfiguracion=" + idConfiguracion +
                ", razonSocial='" + razonSocial + '\'' +
                ", nombreComercial='" + nombreComercial + '\'' +
                ", rfc='" + rfc + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
