package api.astro.whats_orders_manager.modules.facturacion.dto;

import java.util.List;

/** Una página del listado de notas de crédito (pagina es 0-based). */
public record NotaCreditoPaginaDTO(List<NotaCreditoListaDTO> contenido, int pagina, int tamano, long totalElementos,
                                   int totalPaginas) {
}
