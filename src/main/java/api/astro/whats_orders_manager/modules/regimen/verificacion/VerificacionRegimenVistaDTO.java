package api.astro.whats_orders_manager.modules.regimen.verificacion;

import java.time.LocalDateTime;

/** Una verificación tal como la devuelve la API (T-056). */
public record VerificacionRegimenVistaDTO(Long id, LocalDateTime fechaHora, String regimenLocal, Integer codigoHacienda,
                                          String descripcionHacienda, String regimenHacienda, ResultadoVerificacion resultado,
                                          String detalle, Integer usuarioId, String usuarioNombre) {

    public static VerificacionRegimenVistaDTO de(VerificacionRegimen v, String usuarioNombre) {
        return new VerificacionRegimenVistaDTO(v.getId(), v.getFechaHora(), v.getRegimenLocal(), v.getCodigoHacienda(),
            v.getDescripcionHacienda(), v.getRegimenHacienda(), v.getResultado(), v.getDetalle(), v.getUsuarioId(), usuarioNombre);
    }
}
