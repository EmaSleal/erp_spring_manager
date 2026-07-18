package api.astro.whats_orders_manager.modules.inventario.service.impl;

import api.astro.whats_orders_manager.modules.inventario.enums.EstadoLote;
import api.astro.whats_orders_manager.modules.inventario.model.LoteProducto;
import api.astro.whats_orders_manager.modules.inventario.repository.LoteProductoRepository;
import api.astro.whats_orders_manager.modules.inventario.service.LoteProductoService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoteProductoServiceImpl implements LoteProductoService {

    private final LoteProductoRepository loteRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public LoteProducto crearLote(
        Integer productoId,
        String codigo,
        Integer cantidad,
        LocalDate fechaVencimiento,
        Proveedor proveedor
    ) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + productoId));

        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad del lote debe ser mayor a 0");
        }

        LoteProducto lote = LoteProducto.builder()
            .producto(producto)
            .codigo(codigo)
            .cantidad(cantidad)
            .cantidadInicial(cantidad)
            .fechaVencimiento(fechaVencimiento)
            .proveedor(proveedor)
            .estado(EstadoLote.ACTIVO)
            .build();

        LoteProducto guardado = loteRepository.save(lote);
        log.info("Lote {} creado para producto {} con {} unidades", codigo, productoId, cantidad);
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteProducto> findByProducto(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + productoId));
        return loteRepository.findByProducto(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteProducto> findLotesPorVencer(int diasAlerta) {
        LocalDate desde = LocalDate.now();
        LocalDate hasta = LocalDate.now().plusDays(diasAlerta);
        return loteRepository.findLotesPorVencer(desde, hasta);
    }

    @Override
    @Transactional
    public int marcarVencidos() {
        List<LoteProducto> vencidos = loteRepository.findLotesVencidos(LocalDate.now());
        vencidos.forEach(l -> l.setEstado(EstadoLote.VENCIDO));
        loteRepository.saveAll(vencidos);
        if (!vencidos.isEmpty()) {
            log.info("Marcados {} lotes como VENCIDO", vencidos.size());
        }
        return vencidos.size();
    }
}
