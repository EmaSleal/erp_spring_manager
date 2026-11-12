package api.astro.whats_orders_manager.modules.notificacion.dto;

import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Preferencias de un tipo de notificación por canal, tal como las muestra y envía
 * preferencias-notificaciones.js (T-030, P-032). Nunca incluye el usuario: cada canal se guarda
 * como una fila (usuario, tipo, canal) de preferencia_notificacion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciaTipoDTO {

    /** Canales que la pantalla de preferencias permite configurar. */
    public static final List<CanalNotificacion> CANALES =
            List.of(CanalNotificacion.WEB, CanalNotificacion.EMAIL, CanalNotificacion.WHATSAPP);

    private TipoNotificacion tipo;
    private Boolean activoWeb;
    private Boolean activoEmail;
    private Boolean activoWhatsapp;

    public Boolean activoPara(CanalNotificacion canal) {
        return switch (canal) {
            case WEB -> activoWeb;
            case EMAIL -> activoEmail;
            case WHATSAPP -> activoWhatsapp;
            default -> throw new IllegalArgumentException("Canal no configurable: " + canal);
        };
    }
}
