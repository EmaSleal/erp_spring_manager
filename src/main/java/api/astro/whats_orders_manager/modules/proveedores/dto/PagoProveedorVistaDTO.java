package api.astro.whats_orders_manager.modules.proveedores.dto;

import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago a proveedor tal como lo devuelve la API JSON (T-052): sin entidades ni proxies perezosos. */
public record PagoProveedorVistaDTO(
        Long id,
        String numero,
        Long cuentaPorPagarId,
        String cuentaPorPagarNumero,
        LocalDate fecha,
        BigDecimal monto,
        String moneda,
        BigDecimal tipoCambio,
        BigDecimal montoAplicado,
        String metodoPago,
        String referencia,
        String cuentaBancaria,
        String estado) {

    /** Debe llamarse con la entidad adjunta a una sesión abierta (la CxP y la cuenta bancaria son perezosas). */
    public static PagoProveedorVistaDTO de(PagoProveedor p) {
        var cuenta = p.getCuentaPorPagar();
        return new PagoProveedorVistaDTO(
            p.getId(), p.getNumero(),
            cuenta != null ? cuenta.getId() : null,
            cuenta != null ? cuenta.getNumero() : null,
            p.getFecha(), p.getMonto(),
            p.getMoneda() != null ? p.getMoneda().name() : null,
            p.getTipoCambio(), p.getMontoAplicado(),
            p.getMetodoPago() != null ? p.getMetodoPago().name() : null,
            p.getReferencia(),
            p.getCuentaBancariaOrigen() != null ? p.getCuentaBancariaOrigen().getEntidad() : null,
            p.getEstado() != null ? p.getEstado().name() : null);
    }
}
