package api.astro.whats_orders_manager.modules.inventario.service;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaInventarioService {

    private final ProductoRepository productoRepository;
    private final LoteProductoService loteProductoService;

    /** Runs daily at 06:00 to flag stock alerts and expire stale lots. */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void verificarAlertas() {
        log.info("=== Revisión diaria de inventario iniciada ===");

        int vencidos = loteProductoService.marcarVencidos();
        if (vencidos > 0) {
            log.warn("INVENTARIO: {} lote(s) marcados como VENCIDO", vencidos);
        }

        List<Producto> criticos = productoRepository.findProductosStockCritico();
        if (!criticos.isEmpty()) {
            log.warn("INVENTARIO: {} producto(s) en stock CRÍTICO:", criticos.size());
            criticos.forEach(p -> log.warn("  - {} (stock: {}, mínimo: {})",
                p.getDescripcion(), p.getStock(), p.getStockMinimo()));
        }

        List<Producto> reorden = productoRepository.findProductosPuntoReorden();
        if (!reorden.isEmpty()) {
            log.info("INVENTARIO: {} producto(s) alcanzaron punto de reorden", reorden.size());
        }

        log.info("=== Revisión diaria de inventario completada — críticos: {}, vencidos: {} ===",
            criticos.size(), vencidos);
    }
}
