package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Registro de la aceptación (o rechazo) del comprobante del proveedor con su mensaje receptor (T-054, ADR-067 §4). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AceptacionComprobanteDTO {
    /** ACEPTADO o RECHAZADO. */
    private EstadoAceptacionComprobante estado;
    /** Fecha del mensaje receptor: no futura y no anterior a la fecha del comprobante. */
    private LocalDate fecha;
    /** Número del mensaje receptor (hasta 50 caracteres). */
    private String numero;
}
