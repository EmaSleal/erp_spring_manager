package api.astro.whats_orders_manager.modules.regimen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService.TramoEstado;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tramo tal como lo devuelve la API (T-056). {@code vigente}: rige hoy; {@code proximo}: el primer tramo futuro;
 * {@code editable}: se puede editar o borrar; {@code bloqueo}: el motivo cuando no (mensaje fijo del servicio).
 */
public record RegimenTributarioVistaDTO(Long id, Regimen regimen,
                                        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate vigenteDesde, String nota, Integer usuarioId,
                                        String usuarioNombre, LocalDateTime fechaRegistro, boolean vigente,
                                        boolean proximo, boolean editable, String bloqueo) {

    /**
     * Respuesta de {@code GET /api/regimen-tributario/vigente}: el tramo que rige hoy (con {@code vigente=true}, sus
     * mismos campos en la raíz de {@code data}) y, si existe, el siguiente en {@code siguiente} (con {@code proximo=true}).
     */
    public record Vigente(@JsonUnwrapped RegimenTributarioVistaDTO tramo,
                          @JsonInclude(JsonInclude.Include.NON_NULL) RegimenTributarioVistaDTO siguiente) {
    }

    /** Vista sin conocer el estado respecto de hoy (cuando el tramo no está en el historial): no afirma vigencia ni edición. */
    public static RegimenTributarioVistaDTO de(RegimenTributario t, String usuarioNombre) {
        return new RegimenTributarioVistaDTO(t.getId(), t.getRegimen(), t.getVigenteDesde(), t.getNota(), t.getUsuarioId(),
            usuarioNombre, t.getFechaRegistro(), false, false, false, null);
    }

    public static RegimenTributarioVistaDTO de(TramoEstado e, String usuarioNombre) {
        RegimenTributario t = e.tramo();
        return new RegimenTributarioVistaDTO(t.getId(), t.getRegimen(), t.getVigenteDesde(), t.getNota(), t.getUsuarioId(),
            usuarioNombre, t.getFechaRegistro(), e.vigente(), e.proximo(), e.editable(), e.bloqueo());
    }
}
