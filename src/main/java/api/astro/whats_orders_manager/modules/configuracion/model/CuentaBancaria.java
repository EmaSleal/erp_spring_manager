package api.astro.whats_orders_manager.modules.configuracion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuenta_bancaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CuentaBancaria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta_bancaria")
    private Integer idCuentaBancaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /**
     * Nombre de la entidad bancaria (ej: "Banco Nacional - Colones").
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "entidad", nullable = false, length = 100)
    private String entidad;

    /**
     * Número de cuenta IBAN (ej: CR74015104210010004988).
     */
    @Size(max = 34)
    @Column(name = "cuenta_iban", length = 34)
    private String cuentaIban;

    /**
     * Número de cuenta tradicional del banco (ej: 100-01-042-000498-5).
     */
    @Size(max = 50)
    @Column(name = "cuenta_banco", length = 50)
    private String cuentaBanco;

    /**
     * Moneda de la cuenta: CRC, USD, EUR.
     */
    @Size(max = 10)
    @Column(name = "moneda", length = 10)
    private String moneda;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Column(name = "orden")
    private Integer orden = 0;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @LastModifiedDate
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @LastModifiedBy
    @Column(name = "update_by")
    private Integer updateBy;
}
