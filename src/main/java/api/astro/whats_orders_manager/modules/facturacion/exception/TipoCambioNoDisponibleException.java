package api.astro.whats_orders_manager.modules.facturacion.exception;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;

import java.time.LocalDate;

/**
 * Excepción de negocio: no hay un tipo de cambio de venta activo registrado para la
 * moneda en (o antes de) la fecha consultada (ADR-011).
 *
 * Extiende {@link IllegalArgumentException} para que los controladores existentes, que ya
 * devuelven el mensaje de las validaciones de entrada, muestren el mensaje al usuario.
 */
public class TipoCambioNoDisponibleException extends IllegalArgumentException {

    private final MonedaFE moneda;
    private final LocalDate fecha;

    public TipoCambioNoDisponibleException(MonedaFE moneda, LocalDate fecha) {
        super(String.format("No hay tipo de cambio de %s para %s; regístrelo", moneda.getCodigo(), fecha));
        this.moneda = moneda;
        this.fecha = fecha;
    }

    public MonedaFE getMoneda() {
        return moneda;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
