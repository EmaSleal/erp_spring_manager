package api.astro.whats_orders_manager.modules.contabilidad.service.impl;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
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

    @Override
    @Transactional
    public CuentaPorPagar crear(CuentaPorPagarDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
            .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado: " + dto.getProveedorId()));

        OrdenCompra oc = null;
        if (dto.getOrdenCompraId() != null) {
            oc = ordenCompraRepository.findById(dto.getOrdenCompraId())
                .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + dto.getOrdenCompraId()));
        }

        CuentaPorPagar cuenta = CuentaPorPagar.builder()
            .numero(generarNumero())
            .proveedor(proveedor)
            .ordenCompra(oc)
            .descripcion(dto.getDescripcion())
            .monto(dto.getMonto())
            .saldoPendiente(dto.getMonto())
            .moneda(dto.getMoneda())
            .fechaEmision(dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDate.now())
            .fechaVencimiento(dto.getFechaVencimiento())
            .notas(dto.getNotas())
            .build();

        CuentaPorPagar guardada = cuentaRepository.save(cuenta);
        actualizarSaldoProveedor(proveedor, dto.getMonto(), true);
        log.info("Cuenta por pagar {} creada para proveedor {}", guardada.getNumero(), proveedor.getNombre());
        return guardada;
    }

    @Override
    @Transactional
    public CuentaPorPagar crearDesdeOrdenCompra(Long ordenCompraId) {
        OrdenCompra oc = ordenCompraRepository.findById(ordenCompraId)
            .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + ordenCompraId));

        if (oc.getEstado() != EstadoOrdenCompra.RECIBIDA && oc.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
            throw new IllegalStateException("Solo se puede generar CPP de una OC recibida");
        }

        if (cuentaRepository.existsByOrdenCompra(oc)) {
            throw new IllegalStateException("Ya existe una cuenta por pagar para la OC " + oc.getNumero());
        }

        CuentaPorPagar cuenta = CuentaPorPagar.builder()
            .numero(generarNumero())
            .proveedor(oc.getProveedor())
            .ordenCompra(oc)
            .descripcion("Compra según OC " + oc.getNumero())
            .monto(oc.getTotal())
            .saldoPendiente(oc.getTotal())
            .moneda(oc.getMoneda())
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
