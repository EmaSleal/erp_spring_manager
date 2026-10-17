package api.astro.whats_orders_manager.modules.facturacion.repository;

import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineaFacturaRepository extends JpaRepository<LineaFactura, Integer> {

    /**
     * Carga líneas de factura como entidades JPA con producto/presentación para procesos internos
     * (ej. generación de XML de comprobante electrónico).
     */
    @Query("SELECT lf FROM LineaFactura lf " +
           "LEFT JOIN FETCH lf.producto p " +
           "LEFT JOIN FETCH p.presentacion " +
           "WHERE lf.factura.idFactura = :idFactura " +
           "ORDER BY lf.numeroLinea")
    List<LineaFactura> findEntitiesByFacturaId(@Param("idFactura") Integer idFactura);
}
