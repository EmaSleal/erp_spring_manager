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
    // V26: a lo sumo una tasa ACTIVA por fecha y par de monedas; las inactivas (historial) no colisionan
    // porque activo_unico es NULL para ellas.
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tipos_cambio_fecha_origen_destino_activo",
        columnNames = {"fecha", "moneda_origen", "moneda_destino", "activo_unico"}
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

    /**
     * Columna generada por la base (V26): 1 si la fila está activa, NULL si no. Solo existe para la
     * restricción única; Hibernate nunca la escribe y el valor en memoria no se refresca.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Column(name = "activo_unico", insertable = false, updatable = false,
            columnDefinition = "tinyint generated always as (case when activo then 1 end)")
    private Byte activoUnico;

    @Column(length = 100)
    private String creadoPor;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime fechaCreacion;
}
