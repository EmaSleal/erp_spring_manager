package api.astro.whats_orders_manager.modules.proveedores.model;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "proveedores", indexes = {
    @Index(name = "idx_prov_cedula", columnList = "cedula"),
    @Index(name = "idx_prov_nombre", columnList = "nombre"),
    @Index(name = "idx_prov_activo", columnList = "activo"),
    @Index(name = "idx_prov_tipo", columnList = "tipo_proveedor")
})
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String cedula;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_proveedor", nullable = false, length = 30)
    private TipoProveedor tipoProveedor;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CategoriaProveedor categoria;

    // Contact
    @Column(length = 100)
    private String contacto;

    @Column(length = 15)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(name = "sitio_web", length = 100)
    private String sitioWeb;

    // Address
    @Column(length = 500)
    private String direccion;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 50)
    @Builder.Default
    private String pais = "Costa Rica";

    // Financials — uses MonedaFE enum (CRC/USD/EUR) instead of a Moneda entity
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private MonedaFE moneda = MonedaFE.CRC;

    @Column(name = "dias_credito")
    @Builder.Default
    private Integer diasCredito = 0;

    @Column(name = "limite_credito", precision = 19, scale = 2)
    private BigDecimal limiteCredito;

    @Column(name = "saldo_pendiente", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    @Column(name = "descuento_habitual", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descuentoHabitual = BigDecimal.ZERO;

    // Evaluation
    @Column(precision = 3, scale = 2)
    private BigDecimal calificacion;

    @Column(name = "total_compras")
    @Builder.Default
    private Integer totalCompras = 0;

    // Banking
    @Column(length = 100)
    private String banco;

    @Column(name = "numero_cuenta", length = 50)
    private String numeroCuenta;

    @Column(name = "cuenta_iban", length = 50)
    private String cuentaIban;

    @Column(name = "sinpe_movil", length = 15)
    private String sinpeMovil;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(length = 1000)
    private String notas;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
