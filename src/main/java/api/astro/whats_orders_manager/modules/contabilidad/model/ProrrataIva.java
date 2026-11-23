package api.astro.whats_orders_manager.modules.contabilidad.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Porcentaje provisional de prorrata del IVA de un año (T-054, ADR-067 §4). Se aplica a las líneas de uso común de las
 * CxP cuyo comprobante es de ese año. Corregirlo no recalcula las CxP ya registradas (cada línea guarda el porcentaje
 * usado): la liquidación anual de la prorrata es un asiento manual.
 */
@Entity
@Table(name = "prorrata_iva", uniqueConstraints = {
    @UniqueConstraint(name = "uk_prorrata_iva_anio", columnNames = "anio")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProrrataIva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    /** Entre 0 y 100, con 2 decimales. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(length = 300)
    private String nota;

    /** Usuario que hizo el último alta o corrección. */
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaRegistro == null) fechaRegistro = ahora;
        if (fechaActualizacion == null) fechaActualizacion = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
