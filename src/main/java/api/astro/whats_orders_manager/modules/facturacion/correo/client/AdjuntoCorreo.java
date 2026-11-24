package api.astro.whats_orders_manager.modules.facturacion.correo.client;

/** Archivo adjunto de un correo: nombre y bytes. */
public record AdjuntoCorreo(String nombre, byte[] contenido) {

    @Override
    public String toString() {
        return "AdjuntoCorreo(" + nombre + ", " + (contenido == null ? 0 : contenido.length) + " bytes)";
    }
}
