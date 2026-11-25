package api.astro.whats_orders_manager.modules.regimen.panel;

import api.astro.whats_orders_manager.modules.regimen.tope.EstadoTope;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Arma el bloque de régimen de /facturas/inicio (T-056): el aviso de la verificación con Hacienda (DIFIERE, o SIN_RESPUESTA
 * repetida) y el estado del tope de compras del RTS del año actual. Nunca lanza: un fallo deja ese dato en null y el panel
 * se muestra igual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegimenPanelService {

    private final VerificacionRegimenService verificacionService;
    private final TopeComprasRtsService topeService;
    private final Clock clock;

    public RegimenPanel obtener() {
        String aviso = null;
        try {
            aviso = verificacionService.avisoPanel().orElse(null);
        } catch (RuntimeException e) {
            log.warn("Panel de régimen: no se pudo leer el aviso de verificación ({})", e.getClass().getSimpleName());
        }
        EstadoTope tope = null;
        try {
            tope = topeService.estado(LocalDate.now(clock).getYear());
        } catch (RuntimeException e) {
            log.warn("Panel de régimen: no se pudo calcular el tope de compras ({})", e.getClass().getSimpleName());
        }
        return new RegimenPanel(aviso, tope, TopeComprasRtsService.ADVERTENCIA_SOLO_ERP);
    }
}
