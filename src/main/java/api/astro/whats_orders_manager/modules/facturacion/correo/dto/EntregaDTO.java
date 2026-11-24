package api.astro.whats_orders_manager.modules.facturacion.correo.dto;

import api.astro.whats_orders_manager.modules.facturacion.correo.model.EntregaComprobante;

import java.time.LocalDateTime;

/** Entrega para la API y el historial del detalle; el error va resumido (el completo queda en la base). */
public record EntregaDTO(Long id, String origen, String destinatario, String estado, int intentos,
                         LocalDateTime fechaCreacion, LocalDateTime fechaEnvio, String ultimoError) {

    /** Largo máximo del error en el historial. */
    public static final int MAX_ERROR = 120;

    public static EntregaDTO de(EntregaComprobante e) {
        return new EntregaDTO(e.getId(), e.getOrigen().name(), e.getDestinatario(), e.getEstado().name(), e.getIntentos(),
            e.getFechaCreacion(), e.getFechaEnvio(), resumir(e.getUltimoError()));
    }

    static String resumir(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR) + "…";
    }
}
