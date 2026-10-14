package api.astro.whats_orders_manager.modules.contabilidad.service.impl;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaPorPagarServiceImpl implements CuentaPorPagarService {

    private final CuentaPorPagarRepository cuentaRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final AsientoContableService asientoService;
    private final TipoCambioService tipoCambioService;

    /**
     * Crea una CxP y genera su asiento de compra en la misma transacción (ADR-015).
     *
     * Con ordenCompraId: mismas reglas que {@link #crearDesdeOrdenCompra(Long)}. El desglose, la moneda y
     * el tipo de cambio salen de la OC (se ignoran subtotal, impuesto, cuentaGastoId y tipoCambio); se
     * rechaza si la OC no está recibida, si ya tiene CxP, si el monto indicado difiere de oc.total o si
     * la moneda indicada difiere de la de la OC.
     * Sin ordenCompraId (CxP manual, ADR-016): exige subtotal &gt;= 0, impuesto &gt;= 0,
     * subtotal + impuesto = monto (monto null → la suma) y una contrapartida válida
     * ({@link AsientoContableService#validarContrapartidaCxpManual(Long)}).
     *
     * Si la validación o el asiento fallan no queda guardada la CxP ni el cambio de saldo del proveedor.
     */
    @Override
    @Transactional
    public CuentaPorPagar crear(CuentaPorPagarDTO dto) {
        if (dto.getProveedorId() == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio");
        }
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
            .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado: " + dto.getProveedorId()));

        OrdenCompra oc = null;
        MonedaOc monedaOc = null;
        BigDecimal monto;
        BigDecimal subtotal = null;
        BigDecimal impuesto = null;
        CuentaContable cuentaGasto = null;
        String descripcion = dto.getDescripcion();
        if (dto.getOrdenCompraId() != null) {
            oc = ordenCompraRepository.findById(dto.getOrdenCompraId())
                .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + dto.getOrdenCompraId()));
            validarOcParaCxp(oc);
            monedaOc = monedaYTipoCambioDeOc(oc);
            if (dto.getMoneda() != null && dto.getMoneda() != monedaOc.moneda()) {
                throw new IllegalArgumentException(String.format(
                    "La moneda indicada (%s) no coincide con la moneda de la OC %s (%s)",
                    dto.getMoneda().getCodigo(), oc.getNumero(), monedaOc.moneda().getCodigo()));
            }
            if (dto.getMonto() != null && dto.getMonto().compareTo(oc.getTotal()) != 0) {
                throw new IllegalArgumentException(String.format(
                    "El monto %s no coincide con el total de la OC %s (%s)", dto.getMonto(), oc.getNumero(), oc.getTotal()));
            }
            monto = oc.getTotal();
            if (descripcion == null || descripcion.isBlank()) {
                descripcion = "Compra según OC " + oc.getNumero();
            }
        } else {
            subtotal = dto.getSubtotal();
            impuesto = dto.getImpuesto();
            if (subtotal == null || impuesto == null) {
                throw new IllegalArgumentException("La cuenta por pagar manual requiere subtotal e impuesto");
            }
            if (subtotal.signum() < 0 || impuesto.signum() < 0) {
                throw new IllegalArgumentException("El subtotal y el impuesto no pueden ser negativos");
            }
            BigDecimal suma = subtotal.add(impuesto);
            monto = dto.getMonto() != null ? dto.getMonto() : suma;
            if (monto.compareTo(suma) != 0) {
                throw new IllegalArgumentException(String.format(
                    "Subtotal + impuesto (%s) no coincide con el monto (%s)", suma, monto));
            }
            if (monto.signum() <= 0) {
                throw new IllegalArgumentException("El monto de la cuenta por pagar debe ser mayor a 0");
            }
            if (descripcion == null || descripcion.isBlank()) {
                throw new IllegalArgumentException("La descripción es obligatoria");
            }
            cuentaGasto = asientoService.validarContrapartidaCxpManual(dto.getCuentaGastoId());
        }

        // ADR-011: tipo de cambio propio; con OC, moneda y tipo de cambio salen de la orden (ADR-015)
        LocalDate fechaEmision = dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDate.now();
        MonedaFE moneda;
        BigDecimal tipoCambio;
        if (monedaOc != null) {
            moneda = monedaOc.moneda();
            tipoCambio = monedaOc.tipoCambio();
        } else {
            moneda = dto.getMoneda() != null ? dto.getMoneda() : MonedaFE.CRC;
            tipoCambio = tipoCambioService.resolverTipoCambioOperacion(
                moneda, moneda, dto.getTipoCambio(), fechaEmision);
        }

        CuentaPorPagar cuenta = CuentaPorPagar.builder()
            .numero(generarNumero())
            .proveedor(proveedor)
            .ordenCompra(oc)
            .descripcion(descripcion)
            .monto(monto)
            .saldoPendiente(monto)
            .subtotal(subtotal)
            .impuesto(impuesto)
            .cuentaGasto(cuentaGasto)
            .moneda(moneda)
            .tipoCambio(tipoCambio)
            .fechaEmision(fechaEmision)
            .fechaVencimiento(dto.getFechaVencimiento())
            .notas(dto.getNotas())
            .build();

        CuentaPorPagar guardada = cuentaRepository.save(cuenta);
        // ADR-015: el asiento va en la misma transacción; si falla, se revierte también la CxP.
        // El saldo del proveedor se toca después, para no dejarlo modificado en memoria si el asiento falla.
        asientoService.generarAsientoCompra(guardada);
        actualizarSaldoProveedor(proveedor, monto, true);
        log.info("Cuenta por pagar {} creada para proveedor {}", guardada.getNumero(), proveedor.getNombre());
        return guardada;
    }

    @Override
    @Transactional
    public CuentaPorPagar crearDesdeOrdenCompra(Long ordenCompraId) {
        OrdenCompra oc = ordenCompraRepository.findById(ordenCompraId)
            .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + ordenCompraId));

        validarOcParaCxp(oc);
        MonedaOc monedaOc = monedaYTipoCambioDeOc(oc);

        CuentaPorPagar cuenta = CuentaPorPagar.builder()
            .numero(generarNumero())
            .proveedor(oc.getProveedor())
            .ordenCompra(oc)
            .descripcion("Compra según OC " + oc.getNumero())
            .monto(oc.getTotal())
            .saldoPendiente(oc.getTotal())
            .moneda(monedaOc.moneda())
            .tipoCambio(monedaOc.tipoCambio())
            .fechaEmision(LocalDate.now())
            .fechaVencimiento(oc.getProveedor().getDiasCredito() != null && oc.getProveedor().getDiasCredito() > 0
                ? LocalDate.now().plusDays(oc.getProveedor().getDiasCredito())
                : null)
            .build();

        CuentaPorPagar guardada = cuentaRepository.save(cuenta);
        actualizarSaldoProveedor(oc.getProveedor(), oc.getTotal(), true);
        asientoService.generarAsientoCompra(guardada);
        log.info("CPP {} generada desde OC {}", guardada.getNumero(), oc.getNumero());
        return guardada;
    }

    /**
     * Reglas comunes para crear una CxP desde una OC, por cualquiera de las dos vías (ADR-015):
     * la OC debe estar recibida (total o parcialmente) y no tener ya una CxP.
     */
    private void validarOcParaCxp(OrdenCompra oc) {
        if (oc.getEstado() != EstadoOrdenCompra.RECIBIDA && oc.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
            throw new IllegalStateException("Solo se puede generar CPP de una OC recibida");
        }
        if (cuentaRepository.existsByOrdenCompra(oc)) {
            throw new IllegalStateException("Ya existe una cuenta por pagar para la OC " + oc.getNumero());
        }
    }

    /** Moneda y tipo de cambio que hereda de la OC una CxP creada desde ella (ADR-011). */
    private MonedaOc monedaYTipoCambioDeOc(OrdenCompra oc) {
        MonedaFE moneda = oc.getMoneda() != null ? oc.getMoneda() : MonedaFE.CRC;
        BigDecimal tipoCambio = tipoCambioService.resolverTipoCambioOperacion(
            moneda, moneda, oc.getTipoCambio(), oc.getFecha());
        return new MonedaOc(moneda, tipoCambio);
    }

    private record MonedaOc(MonedaFE moneda, BigDecimal tipoCambio) {}

    @Override
    @Transactional(readOnly = true)
    public CuentaPorPagar findById(Long id) {
        return cuentaRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Cuenta por pagar no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagar> findAll() {
        return cuentaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagar> findByEstado(EstadoCuentaPorPagar estado) {
        return cuentaRepository.findByEstadoOrderByFechaVencimientoAsc(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagar> findVencidas() {
        return cuentaRepository.findVencidas(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findOrdenCompraIdsConCpp() {
        return new HashSet<>(cuentaRepository.findOrdenCompraIds());
    }

    @Override
    @Transactional
    public int actualizarVencidas() {
        List<CuentaPorPagar> vencidas = cuentaRepository.findVencidas(LocalDate.now());
        vencidas.forEach(c -> c.setEstado(EstadoCuentaPorPagar.VENCIDA));
        cuentaRepository.saveAll(vencidas);
        if (!vencidas.isEmpty()) {
            log.info("Marcadas {} cuentas por pagar como VENCIDA", vencidas.size());
        }
        return vencidas.size();
    }

    @Override
    @Transactional
    public void aplicarPago(CuentaPorPagar cuenta, BigDecimal monto) {
        cuenta.setSaldoPendiente(cuenta.getSaldoPendiente().subtract(monto));
        cuenta.setEstado(cuenta.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0
            ? EstadoCuentaPorPagar.PAGADA
            : EstadoCuentaPorPagar.PARCIAL);
        cuentaRepository.save(cuenta);
        actualizarSaldoProveedor(cuenta.getProveedor(), monto, false);
    }

    @Override
    @Transactional
    public void revertirPago(CuentaPorPagar cuenta, BigDecimal monto) {
        cuenta.setSaldoPendiente(cuenta.getSaldoPendiente().add(monto));
        cuenta.setEstado(cuenta.getSaldoPendiente().compareTo(cuenta.getMonto()) == 0
            ? EstadoCuentaPorPagar.PENDIENTE
            : EstadoCuentaPorPagar.PARCIAL);
        cuentaRepository.save(cuenta);
        actualizarSaldoProveedor(cuenta.getProveedor(), monto, true);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void actualizarSaldoProveedor(Proveedor proveedor, BigDecimal monto, boolean aumentar) {
        BigDecimal actual = proveedor.getSaldoPendiente() != null
            ? proveedor.getSaldoPendiente() : BigDecimal.ZERO;
        proveedor.setSaldoPendiente(aumentar ? actual.add(monto) : actual.subtract(monto));
        proveedorRepository.save(proveedor);
    }

    private synchronized String generarNumero() {
        int anio = LocalDateTime.now().getYear();
        Integer ultimo = cuentaRepository.findUltimoConsecutivo(anio);
        int siguiente = (ultimo == null ? 0 : ultimo) + 1;
        return String.format("CPP-%d-%05d", anio, siguiente);
    }
}
