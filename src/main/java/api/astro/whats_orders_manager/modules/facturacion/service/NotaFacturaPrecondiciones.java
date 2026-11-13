package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bloqueo de una factura mientras tiene una nota de crédito EN_TRAMITE (ADR-037 §3): no admite pagos nuevos ni
 * confirmar o conciliar los existentes, porque al aceptarse la nota la factura queda anulada. Único punto de la regla
 * (T-032, criterio 30). Anular, rechazar o eliminar un pago no pasa por aquí.
 */
@Component
@RequiredArgsConstructor
public class NotaFacturaPrecondiciones {

    private final NotaFacturaRepository notaRepository;

    /**
     * @param operacion lo que se intenta, para el mensaje (p. ej. "registrar un pago")
     * @throws NotaFacturaConflictoException (409) si la factura tiene una nota EN_TRAMITE
     */
    public void exigirSinNotaEnTramite(Integer idFactura, String operacion) {
        if (idFactura == null) {
            return;
        }
        notaRepository.findFirstByFactura_IdFacturaAndEstado(idFactura, EstadoNotaFactura.EN_TRAMITE)
            .ifPresent(nota -> {
                throw new NotaFacturaConflictoException(mensaje(nota, operacion));
            });
    }

    static String mensaje(NotaFactura nota, String operacion) {
        return "No se puede " + operacion + ": la factura tiene la nota de crédito " + nota.getId()
            + " en trámite ante Hacienda. Espere a que Hacienda la resuelva: si la acepta, la factura queda anulada; "
            + "si la rechaza, la factura vuelve a admitir pagos.";
    }
}
