package api.astro.whats_orders_manager.modules.cliente.model;

import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.sql.Timestamp;
import java.util.Objects;


@Entity
@Table(name = "cliente")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCliente")
    private Integer idCliente;
    private String nombre;

    @CreatedDate
    @Column(name = "fechaRegistro", updatable = false)
    private Timestamp fechaRegistro;

    @Column(name= "tipoCliente")
    private InvoiceType tipoCliente;

    @Column(name = "email", length = 100)
    private String email; // Email del cliente para facturas y comunicaciones

    //identifiacion
    @Column(name = "identificacion", length = 20, unique = true)
    private String identificacion;

    // ========== CAMPOS PARA FACTURACIÓN ELECTRÓNICA COSTA RICA ==========
    
    /**
     * Tipo de identificación según Hacienda CR
     * 01 = Cédula Física, 02 = Cédula Jurídica, 03 = DIMEX, 04 = NITE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion", length = 20)
    private TipoIdentificacion tipoIdentificacion;
    
    /**
     * Número de identificación limpio (sin guiones)
     * Para facturación electrónica Costa Rica
     */
    @Column(name = "numero_identificacion", length = 12)
    private String numeroIdentificacion;

    /**
     * Código de actividad económica según Hacienda CR.
     * Se usa para FE del receptor cuando aplique.
     */
    @Column(name = "codigo_actividad", length = 20)
    private String codigoActividad;
    
    /**
     * Provincia según división territorial de Costa Rica (1-7)
     * OPCIONAL para clientes
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia", referencedColumnName = "codigo")
    private ProvinciaCostaRica provincia;
    
    /**
     * Cantón dentro de la provincia (código de 2 dígitos)
     * OPCIONAL para clientes
     */
    @Column(name = "canton", length = 2)
    private String canton;
    
    /**
     * Distrito dentro del cantón (código de 2 dígitos)
     * OPCIONAL para clientes
     */
    @Column(name = "distrito", length = 2)
    private String distrito;
    
    /**
     * Otras señas de la ubicación (dirección descriptiva)
     * OPCIONAL para clientes. Máximo 250 caracteres según XSD
     */
    @Column(name = "otras_senas", length = 250)
    private String otrasSenas;
    
    // ========== FIN CAMPOS FACTURACIÓN ELECTRÓNICA ==========

    @Column(name = "requiere_factura_electronica")
    private Boolean requiereFacturaElectronica = true; // Default: sí requiere facturación electrónica

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

    @ManyToOne
    //foreign key name FK_Cliente_Usuario
    @JoinColumn(name = "idUsuario", referencedColumnName = "telefono")
    private Usuario usuario;

    // Getters y Setters

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Cliente cliente = (Cliente) o;
        return getIdCliente() != null && Objects.equals(getIdCliente(), cliente.getIdCliente());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}