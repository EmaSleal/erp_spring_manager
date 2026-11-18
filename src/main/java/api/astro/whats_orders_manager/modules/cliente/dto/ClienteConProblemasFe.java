package api.astro.whats_orders_manager.modules.cliente.dto;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.ProblemaDatoFe;

import java.util.List;

/** Un cliente con facturación electrónica activa y sus datos FE inválidos (reporte de T-038). */
public record ClienteConProblemasFe(Cliente cliente, List<ProblemaDatoFe> problemas) {
}
