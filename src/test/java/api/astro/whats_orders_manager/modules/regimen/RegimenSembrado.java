package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T-057: las pruebas de integración con esquema de Hibernate (Flyway apagado) arrancan con regimen_tributario vacía y toda
 * factura creada por FacturaServiceImpl.save exige un tramo para su fecha (no hay régimen por defecto, ADR-072). Esta ayuda
 * siembra, solo si la tabla está vacía, un tramo GENERAL desde 2000-01-01: el comportamiento de siempre de esas pruebas.
 * No toca el código de producción.
 */
public final class RegimenSembrado {

    private RegimenSembrado() {
    }

    /** Tramo GENERAL desde 2000-01-01 si no hay ningún tramo; deja la caché del servicio al día. */
    public static void generalSiVacia(JdbcTemplate jdbc, RegimenTributarioService servicio) {
        jdbc.update("INSERT INTO regimen_tributario (regimen, vigente_desde, nota, fecha_registro) "
                + "SELECT 'GENERAL', '2000-01-01', 'Semilla de pruebas T-057', NOW() FROM (SELECT 1) t "
                + "WHERE NOT EXISTS (SELECT 1 FROM regimen_tributario)");
        servicio.invalidarCache();
    }
}
