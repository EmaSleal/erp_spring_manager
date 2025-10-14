package api.astro.whats_orders_manager.models;

import api.astro.whats_orders_manager.enums.InvoiceType;
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