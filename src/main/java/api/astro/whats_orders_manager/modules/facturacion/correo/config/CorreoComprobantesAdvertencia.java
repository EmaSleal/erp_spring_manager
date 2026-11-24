package api.astro.whats_orders_manager.modules.facturacion.correo.config;

import api.astro.whats_orders_manager.modules.facturacion.correo.client.ProveedorCorreo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Al arrancar con el envío de comprobantes habilitado, avisa con un WARN claro si falta la clave o el remitente: las
 * entregas quedan PENDIENTE sin consumir intentos. Nombra lo que falta, nunca el valor de la clave.
 */
@Component
@Slf4j
public class CorreoComprobantesAdvertencia {

    private final CorreoProperties props;
    private final ProveedorCorreo proveedor;

    public CorreoComprobantesAdvertencia(CorreoProperties props, ProveedorCorreo proveedor) {
        this.props = props;
        this.proveedor = proveedor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verificar() {
        if (!props.habilitado() || proveedor.configurado()) {
            return;
        }
        List<String> faltan = new ArrayList<>();
        if (props.getResend().getApiKey() == null || props.getResend().getApiKey().isBlank()) {
            faltan.add("la variable de entorno RESEND_API_KEY");
        }
        if (props.getRemitente() == null || props.getRemitente().isBlank()) {
            faltan.add("la propiedad correo.remitente");
        }
        if (faltan.isEmpty()) {
            faltan.add("la configuración del proveedor de correo");
        }
        log.warn("El envío de comprobantes por correo está habilitado (correo.comprobantes.habilitado=true) pero falta {}: "
            + "las entregas quedan PENDIENTE y no se consumen intentos hasta que se configure.", String.join(" y ", faltan));
    }
}
