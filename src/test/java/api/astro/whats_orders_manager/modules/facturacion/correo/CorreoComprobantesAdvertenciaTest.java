package api.astro.whats_orders_manager.modules.facturacion.correo;

import api.astro.whats_orders_manager.modules.facturacion.correo.client.ProveedorCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.config.CorreoComprobantesAdvertencia;
import api.astro.whats_orders_manager.modules.facturacion.correo.config.CorreoProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-055: al arrancar con el envío habilitado y sin clave o remitente, un WARN claro (sin la clave). */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("T-055 — advertencia de arranque del correo de comprobantes")
class CorreoComprobantesAdvertenciaTest {

    private static CorreoComprobantesAdvertencia advertencia(boolean habilitado, boolean configurado, String clave,
                                                             String remitente) {
        CorreoProperties p = new CorreoProperties();
        p.getComprobantes().setHabilitado(habilitado);
        p.getResend().setApiKey(clave);
        p.setRemitente(remitente);
        ProveedorCorreo proveedor = mock(ProveedorCorreo.class);
        when(proveedor.configurado()).thenReturn(configurado);
        return new CorreoComprobantesAdvertencia(p, proveedor);
    }

    @Test
    @DisplayName("habilitado y sin clave → WARN que nombra RESEND_API_KEY y dice que las entregas quedan PENDIENTE")
    void habilitadoSinClave(CapturedOutput salida) {
        advertencia(true, false, "", "Monrachem <no-reply@astro-solutions.net>").verificar();

        assertThat(salida.getAll()).contains("WARN").contains("RESEND_API_KEY").contains("PENDIENTE");
    }

    @Test
    @DisplayName("habilitado y sin remitente → WARN que nombra correo.remitente; nunca imprime la clave")
    void habilitadoSinRemitente(CapturedOutput salida) {
        advertencia(true, false, "re_valor_secreto", "").verificar();

        assertThat(salida.getAll()).contains("WARN").contains("correo.remitente").doesNotContain("re_valor_secreto");
    }

    @Test
    @DisplayName("habilitado y configurado, o deshabilitado → sin advertencia")
    void sinAdvertencia(CapturedOutput salida) {
        advertencia(true, true, "k", "r <r@x.co>").verificar();
        advertencia(false, false, "", "").verificar();

        assertThat(salida.getAll()).doesNotContain("RESEND_API_KEY");
    }
}
