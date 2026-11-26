package api.astro.whats_orders_manager.modules.proveedores.exception;

/** «Verificar régimen» no obtuvo el régimen del proveedor en Hacienda (sin respuesta, error o identificación no hallada). */
public class RegimenNoVerificableException extends RuntimeException {

    public RegimenNoVerificableException() {
        super("No se pudo verificar el régimen del proveedor en Hacienda");
    }
}
