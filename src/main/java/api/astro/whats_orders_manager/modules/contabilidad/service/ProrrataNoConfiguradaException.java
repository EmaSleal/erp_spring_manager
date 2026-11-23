package api.astro.whats_orders_manager.modules.contabilidad.service;

/**
 * No hay porcentaje provisional de prorrata para el año del comprobante y la CxP tiene líneas de uso común (T-054,
 * ADR-067 §4). Se responde 409 con el mensaje fijo «No hay porcentaje de prorrata configurado para &lt;año&gt;».
 */
public class ProrrataNoConfiguradaException extends IllegalStateException {

    public ProrrataNoConfiguradaException(int anio) {
        super("No hay porcentaje de prorrata configurado para " + anio);
    }
}
