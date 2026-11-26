package api.astro.whats_orders_manager.modules.facturacion.exception;

/**
 * La edición de una factura la pasaría a un tramo de otro régimen tributario (T-057, ADR-073): el régimen se sella al
 * crearla y no se recalcula. Se responde 409 con un mensaje fijo (FacturaRegimenAdvice).
 *
 * No extiende IllegalStateException a propósito: los controladores que convierten IllegalStateException en un mensaje
 * flash o en 500 no deben tragarse el 409.
 */
public class FacturaRegimenConflictoException extends RuntimeException {

    public FacturaRegimenConflictoException(String mensajeUsuario) {
        super(mensajeUsuario);
    }
}
