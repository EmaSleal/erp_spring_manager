package api.astro.whats_orders_manager.modules.rrhh.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Parámetros de nómina propios de la empresa (convenios, no tasas legales) vigentes para un período.
 * Mismo esquema de versionado que {@link ParametroCCSS}: un registro vigente por rango de fechas;
 * vigenciaHasta=null significa vigencia indefinida (ADR-013).
 */
@Entity
@Table(name = "parametros_nomina_empresa", indexes = {
    @Index(name = "idx_nomina_empresa_vigencia", columnList = "vigencia_desde, vigencia_hasta")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ParametroNominaEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Aporte patronal a la Asociación Solidarista sobre el bruto del afiliado. Ejemplo 2026: 0.0533 */
    @Column(name = "porcentaje_aporte_patronal_ase", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeAportePatronalAse;

    /** Tasa INS riesgos del trabajo (carga patronal, ADR-033). null = no configurada: no se calcula. */
    @Column(name = "porcentaje_ins_riesgos_trabajo", precision = 5, scale = 4)
    private BigDecimal porcentajeInsRiesgosTrabajo;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    /** null significa vigencia indefinida */
    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
