package api.astro.whats_orders_manager.modules.regimen.panel;

import api.astro.whats_orders_manager.modules.regimen.tope.EstadoTope;

/**
 * Bloque de régimen del panel de Facturación (T-056). {@code avisoVerificacion} es null si no hay nada que avisar;
 * {@code tope} es el estado del año del reloj (null si no se pudo calcular).
 */
public record RegimenPanel(String avisoVerificacion, EstadoTope tope, String advertenciaTope) {
}
