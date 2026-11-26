package api.astro.whats_orders_manager.modules.politicas;

import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.HoraFiscal;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Política de emisión (ADR-072, ADR-073 §2, ADR-076 §2-3): decide si el ERP puede crear el comprobante electrónico de una
 * factura, según su régimen sellado y el Clock de la aplicación. Tres reglas, ambas con 409 (EmisionNoPermitidaException):
 * <ol>
 *   <li>una factura SIMPLIFICADO nunca se emite a Hacienda;</li>
 *   <li>una factura con la clave de un comprobante emitido fuera del ERP tampoco (evita la doble emisión);</li>
 *   <li>una factura GENERAL tampoco, desde que empieza un tramo SIMPLIFICADO posterior a su fecha de documento y «hoy»
 *       (fecha de Costa Rica del Clock, ADR-060) ya llegó a él: su comprobante se emite en la herramienta de Hacienda y aquí
 *       solo se registra su clave.</li>
 * </ol>
 * Se aplica al crear un comprobante de factura (emitir, procesar, generar, reemitir). Enviar o reenviar un comprobante ya
 * generado no es una emisión nueva (misma clave y fecha) y no pasa por esta política.
 */
@Component
@RequiredArgsConstructor
public class PoliticaEmision {

    public static final String MENSAJE_SIMPLIFICADO = "Las ventas del régimen simplificado no se emiten a Hacienda";
    public static final String MENSAJE_GENERAL_FUERA_DEL_ERP =
        "Esta venta es del régimen general; su comprobante se emite en la herramienta de Hacienda y se registra aquí su clave";

    /** T-057 criterio 15: la factura ya tiene la clave de un comprobante emitido fuera del ERP; emitir otro sería una doble emisión. */
    public static final String MENSAJE_COMPROBANTE_EXTERNO =
        "Esta venta ya tiene un comprobante emitido fuera del ERP; no se emite otro desde el sistema";

    private final RegimenTributarioService regimenes;
    private final Clock clock;

    /** @throws EmisionNoPermitidaException la factura no se puede emitir desde el ERP (409), antes de tocar consecutivos */
    public void exigirEmisible(Factura factura) {
        if (factura.getRegimenEmisor() == Regimen.SIMPLIFICADO) {
            throw new EmisionNoPermitidaException(MENSAJE_SIMPLIFICADO);
        }
        if (factura.getComprobanteExternoClave() != null && !factura.getComprobanteExternoClave().isBlank()) {
            throw new EmisionNoPermitidaException(MENSAJE_COMPROBANTE_EXTERNO);
        }
        LocalDate hoy = HoraFiscal.ahora(clock).toLocalDate();
        LocalDate fechaDocumento = fechaDelOrigenRaiz(factura, hoy);
        if (regimenes.empiezaTramo(Regimen.SIMPLIFICADO, fechaDocumento, hoy)) {
            throw new EmisionNoPermitidaException(MENSAJE_GENERAL_FUERA_DEL_ERP);
        }
    }

    /**
     * ADR-076 §1-2: una sustituta hereda el régimen de su origen y, para la frontera de emisión, también su fecha: se sigue
     * {@code facturaSustituida} hasta el documento raíz (con protección contra ciclos) y se usa la fecha de ese documento.
     */
    private static LocalDate fechaDelOrigenRaiz(Factura factura, LocalDate hoy) {
        Factura raiz = factura;
        java.util.Set<Factura> vistas = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        vistas.add(raiz);
        while (raiz.getFacturaSustituida() != null && vistas.add(raiz.getFacturaSustituida())) {
            raiz = raiz.getFacturaSustituida();
        }
        return raiz.getFechaEmision() != null ? raiz.getFechaEmision() : hoy;
    }
}
