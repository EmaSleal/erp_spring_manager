package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;

import java.util.List;

/** Resultado del alta de una CxP: la cuenta guardada y sus avisos (IVA de una línea y exceso sobre la orden de compra). */
public record CuentaPorPagarCreada(CuentaPorPagar cuenta, List<String> advertencias) {
}
