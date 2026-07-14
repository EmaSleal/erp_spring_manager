package api.astro.whats_orders_manager.modules.inventario.service.impl;

import api.astro.whats_orders_manager.modules.inventario.dto.ResumenInventarioDTO;
import api.astro.whats_orders_manager.modules.inventario.model.LoteProducto;
import api.astro.whats_orders_manager.modules.inventario.repository.LoteProductoRepository;
import api.astro.whats_orders_manager.modules.inventario.service.ReporteInventarioService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteInventarioServiceImpl implements ReporteInventarioService {

    private static final int DIAS_ALERTA_DEFAULT = 30;

    private final ProductoRepository productoRepository;
    private final LoteProductoRepository loteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Producto> getStockCritico() {
        return productoRepository.findProductosStockCritico();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> getStockBajo() {
        return productoRepository.findProductosStockBajo();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> getPuntoReorden() {
        return productoRepository.findProductosPuntoReorden();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteProducto> getLotesPorVencer(int diasAlerta) {
        return loteRepository.findLotesPorVencer(LocalDate.now(), LocalDate.now().plusDays(diasAlerta));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenInventarioDTO getResumen() {
        List<Producto> criticos = productoRepository.findProductosStockCritico();
        List<Producto> bajos    = productoRepository.findProductosStockBajo();
        List<Producto> reorden  = productoRepository.findProductosPuntoReorden();
        List<LoteProducto> vencidos    = loteRepository.findLotesVencidos(LocalDate.now());
        List<LoteProducto> porVencer   = loteRepository.findLotesPorVencer(
            LocalDate.now(), LocalDate.now().plusDays(DIAS_ALERTA_DEFAULT)
        );
        long totalActivos = productoRepository.count();

        return ResumenInventarioDTO.builder()
            .totalProductosActivos((int) totalActivos)
            .productosStockCritico(criticos.size())
            .productosStockBajo(bajos.size())
            .productosPuntoReorden(reorden.size())
            .lotesVencidos(vencidos.size())
            .lotesPorVencer(porVencer.size())
            .build();
    }
}
