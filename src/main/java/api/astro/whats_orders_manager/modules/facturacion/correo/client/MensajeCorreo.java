package api.astro.whats_orders_manager.modules.facturacion.correo.client;

import java.util.List;

/**
 * Mensaje que se entrega a un {@link ProveedorCorreo}.
 *
 * @param remitente    From, con nombre
 * @param destinatario dirección del receptor (una sola)
 * @param cco          copias ocultas (puede ser vacía)
 * @param replyTo      Reply-To o null
 */
public record MensajeCorreo(String remitente, String destinatario, List<String> cco, String replyTo, String asunto,
                            String html, List<AdjuntoCorreo> adjuntos) {
}
