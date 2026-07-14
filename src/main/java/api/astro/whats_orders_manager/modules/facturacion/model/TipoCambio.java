package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "tipos_cambio",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tipos_cambio_fecha_origen_destino",
        columnNames = {"fecha", "moneda_origen", "moneda_destino"}
    )
)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TipoCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda_origen", nullable = false, length = 10)
    private MonedaFE monedaOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda_destino", nullable = false, length = 10)
    private MonedaFE monedaDestino;

    @Column(nullable = false)
    private LocalDate fecha;

    // CRC units per 1 unit of foreign currency
    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal tasaCompra;

    @Column(nullable = false, precision = 18, scale = 5)
    private BigDecimal tasaVenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FuenteTipoCambio fuente;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Column(length = 100)
    private String creadoPor;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;
}
