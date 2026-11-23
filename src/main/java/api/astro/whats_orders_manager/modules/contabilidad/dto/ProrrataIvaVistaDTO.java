package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.contabilidad.model.ProrrataIva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Prorrata del IVA de un año tal como la devuelve la API (T-054). */
public record ProrrataIvaVistaDTO(Long id, Integer anio, BigDecimal porcentaje, String nota, Integer usuarioId,
                                  String usuarioNombre, LocalDateTime fechaRegistro, LocalDateTime fechaActualizacion) {

    public static ProrrataIvaVistaDTO de(ProrrataIva p, String usuarioNombre) {
        return new ProrrataIvaVistaDTO(p.getId(), p.getAnio(), p.getPorcentaje(), p.getNota(), p.getUsuarioId(),
            usuarioNombre, p.getFechaRegistro(), p.getFechaActualizacion());
    }
}
