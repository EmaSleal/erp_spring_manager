package api.astro.whats_orders_manager.modules.facturacion.exception;

import api.astro.whats_orders_manager.modules.facturacion.service.PoliticaMutabilidadFactura;

/**
 * La factura no se puede modificar, anular ni eliminar por el estado de su comprobante electrónico vigente
 * (ADR-030). Se responde con 409 y el motivo accionable (FacturaBloqueadaAdvice).
 *
 * No extiende IllegalStateException a propósito: los controladores que convierten IllegalStateException en un
 * mensaje flash no deben tragarse el 409.
 */
public class FacturaBloqueadaException extends RuntimeException {

    private final transient PoliticaMutabilidadFactura.Decision decision;

    public FacturaBloqueadaException(PoliticaMutabilidadFactura.Decision decision) {
        super(decision.motivo());
        this.decision = decision;
    }

    public PoliticaMutabilidadFactura.Decision getDecision() {
        return decision;
    }
}
