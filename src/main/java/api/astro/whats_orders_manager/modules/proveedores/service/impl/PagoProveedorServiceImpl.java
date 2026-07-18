package api.astro.whats_orders_manager.modules.proveedores.service.impl;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
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

    @Override
    @Transactional
    public PagoProveedor registrarPago(PagoProveedorDTO dto, Usuario usuario) {
        CuentaPorPagar cuenta = cuentaRepository.findById(dto.getCuentaPorPagarId())
            .orElseThrow(() -> new NoSuchElementException("Cuenta por pagar no encontrada: " + dto.getCuentaPorPagarId()));

        if (cuenta.getEstado() == EstadoCuentaPorPagar.PAGADA
            || cuenta.getEstado() == EstadoCuentaPorPagar.ANULADA) {
            throw new IllegalStateException("La cuenta por pagar ya está " + cuenta.getEstado());
        }

        if (dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a 0");
        }

        if (dto.getMonto().compareTo(cuenta.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException(
                "El monto del pago (" + dto.getMonto() + ") supera el saldo pendiente (" + cuenta.getSaldoPendiente() + ")");
        }

        PagoProveedor pago = PagoProveedor.builder()
            .numero(generarNumero())
            .cuentaPorPagar(cuenta)
            .monto(dto.getMonto())
            .metodoPago(dto.getMetodoPago())
            .referencia(dto.getReferencia())
            .fecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now())
            .notas(dto.getNotas())
            .registradoPor(usuario)
            .estado(EstadoPagoProveedor.CONFIRMADO)
            .build();

        PagoProveedor guardado = pagoRepository.save(pago);
        cuentaService.aplicarPago(cuenta, dto.getMonto());
        asientoService.generarAsientoPagoProveedor(guardado);

        log.info("Pago {} de {} registrado para CPP {} por {}",
            guardado.getNumero(), dto.getMonto(), cuenta.getNumero(), usuario.getNombre());
        return guardado;
    }

    @Override
    @Transactional
    public void anularPago(Long pagoId, Usuario usuario) {
        // TODO: reversing entry on anularPago — out of scope, see proposal
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
        cuentaService.revertirPago(cuenta, pago.getMonto());

        log.info("Pago {} anulado por {}", pago.getNumero(), usuario.getNombre());
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
