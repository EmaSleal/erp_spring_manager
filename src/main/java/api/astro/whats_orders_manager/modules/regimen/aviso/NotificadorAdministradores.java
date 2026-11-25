package api.astro.whats_orders_manager.modules.regimen.aviso;

import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Envía una notificación web (tipo SISTEMA) a cada usuario ADMIN activo y no bloqueado (T-056: aviso de que Hacienda
 * difiere y de que el tope del RTS se acerca o se cruzó). Un fallo con un destinatario se registra y no impide avisar a los
 * demás ni se propaga: un aviso nunca debe romper la operación que lo originó.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificadorAdministradores {

    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    /** Aviso corriente: respeta la preferencia de notificación SISTEMA/WEB de cada administrador (filtro de T-030). */
    public void notificar(String titulo, String mensaje) {
        enviar(titulo, mensaje, false);
    }

    /**
     * Aviso crítico (Hacienda difiere del ERP, tope del RTS al 100 %): se envía aunque el administrador tenga inactiva la
     * preferencia SISTEMA/WEB. La excepción vive aquí; el filtro general de T-030 no cambia.
     */
    public void notificarCritico(String titulo, String mensaje) {
        enviar(titulo, mensaje, true);
    }

    private void enviar(String titulo, String mensaje, boolean critico) {
        for (Usuario admin : usuarioService.findByRol("ADMIN")) {
            if (!Boolean.TRUE.equals(admin.getActivo()) || Boolean.TRUE.equals(admin.getBloqueado())) {
                continue;
            }
            try {
                if (critico) {
                    notificacionService.enviarNotificacionSinPreferencias(admin, TipoNotificacion.SISTEMA, CanalNotificacion.WEB, titulo, mensaje);
                } else {
                    notificacionService.enviarNotificacion(admin, TipoNotificacion.SISTEMA, CanalNotificacion.WEB, titulo, mensaje);
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo notificar a un administrador: {}", e.getMessage());
            }
        }
    }
}
