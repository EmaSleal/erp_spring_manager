package api.astro.whats_orders_manager.modules.producto.service.impl;

import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ArticuloMaestroRepository;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.producto.service.ArticuloMaestroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ArticuloMaestroServiceImpl implements ArticuloMaestroService {

    private final ArticuloMaestroRepository maestroRepository;
    private final ProductoRepository productoRepository;

    @Override
    public List<ArticuloMaestro> findAll() {
        return maestroRepository.findAll();
    }

    @Override
    public Optional<ArticuloMaestro> findById(Integer id) {
        return maestroRepository.findById(id);
    }

    /**
     * Save (create or update) a master article and write-through common/FE fields
     * to all linked Producto variants in the same transaction.
     *
     * The deprecated Producto fields (descripcion, active, gravado, etc.) are updated
     * so that any legacy read path not yet navigating through the master still sees
     * consistent data during the transition period.
     */
    @Override
    @SuppressWarnings("deprecation") // Intentional write-through to deprecated Producto fields
    public ArticuloMaestro save(ArticuloMaestro maestro) {
        ArticuloMaestro saved = maestroRepository.save(maestro);
        if (saved.getIdArticuloMaestro() != null) {
            List<Producto> variantes = productoRepository
                    .findByArticuloMaestro_IdArticuloMaestro(saved.getIdArticuloMaestro());
            if (!variantes.isEmpty()) {
                variantes.forEach(v -> {
                    v.setDescripcion(saved.getDescripcion());
                    v.setActive(saved.getActive());
                    v.setGravado(saved.getGravado());
                    v.setPorcentajeImpuesto(saved.getPorcentajeImpuesto());
                    v.setAplicaOtroImpuesto(saved.getAplicaOtroImpuesto());
                    v.setCodigoCabys(saved.getCodigoCabys());
                    v.setDescripcionCabys(saved.getDescripcionCabys());
                });
                productoRepository.saveAll(variantes);
                log.info("Write-through: updated {} variant(s) for master id={}", variantes.size(), saved.getIdArticuloMaestro());
            }
        }
        return saved;
    }

    /**
     * Deactivate master and cascade active=false to all variants atomically.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void deactivate(Integer id) {
        ArticuloMaestro maestro = maestroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ArticuloMaestro not found: " + id));
        maestro.setActive(false);
        maestroRepository.save(maestro);

        List<Producto> variantes = productoRepository.findByArticuloMaestro_IdArticuloMaestro(id);
        variantes.forEach(v -> v.setActive(false));
        productoRepository.saveAll(variantes);
        log.info("Deactivated master id={} and {} variant(s)", id, variantes.size());
    }

    /**
     * Reactivate master and cascade active=true to all variants atomically.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void reactivate(Integer id) {
        ArticuloMaestro maestro = maestroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ArticuloMaestro not found: " + id));
        maestro.setActive(true);
        maestroRepository.save(maestro);

        List<Producto> variantes = productoRepository.findByArticuloMaestro_IdArticuloMaestro(id);
        variantes.forEach(v -> v.setActive(true));
        productoRepository.saveAll(variantes);
        log.info("Reactivated master id={} and {} variant(s)", id, variantes.size());
    }

    /**
     * Delete a master article.
     * Rejects deletion when any variant is still linked, to prevent orphaned products.
     */
    @Override
    public void deleteById(Integer id) {
        List<Producto> variantes = productoRepository.findByArticuloMaestro_IdArticuloMaestro(id);
        if (!variantes.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete master with existing variants. Master id=" + id
                    + " has " + variantes.size() + " variant(s).");
        }
        maestroRepository.deleteById(id);
        log.info("Deleted master id={}", id);
    }

    @Override
    public List<Producto> findVariantes(Integer maestroId) {
        return productoRepository.findByArticuloMaestro_IdArticuloMaestro(maestroId);
    }

    /**
     * Delete a single variant from a master article.
     * Rejects deletion when the variant is the last one, so every master retains at least one variant.
     */
    @Override
    @Transactional
    public void deleteVariante(Integer maestroId, Integer varianteId) {
        List<Producto> variantes = productoRepository.findByArticuloMaestro_IdArticuloMaestro(maestroId);
        if (variantes.size() <= 1) {
            throw new IllegalStateException("Cannot delete the last variant of a master article");
        }
        productoRepository.deleteById(varianteId);
        log.info("Deleted variant id={} from master id={}", varianteId, maestroId);
    }

    /**
     * Returns a map of master article ID to variant count.
     * Used by the list view to avoid lazy-loading the variantes collection in the Thymeleaf template.
     */
    @Override
    public Map<Integer, Long> getVarianteCounts() {
        List<Object[]> results = maestroRepository.countVariantesByMaestro();
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : results) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }
}
