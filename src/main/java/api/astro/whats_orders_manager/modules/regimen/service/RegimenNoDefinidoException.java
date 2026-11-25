package api.astro.whats_orders_manager.modules.regimen.service;

import java.time.LocalDate;

/**
 * No hay ningún tramo de régimen que cubra la fecha pedida (ADR-072: no existe un régimen por defecto). Quien la reciba
 * no debe inventar uno: el documento no se puede clasificar hasta que se corrija la tabla de tramos.
 */
public class RegimenNoDefinidoException extends IllegalStateException {

    private final LocalDate fecha;

    public RegimenNoDefinidoException(LocalDate fecha) {
        super("No hay un régimen tributario definido para la fecha " + fecha);
        this.fecha = fecha;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
