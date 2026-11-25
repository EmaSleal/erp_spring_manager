/**
 * Políticas por régimen tributario (ADR-072). Este es, junto con {@code modules.regimen}, el único paquete donde se
 * puede consultar el régimen vigente (RegimenTributarioService); ArquitecturaRegimenTest lo hace cumplir. Las ventas,
 * la emisión, las notas, las compras y los reportes obtienen su comportamiento de una política de este paquete y no
 * preguntan el régimen directamente. Las políticas llegan con P-129, P-130 y P-131; por ahora el paquete está vacío.
 */
package api.astro.whats_orders_manager.modules.politicas;
