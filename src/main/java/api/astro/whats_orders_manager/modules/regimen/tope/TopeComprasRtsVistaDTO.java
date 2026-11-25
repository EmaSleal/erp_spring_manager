package api.astro.whats_orders_manager.modules.regimen.tope;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Parámetros del tope de un año tal como los devuelve la API; {@code tope} = salario base x multiplicador. */
public record TopeComprasRtsVistaDTO(Long id, Integer anio, BigDecimal salarioBase, BigDecimal multiplicador, BigDecimal tope,
                                     BigDecimal umbralAviso, BigDecimal umbralTope, String nota, Integer usuarioId,
                                     String usuarioNombre, LocalDateTime fechaRegistro, LocalDateTime fechaActualizacion) {

    public static TopeComprasRtsVistaDTO de(TopeComprasRts t, String usuarioNombre) {
        return new TopeComprasRtsVistaDTO(t.getId(), t.getAnio(), t.getSalarioBase(), t.getMultiplicador(), t.tope(),
            t.getUmbralAviso(), t.getUmbralTope(), t.getNota(), t.getUsuarioId(), usuarioNombre, t.getFechaRegistro(),
            t.getFechaActualizacion());
    }
}
