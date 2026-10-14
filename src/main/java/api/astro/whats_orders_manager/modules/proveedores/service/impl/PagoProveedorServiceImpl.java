package api.astro.whats_orders_manager.modules.proveedores.service.impl;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.facturacion.util.ConversionMoneda;
import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.PagoProveedorService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoProveedorServiceImpl implements PagoProveedorService {

    private final PagoProveedorRepository pagoRepository;
    private final CuentaPorPagarRepository cuentaRepository;
    private final CuentaPorPagarService cuentaService;
    private final AsientoContableService asientoService;
    private final TipoCambioService tipoCambioService;

    @Override
    @Transactional
    public PagoProveedor registrarPago(PagoProveedorDTO dto, Usuario usuario) {
        CuentaPorPagar cuenta = cuentaRepository.findById(dto.getCuentaPorPagarId())
            .orElseThrow(() -> new NoSuchElementException("Cuenta por pagar no encontrada: " + dto.getCuentaPorPagarId()));

        if (cuenta.getEstado() == EstadoCuentaPorPagar.PAGADA
            || cuenta.getEstado() == EstadoCuentaPorPagar.ANULADA) {
            throw new IllegalStateException("La cuenta por pagar ya está " + cuenta.getEstado());
        }

        if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a 0");
        }

        // ADR-011: moneda y tipo de cambio propios; montoAplicado en la moneda de la CxP
        LocalDate fecha = dto.getFecha() != null ? dto.getFecha() : LocalDate.now();
        MonedaFE monedaCuenta = ConversionMoneda.oCrc(cuenta.getMoneda());
        MonedaFE monedaPago = dto.getMoneda() != null ? dto.getMoneda() : monedaCuenta;
        BigDecimal tipoCambio = tipoCambioService.resolverTipoCambioOperacion(
            monedaPago, monedaCuenta, dto.getTipoCambio(), fecha);
        BigDecimal montoAplicado = ConversionMoneda.montoAplicado(dto.getMonto(), monedaPago, monedaCuenta, tipoCambio);

        if (montoAplicado.compareTo(cuenta.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException(
                "El monto del pago (" + montoAplicado + " " + monedaCuenta.getCodigo()
                    + ") supera el saldo pendiente (" + cuenta.getSaldoPendiente() + " " + monedaCuenta.getCodigo() + ")");
        }

        PagoProveedor pago = PagoProveedor.builder()
            .numero(generarNumero())
            .cuentaPorPagar(cuenta)
            .monto(dto.getMonto())
            .moneda(monedaPago)
            .tipoCambio(tipoCambio)
            .montoAplicado(montoAplicado)
            .metodoPago(dto.getMetodoPago())
            .referencia(dto.getReferencia())
            .fecha(fecha)
            .notas(dto.getNotas())
            .registradoPor(usuario)
            .estado(EstadoPagoProveedor.CONFIRMADO)
            .build();

        PagoProveedor guardado = pagoRepository.save(pago);
        cuentaService.aplicarPago(cuenta, montoAplicado);
        asientoService.generarAsientoPagoProveedor(guardado);

        log.info("Pago {} de {} {} registrado para CPP {} por {}",
            guardado.getNumero(), dto.getMonto(), monedaPago, cuenta.getNumero(), usuario.getNombre());
        return guardado;
    }

    @Override
    @Transactional
    public void anularPago(Long pagoId, Usuario usuario) {
        PagoProveedor pago = findById(pagoId);

        if (pago.getEstado() == EstadoPagoProveedor.ANULADO) {
            throw new IllegalStateException("El pago ya está anulado");
        }

        CuentaPorPagar cuenta = pago.getCuentaPorPagar();
        if (cuenta.getEstado() == EstadoCuentaPorPagar.ANULADA) {
            throw new IllegalStateException("La cuenta por pagar está anulada");
        }

        pago.setEstado(EstadoPagoProveedor.ANULADO);
        pagoRepository.save(pago);
        // ADR-011: se restituye el saldo en la moneda de la CxP (montoAplicado)
        cuentaService.revertirPago(cuenta, pago.getMontoAplicado() != null ? pago.getMontoAplicado() : pago.getMonto());
        // ADR-008 regla 4: el asiento del pago no se edita ni se borra; se revierte con líneas espejo
        asientoService.revertirAsientoPagoProveedor(pago,
            "Anulación de pago a proveedor " + pago.getNumero()
                + (usuario != null && usuario.getNombre() != null ? " por " + usuario.getNombre() : ""));

        log.info("Pago {} anulado por {}", pago.getNumero(), usuario != null ? usuario.getNombre() : "N/A");
    }

    @Override
    @Transactional(readOnly = true)
    public PagoProveedor findById(Long id) {
        return pagoRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Pago no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoProveedor> findByCuenta(Long cuentaId) {
        CuentaPorPagar cuenta = cuentaRepository.findById(cuentaId)
            .orElseThrow(() -> new NoSuchElementException("Cuenta por pagar no encontrada: " + cuentaId));
        return pagoRepository.findByCuentaPorPagarOrderByFechaDesc(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoProveedor> findByPeriodo(LocalDate desde, LocalDate hasta) {
        return pagoRepository.findByFechaBetweenOrderByFechaDesc(desde, hasta);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private synchronized String generarNumero() {
        int anio = LocalDateTime.now().getYear();
        Integer ultimo = pagoRepository.findUltimoConsecutivo(anio);
        int siguiente = (ultimo == null ? 0 : ultimo) + 1;
        return String.format("PP-%d-%05d", anio, siguiente);
    }
}
