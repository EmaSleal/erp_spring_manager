package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.AmbienteDistintoException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

/**
 * T-044 (P-099): la regla única de ambiente. Un comprobante solo se transmite, se consulta o se referencia en el
 * ambiente en que se emitió (comprobante_electronico.ambiente, V43); el ambiente «activo» es el de la configuración de
 * Hacienda activa, leída con la lectura aislada de T-041 ({@link LecturaConfiguracionActiva}).
 *
 * <ul>
 *   <li>Si la configuración activa no se puede leer ({@link #ambienteActivo()} vacío), la regla no decide: sigue el
 *       comportamiento de T-041 (ERROR de configuración al enviar, mensaje fijo al consultar), sin mensajes nuevos.</li>
 *   <li>Un comprobante sin ambiente (null) no coincide con ninguno: V43 completó todas las filas, así que null no
 *       debería existir; si apareciera, no se transmite ni se consulta (lado seguro) y el mensaje lo nombra
 *       SIN_AMBIENTE.</li>
 * </ul>
 */
@Component
@Slf4j
public class AmbienteComprobanteGuard {

    private final ConfiguracionHaciendaService configuracionService;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    public AmbienteComprobanteGuard(ConfiguracionHaciendaService configuracionService,
                                    PlatformTransactionManager transactionManager) {
        this.configuracionService = configuracionService;
        this.transactionManager = transactionManager;
    }

    /** Ambiente de la configuración activa; vacío si no se puede leer (ninguna, varias o credenciales ilegibles). */
    public Optional<AmbienteHacienda> ambienteActivo() {
        try {
            AmbienteHacienda ambiente = LecturaConfiguracionActiva.leer(configuracionService, transactionManager, null)
                .getAmbiente();
            return Optional.ofNullable(ambiente);
        } catch (ConfiguracionHaciendaNoDisponibleException e) {
            log.warn("Ambiente activo no disponible, la regla de ambiente no decide: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** true si el comprobante es del ambiente activo. Sin configuración legible, no hay regla que lo contradiga. */
    public boolean coincide(ComprobanteElectronico comprobante) {
        return coincide(comprobante.getAmbiente(), ambienteActivo());
    }

    /** Variante para quien ya tiene el ambiente activo (listados): vacío = sin regla = coincide. */
    public static boolean coincide(AmbienteHacienda ambienteComprobante, Optional<AmbienteHacienda> activo) {
        return activo.isEmpty() || ambienteComprobante == activo.get();
    }

    /**
     * Rechaza el envío o la consulta de un comprobante de otro ambiente, sin tocar su estado.
     *
     * @throws AmbienteDistintoException si el ambiente del comprobante no es el de la configuración activa
     */
    public void exigirMismoAmbiente(ComprobanteElectronico comprobante) {
        ambienteActivo().ifPresent(activo -> {
            if (comprobante.getAmbiente() != activo) {
                log.warn("Comprobante {} ({}): ambiente {} distinto del activo {}; no se envía ni se consulta",
                    comprobante.getId(), comprobante.getClaveNumerica(), comprobante.getAmbiente(), activo);
                throw new AmbienteDistintoException(comprobante.getAmbiente(), activo);
            }
        });
    }

    /** Motivo fijo para la UI y los listados («El comprobante pertenece al ambiente X…»), o null si coincide. */
    public static String motivo(AmbienteHacienda ambienteComprobante, Optional<AmbienteHacienda> activo) {
        return activo.isPresent() && ambienteComprobante != activo.get()
            ? AmbienteDistintoException.mensaje(ambienteComprobante, activo.get()) : null;
    }

    /** «La factura se emitió en el ambiente X; la configuración activa es Y», para las referencias (nota, sustituta). */
    public static String mensajeReferencia(AmbienteHacienda ambienteFactura, AmbienteHacienda ambienteActivo) {
        return "La factura se emitió en el ambiente " + AmbienteDistintoException.nombre(ambienteFactura)
            + "; la configuración activa es " + AmbienteDistintoException.nombre(ambienteActivo);
    }

    /**
     * Motivo de referencia si el comprobante de la factura (el ACEPTADO) no es del ambiente activo; vacío si coincide o
     * si no hay configuración legible (no se inventa un mensaje: sigue T-041).
     */
    public Optional<String> motivoReferencia(ComprobanteElectronico comprobanteDeLaFactura) {
        return ambienteActivo()
            .filter(activo -> comprobanteDeLaFactura.getAmbiente() != activo)
            .map(activo -> mensajeReferencia(comprobanteDeLaFactura.getAmbiente(), activo));
    }
}
