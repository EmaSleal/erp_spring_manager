package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.facturacion.service.PoliticaMutabilidadFactura;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Segunda capa de ADR-030 §6: rechaza insertar, modificar o borrar una línea de una factura cuyo comprobante
 * vigente bloquea los campos fiscales, aunque la operación no pase por los servicios (p. ej. una llamada directa
 * al repositorio en una ruta futura).
 *
 * Hibernate la crea con el SpringBeanContainer de Spring Boot, que inyecta la política. Se usa ObjectProvider para
 * que los contextos de prueba parciales (@DataJpaTest) que no registran la política sigan arrancando: sin política
 * disponible no se aplica ningún control.
 */
public class LineaFacturaMutabilidadListener {

    @Autowired
    private ObjectProvider<PoliticaMutabilidadFactura> politica;

    @PrePersist
    @PreUpdate
    @PreRemove
    public void verificar(LineaFactura linea) {
        PoliticaMutabilidadFactura p = politica != null ? politica.getIfAvailable() : null;
        if (p == null || linea.getFactura() == null) {
            return;
        }
        p.exigirEditable(linea.getFactura().getIdFactura());
    }
}
