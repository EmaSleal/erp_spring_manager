package api.astro.whats_orders_manager.modules.regimen.verificacion;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Una verificación del régimen contra /fe/ae (T-056). Guarda lo que tenía el ERP y lo que dijo Hacienda; nunca cambia
 * regimen_tributario. Retención corta: se borran las de más de N días, salvo la última.
 */
@Entity
@Table(name = "verificacion_regimen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificacionRegimen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Hora de la verificación según el Clock de la aplicación (America/Costa_Rica). */
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /** Régimen vigente hoy en el ERP (GENERAL o SIMPLIFICADO); null si no hay tramo. */
    @Column(name = "regimen_local", length = 12)
    private String regimenLocal;

    @Column(name = "codigo_hacienda")
    private Integer codigoHacienda;

    @Column(name = "descripcion_hacienda", length = 200)
    private String descripcionHacienda;

    /** Traducción del código: GENERAL, SIMPLIFICADO u OTRO. */
    @Column(name = "regimen_hacienda", length = 12)
    private String regimenHacienda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ResultadoVerificacion resultado;

    /** Motivo cuando no hubo respuesta utilizable. */
    @Column(length = 300)
    private String detalle;

    /** Quién la pidió a mano; null en la del job. */
    @Column(name = "usuario_id")
    private Integer usuarioId;
}
