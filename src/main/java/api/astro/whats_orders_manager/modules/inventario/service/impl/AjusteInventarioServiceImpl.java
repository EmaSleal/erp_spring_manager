package api.astro.whats_orders_manager.modules.inventario.service.impl;

import api.astro.whats_orders_manager.modules.inventario.dto.AjusteRequest;
import api.astro.whats_orders_manager.modules.inventario.enums.EstadoAjuste;
import api.astro.whats_orders_manager.modules.inventario.model.AjusteInventario;
import api.astro.whats_orders_manager.modules.inventario.model.DetalleAjusteInventario;
import api.astro.whats_orders_manager.modules.inventario.enums.TipoMovimientoInventario;
import api.astro.whats_orders_manager.modules.inventario.repository.AjusteInventarioRepository;
import api.astro.whats_orders_manager.modules.inventario.service.AjusteInventarioService;
import api.astro.whats_orders_manager.modules.inventario.service.MovimientoInventarioService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AjusteInventarioServiceImpl implements AjusteInventarioService {

    private final AjusteInventarioRepository ajusteRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioService movimientoService;

    @Override
    @Transactional
    public AjusteInventario crear(AjusteRequest request, Usuario usuario) {
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("El ajuste debe tener al menos un detalle");
        }

        AjusteInventario ajuste = AjusteInventario.builder()
            .numero(generarNumero())
            .tipoAjuste(request.getTipoAjuste())
            .motivo(request.getMotivo())
            .usuario(usuario)
            .build();

        for (AjusteRequest.DetalleRequest dr : request.getDetalles()) {
            Producto producto = productoRepository.findById(dr.getProductoId())
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + dr.getProductoId()));

            DetalleAjusteInventario detalle = DetalleAjusteInventario.builder()
                .ajuste(ajuste)
                .producto(producto)
                .cantidadFisica(dr.getCantidadFisica())
                .cantidadSistema(dr.getCantidadSistema())
                .costoUnitario(dr.getCostoUnitario())
                .build();

            ajuste.getDetalles().add(detalle);
        }

        AjusteInventario guardado = ajusteRepository.save(ajuste);
        log.info("Ajuste {} creado con {} detalles", guardado.getNumero(), guardado.getDetalles().size());
        return guardado;
    }

    @Override
    @Transactional
    public AjusteInventario aprobar(Long id, Usuario aprobadoPor) {
        AjusteInventario ajuste = ajusteRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Ajuste no encontrado: " + id));

        if (ajuste.getEstado() != EstadoAjuste.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aprobar ajustes en estado PENDIENTE");
        }

        for (DetalleAjusteInventario detalle : ajuste.getDetalles()) {
            int diferencia = detalle.getCantidadFisica() - detalle.getCantidadSistema();
            if (diferencia == 0) continue;

            Integer productoId = detalle.getProducto().getIdProducto();
            String ref = "AJUSTE";
            Long refId = ajuste.getId();
            String obs = "Ajuste " + ajuste.getNumero();

            if (diferencia > 0) {
                movimientoService.registrarEntrada(
                    productoId, diferencia, detalle.getCostoUnitario(),
                    TipoMovimientoInventario.ENTRADA_AJUSTE, ref, refId, aprobadoPor, obs, null
                );
            } else {
                movimientoService.registrarSalida(
                    productoId, Math.abs(diferencia),
                    TipoMovimientoInventario.SALIDA_AJUSTE, ref, refId, aprobadoPor, obs
                );
            }
        }

        ajuste.setEstado(EstadoAjuste.APROBADO);
        ajuste.setAprobadoPor(aprobadoPor);
        ajuste.setFechaAprobacion(LocalDateTime.now());
        log.info("Ajuste {} aprobado por {}", ajuste.getNumero(), aprobadoPor.getNombre());
        return ajusteRepository.save(ajuste);
    }

    @Override
    @Transactional
    public AjusteInventario rechazar(Long id, Usuario aprobadoPor) {
        AjusteInventario ajuste = ajusteRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Ajuste no encontrado: " + id));

        if (ajuste.getEstado() != EstadoAjuste.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden rechazar ajustes en estado PENDIENTE");
        }

        ajuste.setEstado(EstadoAjuste.RECHAZADO);
        ajuste.setAprobadoPor(aprobadoPor);
        ajuste.setFechaAprobacion(LocalDateTime.now());
        log.info("Ajuste {} rechazado por {}", ajuste.getNumero(), aprobadoPor.getNombre());
        return ajusteRepository.save(ajuste);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AjusteInventario> findAll() {
        return ajusteRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AjusteInventario> findByEstado(EstadoAjuste estado) {
        return ajusteRepository.findByEstadoOrderByFechaDesc(estado);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private synchronized String generarNumero() {
        int anio = LocalDateTime.now().getYear();
        Integer ultimo = ajusteRepository.findUltimoConsecutivo(anio);
        int siguiente = (ultimo == null ? 0 : ultimo) + 1;
        return String.format("AJ-%d-%05d", anio, siguiente);
    }
}
