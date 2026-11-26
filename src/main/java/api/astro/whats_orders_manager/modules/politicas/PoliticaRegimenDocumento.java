package api.astro.whats_orders_manager.modules.politicas;

import api.astro.whats_orders_manager.modules.facturacion.exception.FacturaRegimenConflictoException;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Sello del régimen de un documento de venta (ADR-072, ADR-073 Opción A): el régimen de una factura es el vigente en la
 * fecha del documento (ADR-014) cuando se crea, y no se recalcula. Es el único punto que pregunta el régimen para sellar.
 */
@Component
@RequiredArgsConstructor
public class PoliticaRegimenDocumento {

    public static final String MENSAJE_CAMBIO_DE_REGIMEN =
        "La nueva fecha de la factura pertenece a un tramo de otro régimen tributario; la factura conserva el régimen con el "
            + "que se creó y su fecha no puede pasar al otro régimen";

    private final RegimenTributarioService regimenes;

    /**
     * Régimen vigente en la fecha del documento, para sellarlo al crear.
     *
     * @throws RegimenNoDefinidoException ningún tramo rige en esa fecha (nunca se supone un régimen)
     */
    public Regimen sellar(LocalDate fechaDocumento) {
        return regimenes.vigenteEn(fechaDocumento).getRegimen();
    }

    /**
     * ADR-076 §1: un documento derivado (la factura sustituta) hereda el régimen de su origen en lugar de sellarse por su
     * propia fecha: la sustituta de una factura GENERAL de diciembre creada en enero sigue siendo GENERAL.
     */
    public Regimen heredar(api.astro.whats_orders_manager.modules.facturacion.model.Factura origen) {
        return origen.getRegimenEmisor();
    }

    /**
     * Una edición que mueve la fecha del documento no puede dejarla en un tramo de otro régimen que el sellado.
     *
     * @throws FacturaRegimenConflictoException la nueva fecha cae en un tramo de otro régimen (409)
     * @throws RegimenNoDefinidoException       ningún tramo rige en la nueva fecha
     */
    public void exigirMismoRegimen(Regimen sellado, LocalDate nuevaFecha) {
        if (sellar(nuevaFecha) != sellado) {
            throw new FacturaRegimenConflictoException(MENSAJE_CAMBIO_DE_REGIMEN);
        }
    }
}
