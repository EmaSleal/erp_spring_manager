package api.astro.whats_orders_manager.modules.inventario.service.impl;

import api.astro.whats_orders_manager.modules.inventario.enums.EstadoLote;
import api.astro.whats_orders_manager.modules.inventario.enums.TipoMovimientoInventario;
import api.astro.whats_orders_manager.modules.inventario.model.LoteProducto;
import api.astro.whats_orders_manager.modules.inventario.model.MovimientoInventario;
import api.astro.whats_orders_manager.modules.inventario.repository.LoteProductoRepository;
import api.astro.whats_orders_manager.modules.inventario.repository.MovimientoInventarioRepository;
import api.astro.whats_orders_manager.modules.inventario.service.MovimientoInventarioService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteRepository;

    @Override
    @Transactional
    public MovimientoInventario registrarSalida(
        Integer productoId,
        Integer cantidad,
        TipoMovimientoInventario tipo,
        String documentoOrigen,
        Long documentoOrigenId,
        Usuario usuario,
        String observaciones
    ) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        Producto producto = productoRepository.findByIdForUpdate(productoId)
            .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + productoId));

        int stockActual = producto.getStock() != null ? producto.getStock() : 0;
        if (stockActual < cantidad) {
            throw new IllegalArgumentException(String.format(
                "Stock insuficiente para '%s'. Disponible: %d, Solicitado: %d",
                producto.getDescripcion(), stockActual, cantidad
            ));
        }

        MovimientoInventario movimiento = MovimientoInventario.builder()
            .producto(producto)
            .tipo(tipo)
            .cantidad(-cantidad)
            .costoUnitario(producto.getCosto())
            .stockAnterior(stockActual)
            .stockNuevo(stockActual - cantidad)
            .documentoOrigen(documentoOrigen)
            .documentoOrigenId(documentoOrigenId)
            .observaciones(observaciones)
            .usuario(usuario)
            .build();

        if (Boolean.TRUE.equals(producto.getManejaLotes())) {
            aplicarSalidaConLotes(movimiento, producto, cantidad);
        }

        producto.setStock(stockActual - cantidad);
        productoRepository.save(producto);

        movimientoRepository.save(movimiento);

        log.info("Salida registrada: {} u. de '{}' ({})", cantidad, producto.getDescripcion(), tipo);
        return movimiento;
    }

    @Override
    @Transactional
    public MovimientoInventario registrarEntrada(
        Integer productoId,
        Integer cantidad,
        BigDecimal costoUnitario,
        TipoMovimientoInventario tipo,
        String documentoOrigen,
        Long documentoOrigenId,
        Usuario usuario,
        String observaciones,
        LoteProducto lote
    ) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        Producto producto = productoRepository.findByIdForUpdate(productoId)
            .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + productoId));

        int stockActual = producto.getStock() != null ? producto.getStock() : 0;

        MovimientoInventario movimiento = MovimientoInventario.builder()
            .producto(producto)
            .tipo(tipo)
            .cantidad(cantidad)
            .costoUnitario(costoUnitario)
            .stockAnterior(stockActual)
            .stockNuevo(stockActual + cantidad)
            .documentoOrigen(documentoOrigen)
            .documentoOrigenId(documentoOrigenId)
            .observaciones(observaciones)
            .usuario(usuario)
            .lote(lote)
            .build();

        if (costoUnitario != null && costoUnitario.compareTo(BigDecimal.ZERO) > 0) {
            actualizarCostoPromedio(producto, cantidad, costoUnitario);
        }

        producto.setStock(stockActual + cantidad);
        productoRepository.save(producto);

        if (lote != null) {
            lote.setCantidad(lote.getCantidad() + cantidad);
            loteRepository.save(lote);
        }

        movimientoRepository.save(movimiento);

        log.info("Entrada registrada: {} u. de '{}' ({})", cantidad, producto.getDescripcion(), tipo);
        return movimiento;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventario> obtenerKardex(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + productoId));
        return movimientoRepository.findByProductoOrderByFechaDesc(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventario> obtenerKardexTodos() {
        return movimientoRepository.findAllByOrderByFechaDesc();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void aplicarSalidaConLotes(MovimientoInventario movimiento, Producto producto, int cantidad) {
        List<LoteProducto> lotes = loteRepository
            .findByProductoAndEstadoOrderByFechaVencimientoAsc(producto, EstadoLote.ACTIVO);

        int restante = cantidad;
        for (LoteProducto lote : lotes) {
            if (restante == 0) break;
            int consumido = Math.min(lote.getCantidad(), restante);
            lote.setCantidad(lote.getCantidad() - consumido);
            restante -= consumido;
            loteRepository.save(lote);
            if (movimiento.getLote() == null) {
                movimiento.setLote(lote);
            }
        }

        if (restante > 0) {
            throw new IllegalArgumentException(
                "Lotes insuficientes: faltan " + restante + " unidades para completar la salida"
            );
        }
    }

    private void actualizarCostoPromedio(Producto producto, int cantidadNueva, BigDecimal costoNuevo) {
        BigDecimal stockAnterior = BigDecimal.valueOf(producto.getStock() != null ? producto.getStock() : 0);
        BigDecimal costoAnterior = producto.getCosto() != null ? producto.getCosto() : BigDecimal.ZERO;
        BigDecimal cantNueva = BigDecimal.valueOf(cantidadNueva);

        BigDecimal cantidadTotal = stockAnterior.add(cantNueva);
        if (cantidadTotal.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal costoPromedio = stockAnterior.multiply(costoAnterior)
            .add(cantNueva.multiply(costoNuevo))
            .divide(cantidadTotal, 2, RoundingMode.HALF_UP);

        producto.setCosto(costoPromedio);
    }
}
