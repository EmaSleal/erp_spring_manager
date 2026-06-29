package api.astro.whats_orders_manager.modules.producto.service;

import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ArticuloMaestroService {

    List<ArticuloMaestro> findAll();

    Optional<ArticuloMaestro> findById(Integer id);

    /**
     * Save (create or update) a master article.
     * When updating, write-through propagates descripcion, active, and all
     * FE/tax fields to every linked Producto variant within the same transaction.
     */
    ArticuloMaestro save(ArticuloMaestro maestro);

    /**
     * Deactivate a master and all its Producto variants atomically.
     * Sets master.active=false and variant.active=false for every linked variant.
     */
    void deactivate(Integer id);

    /**
     * Reactivate a master and all its Producto variants atomically.
     * Sets master.active=true and variant.active=true for every linked variant.
     */
    void reactivate(Integer id);

    /**
     * Delete a master article.
     * Throws {@link IllegalStateException} when one or more variants are still linked to it.
     */
    void deleteById(Integer id);

    /**
     * Return all Producto variants linked to the given master ID.
     */
    List<Producto> findVariantes(Integer maestroId);

    /**
     * Delete a single variant from a master article.
     * Throws {@link IllegalStateException} when the variant to delete is the last one
     * linked to the master — ensures every master retains at least one variant.
     */
    void deleteVariante(Integer maestroId, Integer varianteId);

    /**
     * Returns a map of master article ID to variant count.
     * Used by the list view to avoid lazy-loading the variantes collection in the template.
     */
    Map<Integer, Long> getVarianteCounts();
}
