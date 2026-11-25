package api.astro.whats_orders_manager.modules.regimen.model;

import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tramo de régimen tributario con vigencia (T-056, ADR-072). Un tramo rige desde {@code vigenteDesde} hasta el día
 * anterior al inicio del siguiente tramo: no hay fecha de fin ni régimen por defecto. El régimen de un documento es el
 * del tramo con el mayor {@code vigenteDesde} menor o igual a la fecha del documento (ADR-014).
 */
@Entity
@Table(name = "regimen_tributario", uniqueConstraints = {
    @UniqueConstraint(name = "uk_regimen_tributario_desde", columnNames = "vigente_desde")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegimenTributario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Regimen regimen;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(length = 300)
    private String nota;

    /** Usuario que hizo el último alta o corrección; null en la semilla de la migración. */
    @Column(name = "usuario_id")
    private Integer usuarioId;

    /** La escribe el servicio con el Clock de la aplicación (America/Costa_Rica, ADR-060). */
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
