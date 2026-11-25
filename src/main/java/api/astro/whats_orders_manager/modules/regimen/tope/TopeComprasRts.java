package api.astro.whats_orders_manager.modules.regimen.tope;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Parámetros del tope anual de compras del régimen simplificado de un año (T-056, ADR-072). El tope es
 * {@code salarioBase x multiplicador}. El multiplicador (186, Decreto 43881-H) y los umbrales (80 % y 100 %) no están en
 * el código: son los valores por defecto de la columna, que se aplican al insertar con {@code null} (DynamicInsert).
 * {@code nivelNotificado} es el último nivel avisado del año, para no repetir la notificación.
 */
@Entity
@Table(name = "tope_compras_rts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tope_compras_rts_anio", columnNames = "anio")
})
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopeComprasRts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "salario_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal salarioBase;

    /** Sin nullable=false a propósito: null al insertar deja el DEFAULT de la columna (186). */
    @Column(precision = 7, scale = 2, columnDefinition = "DECIMAL(7,2) DEFAULT 186.00")
    private BigDecimal multiplicador;

    @Column(name = "umbral_aviso", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 80.00")
    private BigDecimal umbralAviso;

    @Column(name = "umbral_tope", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 100.00")
    private BigDecimal umbralTope;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_notificado", length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'BAJO'")
    private NivelTope nivelNotificado;

    @Column(length = 300)
    private String nota;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /** Salario base x multiplicador; null si falta alguno. */
    public BigDecimal tope() {
        return salarioBase == null || multiplicador == null ? null : salarioBase.multiply(multiplicador).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
