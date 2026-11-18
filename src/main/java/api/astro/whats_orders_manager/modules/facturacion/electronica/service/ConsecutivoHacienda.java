package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;

/**
 * Toma el siguiente consecutivo de un tipo de comprobante (P-089, ADR-043 §1a).
 *
 * <p>La implementación productiva ({@code ConsecutivoHaciendaJpa}) bloquea la fila de configuracion_hacienda con
 * {@code SELECT ... FOR UPDATE} dentro de la transacción del llamador, lee el valor vigente de la BD y deja el
 * incremento pendiente de esa misma transacción. Dos emisiones solapadas se serializan en la BD y nunca toman el mismo
 * número. Debe llamarse dentro de una transacción (la etapa de preparación de la emisión).
 */
@FunctionalInterface
public interface ConsecutivoHacienda {

    /**
     * @param config configuración de Hacienda activa (la de la empresa que emite)
     * @param codigoTipo código de Hacienda del tipo de comprobante (01, 03, 04...)
     * @return el consecutivo que corresponde a este comprobante
     */
    Long siguiente(ConfiguracionHacienda config, String codigoTipo);
}
