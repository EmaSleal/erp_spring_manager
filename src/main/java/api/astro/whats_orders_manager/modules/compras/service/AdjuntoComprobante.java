package api.astro.whats_orders_manager.modules.compras.service;

/** Adjunto ya validado (extensión, tamaño y contenido) listo para guardarse; el tipo sale de la extensión, no del cliente. */
public record AdjuntoComprobante(byte[] contenido, String nombreOriginal, String extension, String tipo) {
}
