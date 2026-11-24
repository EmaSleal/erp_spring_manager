package api.astro.whats_orders_manager.modules.facturacion.correo.client;

/** Proveedor de correo transaccional detrás de una interfaz (ADR-070); hoy solo {@link ResendProveedorCorreo}. */
public interface ProveedorCorreo {

    /** Envía el mensaje. No lanza ante un fallo de entrega: lo clasifica en el resultado. */
    ResultadoEnvioCorreo enviar(MensajeCorreo mensaje);

    /** true si el proveedor tiene lo necesario para enviar (clave y remitente). */
    boolean configurado();
}
