package api.astro.whats_orders_manager.shared.exception;

/**
 * Error de validación de datos de entrada con un mensaje apto para el usuario (texto fijo, sin detalles internos).
 * Los controladores REST solo exponen el mensaje de esta excepción en un 400; cualquier otra excepción responde con
 * un mensaje genérico y el detalle va solo al log (P-070).
 */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
