package api.astro.whats_orders_manager.modules.contabilidad.dto;

import java.util.List;

/** Respuesta del alta de una CxP: la cuenta creada y los avisos (IVA de una fila que difiere de base × tarifa). */
public record CuentaPorPagarCreadaDTO(CuentaPorPagarVistaDTO cuenta, List<String> advertencias) {
}
