package api.astro.whats_orders_manager.modules.facturacion.util;

/** Tramos fijos de antigüedad de los saldos a favor (T-049, ADR-058 §6): 0–7, 8–30, 31–90, 91–180 y más de 180 días. */
public enum TramoAntiguedad {
    DE_0_A_7("0-7"), DE_8_A_30("8-30"), DE_31_A_90("31-90"), DE_91_A_180("91-180"), MAS_DE_180(">180");

    private final String etiqueta;

    TramoAntiguedad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Tramo de una antigüedad en días (>= 0). */
    public static TramoAntiguedad de(long dias) {
        if (dias <= 7) {
            return DE_0_A_7;
        }
        if (dias <= 30) {
            return DE_8_A_30;
        }
        if (dias <= 90) {
            return DE_31_A_90;
        }
        return dias <= 180 ? DE_91_A_180 : MAS_DE_180;
    }
}
