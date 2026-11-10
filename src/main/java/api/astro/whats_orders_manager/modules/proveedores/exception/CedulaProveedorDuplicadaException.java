package api.astro.whats_orders_manager.modules.proveedores.exception;

/**
 * Excepción de negocio: ya existe un proveedor con la cédula indicada.
 *
 * Extiende {@link IllegalArgumentException} para no cambiar el contrato de ProveedorService; ProveedorRestController
 * la traduce a un mensaje fijo (T-030), sin devolver el texto de la excepción.
 */
public class CedulaProveedorDuplicadaException extends IllegalArgumentException {

    public CedulaProveedorDuplicadaException(String cedula) {
        super("Ya existe un proveedor con cédula: " + cedula);
    }
}
