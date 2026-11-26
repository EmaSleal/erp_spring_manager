package api.astro.whats_orders_manager.modules.compras.dto;

import java.util.List;

/** Respuesta de la compra de contado: lo que la pantalla confirma con el número de la CxP. */
public record CompraContadoResultadoDTO(Long cuentaPorPagarId, String numeroCuentaPorPagar, Long pagoId, String numeroPago,
                                        List<String> advertencias, boolean tieneAdjunto) {
}
