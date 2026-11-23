package api.astro.whats_orders_manager.modules.proveedores.service.impl;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.inventario.enums.TipoMovimientoInventario;
import api.astro.whats_orders_manager.modules.inventario.model.LoteProducto;
import api.astro.whats_orders_manager.modules.inventario.service.LoteProductoService;
import api.astro.whats_orders_manager.modules.inventario.service.MovimientoInventarioService;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.proveedores.dto.DetalleOrdenCompraDTO;
import api.astro.whats_orders_manager.modules.proveedores.dto.OrdenCompraDTO;
import api.astro.whats_orders_manager.modules.proveedores.dto.OrdenCompraVistaDTO;
import api.astro.whats_orders_manager.modules.proveedores.dto.RecepcionDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.DetalleOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.OrdenCompraConFacturasException;
import api.astro.whats_orders_manager.modules.proveedores.service.OrdenCompraService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class OrdenCompraServiceImpl implements OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioService movimientoService;
    private final LoteProductoService loteService;
    private final TipoCambioService tipoCambioService;
    private final CuentaPorPagarRepository cuentaPorPagarRepository;
    private final Clock clock;
    private final boolean controlInventarioHabilitado;

    public OrdenCompraServiceImpl(OrdenCompraRepository ordenCompraRepository, ProveedorRepository proveedorRepository,
                                  ProductoRepository productoRepository, MovimientoInventarioService movimientoService,
                                  LoteProductoService loteService, TipoCambioService tipoCambioService,
                                  CuentaPorPagarRepository cuentaPorPagarRepository, Clock clock,
                                  @Value("${inventario.control.habilitado}") boolean controlInventarioHabilitado) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.movimientoService = movimientoService;
        this.loteService = loteService;
        this.tipoCambioService = tipoCambioService;
        this.cuentaPorPagarRepository = cuentaPorPagarRepository;
        this.clock = clock;
        this.controlInventarioHabilitado = controlInventarioHabilitado;
    }

    @Override
    @Transactional
    public OrdenCompra crear(OrdenCompraDTO dto, Usuario usuario) {
        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
            .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado: " + dto.getProveedorId()));

        if (!proveedor.getActivo()) {
            throw new IllegalArgumentException("El proveedor no está activo: " + proveedor.getNombre());
        }

        // ADR-011: tipo de cambio propio de la orden (tasa de venta del día por defecto, editable; CRC = 1)
        MonedaFE moneda = dto.getMoneda() != null ? dto.getMoneda() : MonedaFE.CRC;
        LocalDate fecha = LocalDate.now();
        BigDecimal tipoCambio = tipoCambioService.resolverTipoCambioOperacion(moneda, moneda, dto.getTipoCambio(), fecha);

        OrdenCompra oc = OrdenCompra.builder()
            .numero(generarNumero())
            .proveedor(proveedor)
            .fecha(fecha)
            .fechaEntregaEsperada(dto.getFechaEntregaEsperada())
            .moneda(moneda)
            .tipoCambio(tipoCambio)
            .impuesto(dto.getImpuesto() != null ? dto.getImpuesto() : BigDecimal.ZERO)
            .notas(dto.getNotas())
            .creadoPor(usuario)
            .build();

        if (dto.getDetalles() != null) {
            for (DetalleOrdenCompraDTO d : dto.getDetalles()) {
                var producto = d.getProductoId() != null
                    ? productoRepository.findById(d.getProductoId()).orElse(null)
                    : null;
                BigDecimal lineTotal = d.getPrecioUnitario() != null && d.getCantidad() != null
                    ? d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()))
                    : BigDecimal.ZERO;
                oc.getDetalles().add(DetalleOrdenCompra.builder()
                    .ordenCompra(oc)
                    .producto(producto)
                    .descripcion(d.getDescripcion())
                    .cantidad(d.getCantidad())
                    .precioUnitario(d.getPrecioUnitario())
                    .total(lineTotal)
                    .build());
            }
        }

        oc.recalcularTotales();
        OrdenCompra guardada = ordenCompraRepository.save(oc);
        log.info("OC {} creada para proveedor {} por {}", guardada.getNumero(), proveedor.getNombre(), usuario.getNombre());
        return guardada;
    }

    @Override
    @Transactional
    public OrdenCompra aprobar(Long id, Usuario usuario) {
        OrdenCompra oc = findById(id);
        if (oc.getEstado() != EstadoOrdenCompra.BORRADOR && oc.getEstado() != EstadoOrdenCompra.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar OC en estado BORRADOR o PENDIENTE");
        }
        oc.setEstado(EstadoOrdenCompra.APROBADA);
        oc.setAprobadoPor(usuario);
        oc.setFechaAprobacion(LocalDateTime.now());
        log.info("OC {} aprobada por {}", oc.getNumero(), usuario.getNombre());
        return ordenCompraRepository.save(oc);
    }

    @Override
    @Transactional
    public OrdenCompra cancelar(Long id, Usuario usuario) {
        // mismo orden de bloqueo que el alta de una CxP (proveedor → orden → CxP): una factura que se registra a la vez
        // o la ve esta cancelación o la rechaza el estado de la orden
        Long proveedorId = ordenCompraRepository.findProveedorId(id)
            .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + id));
        proveedorRepository.findByIdForUpdate(proveedorId)
            .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado: " + proveedorId));
        OrdenCompra oc = ordenCompraRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + id));
        if (oc.getEstado() == EstadoOrdenCompra.RECIBIDA || oc.getEstado() == EstadoOrdenCompra.CANCELADA) {
            throw new IllegalStateException("No se puede cancelar una OC en estado " + oc.getEstado());
        }
        if (!cuentaPorPagarRepository.findByOrdenCompraAndEstadoNotForUpdate(oc, EstadoCuentaPorPagar.ANULADA).isEmpty()) {
            throw new OrdenCompraConFacturasException();
        }
        oc.setEstado(EstadoOrdenCompra.CANCELADA);
        log.info("OC {} cancelada por {}", oc.getNumero(), usuario.getNombre());
        return ordenCompraRepository.save(oc);
    }

    @Override
    @Transactional
    public OrdenCompra recibirParcial(RecepcionDTO dto, Usuario usuario) {
        OrdenCompra oc = findById(dto.getOrdenCompraId());

        if (oc.getEstado() != EstadoOrdenCompra.APROBADA
            && oc.getEstado() != EstadoOrdenCompra.ENVIADA
            && oc.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
            throw new IllegalStateException("Solo se pueden recibir OC en estado APROBADA, ENVIADA o RECIBIDA_PARCIAL");
        }

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("La recepción debe incluir al menos un ítem");
        }

        for (RecepcionDTO.ItemRecepcionDTO item : dto.getItems()) {
            DetalleOrdenCompra detalle = oc.getDetalles().stream()
                .filter(d -> d.getId().equals(item.getDetalleId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Detalle de OC no encontrado: " + item.getDetalleId()));

            // con el control de inventario habilitado solo se reciben las líneas con producto (comportamiento anterior);
            // sin él (ADR-066 §5, ADR-061) toda línea se recibe por cantidad y nada toca el inventario
            if (controlInventarioHabilitado && detalle.getProducto() == null) continue;

            int max = detalle.getCantidad() - detalle.getCantidadRecibida();
            int aRecibir = Math.min(item.getCantidadRecibida(), max);
            if (aRecibir <= 0) continue;

            if (controlInventarioHabilitado) {
                LoteProducto lote = null;
                if (item.getCodigoLote() != null && !item.getCodigoLote().isBlank()) {
                    lote = loteService.crearLote(
                        detalle.getProducto().getIdProducto(),
                        item.getCodigoLote(),
                        aRecibir,
                        item.getFechaVencimiento(),
                        oc.getProveedor()
                    );
                }

                BigDecimal costo = item.getCostoUnitario() != null ? item.getCostoUnitario() : detalle.getPrecioUnitario();
                movimientoService.registrarEntrada(
                    detalle.getProducto().getIdProducto(),
                    aRecibir,
                    costo,
                    TipoMovimientoInventario.ENTRADA_COMPRA,
                    "OC",
                    oc.getId(),
                    usuario,
                    "Recepción OC " + oc.getNumero(),
                    lote
                );
            }

            detalle.setCantidadRecibida(detalle.getCantidadRecibida() + aRecibir);
        }

        List<DetalleOrdenCompra> detallesAContar = oc.getDetalles().stream()
            .filter(d -> !controlInventarioHabilitado || d.getProducto() != null)
            .toList();

        boolean todosCompletos = !detallesAContar.isEmpty()
            && detallesAContar.stream().allMatch(DetalleOrdenCompra::isRecibidoCompleto);

        oc.setEstado(todosCompletos ? EstadoOrdenCompra.RECIBIDA : EstadoOrdenCompra.RECIBIDA_PARCIAL);
        if (todosCompletos) oc.setFechaRecepcion(LocalDate.now(clock));

        return ordenCompraRepository.save(oc);
    }

    @Override
    public boolean recepcionMueveInventario() {
        return controlInventarioHabilitado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenCompra> findAll() {
        return ordenCompraRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenCompra findById(Long id) {
        return ordenCompraRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenCompraVistaDTO vistaById(Long id) {
        return OrdenCompraVistaDTO.de(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenCompraVistaDTO> vistas(EstadoOrdenCompra estado) {
        List<OrdenCompra> lista = estado != null ? ordenCompraRepository.findByEstadoOrderByFechaDesc(estado)
            : ordenCompraRepository.findAll();
        return lista.stream().map(OrdenCompraVistaDTO::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenCompra> findByEstado(EstadoOrdenCompra estado) {
        return ordenCompraRepository.findByEstadoOrderByFechaDesc(estado);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private synchronized String generarNumero() {
        int anio = LocalDateTime.now().getYear();
        Integer ultimo = ordenCompraRepository.findUltimoConsecutivo(anio);
        int siguiente = (ultimo == null ? 0 : ultimo) + 1;
        return String.format("OC-%d-%05d", anio, siguiente);
    }
}
