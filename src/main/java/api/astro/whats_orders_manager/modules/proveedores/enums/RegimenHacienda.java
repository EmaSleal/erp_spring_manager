package api.astro.whats_orders_manager.modules.proveedores.enums;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;

/**
 * Régimen tributario de un proveedor tal como lo informa Hacienda en /fe/ae (T-058, ADR-074 §2). Es el dominio de
 * {@code proveedores.regimen_hacienda} y de {@code cuentas_por_pagar.regimen_proveedor}. No confundir con
 * {@code regimen.enums.Regimen}, que es el de la empresa (solo GENERAL o SIMPLIFICADO).
 *
 * La traducción del código de Hacienda es propia de este dominio (1 = GENERAL, 2 = SIMPLIFICADO, 3 = NO_CONTRIBUYENTE, otro
 * código = OTRO) y no reutiliza la de la verificación del régimen propio (T-056), que solo distingue GENERAL, SIMPLIFICADO y OTRO.
 */
public enum RegimenHacienda {
    GENERAL,
    SIMPLIFICADO,
    NO_CONTRIBUYENTE,
    OTRO,
    /** Sin consulta, o la consulta falló, no respondió o no encontró la identificación. Es el valor por defecto. */
    SIN_VERIFICAR;

    /** Régimen de un código de Hacienda; un código desconocido es OTRO y nulo es SIN_VERIFICAR. */
    public static RegimenHacienda deCodigo(Integer codigo) {
        if (codigo == null) {
            return SIN_VERIFICAR;
        }
        return switch (codigo) {
            case 1 -> GENERAL;
            case 2 -> SIMPLIFICADO;
            case 3 -> NO_CONTRIBUYENTE;
            default -> OTRO;
        };
    }

    /**
     * Régimen de una respuesta de /fe/ae. Solo una consulta EXITOSA con régimen informado traduce el código; ERROR,
     * NO_ENCONTRADA (Hacienda no conoce la identificación: no hay régimen que registrar), respuesta nula o sin régimen son
     * SIN_VERIFICAR.
     */
    public static RegimenHacienda deConsulta(HaciendaConsultaDTO consulta) {
        if (consulta == null || consulta.getResultado() != HaciendaConsultaDTO.ResultadoConsulta.EXITOSA
                || consulta.getRegimen() == null) {
            return SIN_VERIFICAR;
        }
        return deCodigo(consulta.getRegimen().getCodigo());
    }
}
