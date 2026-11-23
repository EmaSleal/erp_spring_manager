package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagarLinea;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TarifaIva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Cuenta por pagar tal como la devuelve la API JSON (T-052, P-023 §1): datos planos, sin entidades ni proxies
 * perezosos. Se arma dentro de la transacción de lectura del servicio.
 *
 * T-054: {@code lineas} reemplaza al desglose de IVA; {@code estadoAceptacion}, {@code fechaAceptacion} y
 * {@code numeroMensajeReceptor} son la aceptación del comprobante; {@code diasDesdeComprobante} son los días entre la fecha
 * del comprobante y hoy, solo para las CxP no anuladas con aceptación PENDIENTE (null en las demás).
 *
 * {@code fechaEmisionComprobante} es la fecha de emisión del comprobante del proveedor (null en las CxP anteriores a
 * V53, que no la guardaban); {@code fechaEmision} es la de la CxP, que en las CxP manuales nuevas es la misma (ADR-014).
 */
public record CuentaPorPagarVistaDTO(
        Long id,
        String numero,
        Long proveedorId,
        String proveedorNombre,
        Long ordenCompraId,
        String ordenCompraNumero,
        String descripcion,
        BigDecimal monto,
        BigDecimal saldoPendiente,
        BigDecimal subtotal,
        BigDecimal impuesto,
        String moneda,
        BigDecimal tipoCambio,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String estado,
        String numeroComprobanteProveedor,
        String claveComprobante,
        LocalDate fechaEmisionComprobante,
        List<Linea> lineas,
        String estadoAceptacion,
        LocalDate fechaAceptacion,
        String numeroMensajeReceptor,
        Long diasDesdeComprobante,
        Integer anuladaPorId,
        LocalDateTime fechaAnulacion,
        String motivoAnulacion,
        String notas) {

    /**
     * Línea de la CxP: cuenta (código y nombre), tarifa ({@code codigoTarifa} y {@code porcentaje} son null cuando es «sin
     * tarifa»), base, IVA, tratamiento y el IVA acreditable y no acreditable guardados.
     */
    public record Linea(Integer numeroLinea, String descripcion, Long cuentaId, String cuentaCodigo, String cuentaNombre,
                        String codigoTarifa, BigDecimal porcentaje, BigDecimal base, BigDecimal iva, String tratamiento,
                        BigDecimal tarifaLimite, BigDecimal porcentajeProrrata, BigDecimal ivaAcreditable,
                        BigDecimal ivaNoAcreditable) {

        static Linea de(CuentaPorPagarLinea l) {
            BigDecimal porcentaje = TarifaIva.deCodigo(l.getCodigoTarifa()).map(TarifaIva::getPorcentaje).orElse(null);
            var cuenta = l.getCuentaContable();
            return new Linea(l.getNumeroLinea(), l.getDescripcion(),
                cuenta != null ? cuenta.getIdCuenta() : null, cuenta != null ? cuenta.getCodigo() : null,
                cuenta != null ? cuenta.getNombre() : null, l.getCodigoTarifa(), porcentaje, l.getBase(), l.getIva(),
                l.getTratamiento() != null ? l.getTratamiento().name() : null, l.getTarifaLimite(),
                l.getPorcentajeProrrata(), l.getIvaAcreditable(), l.getIvaNoAcreditable());
        }
    }

    /** Debe llamarse con la entidad adjunta a una sesión abierta (proveedor, OC y líneas son perezosos). */
    public static CuentaPorPagarVistaDTO de(CuentaPorPagar c, LocalDate hoy) {
        var proveedor = c.getProveedor();
        var oc = c.getOrdenCompra();
        var aceptacion = c.getEstadoAceptacion();
        boolean pendiente = (aceptacion == null || aceptacion == EstadoAceptacionComprobante.PENDIENTE)
            && c.getEstado() != EstadoCuentaPorPagar.ANULADA && c.getFechaComprobante() != null && hoy != null;
        Long dias = pendiente ? ChronoUnit.DAYS.between(c.getFechaComprobante(), hoy) : null;
        return new CuentaPorPagarVistaDTO(
            c.getId(), c.getNumero(),
            proveedor != null ? proveedor.getId() : null,
            proveedor != null ? proveedor.getNombre() : null,
            oc != null ? oc.getId() : null,
            oc != null ? oc.getNumero() : null,
            c.getDescripcion(), c.getMonto(), c.getSaldoPendiente(), c.getSubtotal(), c.getImpuesto(),
            c.getMoneda() != null ? c.getMoneda().name() : "CRC",
            c.getTipoCambio(), c.getFechaEmision(), c.getFechaVencimiento(),
            c.getEstado() != null ? c.getEstado().name() : null,
            c.getNumeroComprobanteProveedor(), c.getClaveComprobante(), c.getFechaComprobante(),
            c.getLineas().stream().map(Linea::de).toList(),
            aceptacion != null ? aceptacion.name() : EstadoAceptacionComprobante.PENDIENTE.name(),
            c.getFechaAceptacion(), c.getNumeroMensajeReceptor(), dias,
            c.getAnuladaPorId(), c.getFechaAnulacion(), c.getMotivoAnulacion(), c.getNotas());
    }
}
