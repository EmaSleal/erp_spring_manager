package api.astro.whats_orders_manager.modules.nomina.enums;

/**
 * State machine for Nomina lifecycle.
 *
 * Valid forward chain: BORRADOR → CALCULADA → APROBADA → CONTABILIZADA
 * Cancellation: BORRADOR | CALCULADA → ANULADA (APROBADA and later are terminal)
 */
public enum EstadoNomina {

    BORRADOR,
    CALCULADA,
    APROBADA,
    CONTABILIZADA,
    ANULADA;

    /** True only when in BORRADOR — the payroll can receive calculation. */
    public boolean puedeCalcular() {
        return this == BORRADOR;
    }

    /** True only when in CALCULADA — the payroll can be approved. */
    public boolean puedeAprobar() {
        return this == CALCULADA;
    }

    /** True only when in APROBADA — the payroll can be posted to accounting. */
    public boolean puedeContabilizar() {
        return this == APROBADA;
    }

    /** True when in BORRADOR or CALCULADA — the payroll can be cancelled. */
    public boolean puedeAnular() {
        return this == BORRADOR || this == CALCULADA;
    }
}
