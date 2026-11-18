package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Regla única de «¿se puede emitir una factura sustituta de esta factura?» (ADR-045 §1 y §3). La usan el endpoint, la
 * bandera de la pantalla y la etapa a de la emisión (antes del consecutivo).
 *
 * Procede si la factura está anulada por una nota de crédito total APLICADA con código 01 o 13, la nota tiene su
 * comprobante ACEPTADO y el comprobante de la factura al que referencia está ACEPTADO con clave. El código de la
 * sustituta se deduce de la nota: 07 para 01 y 15 para 13.
 */
@Component
@RequiredArgsConstructor
public class SustitucionElegibilidad {

    public static final String MENSAJE_NO_ANULADA =
        "La factura no está anulada por una nota de crédito total aceptada";
    public static final String MENSAJE_NOTA_NO_ACEPTADA =
        "La nota de crédito de la factura no está aceptada por Hacienda";
    public static final String MENSAJE_ORIGEN_NO_ACEPTADO =
        "El comprobante de la factura original no está aceptado por Hacienda";
    public static final String MENSAJE_YA_TIENE_SUSTITUTA = "La factura ya tiene una sustituta: ";
    public static final String MENSAJE_ORIGINAL_YA_NO_CUMPLE =
        "La factura que esta sustituye ya no cumple las condiciones para ser sustituida; la factura no se emitió";

    private final NotaFacturaRepository notaRepository;
    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final FacturaRepository facturaRepository;

    /**
     * Resultado de la evaluación: o es elegible (con los datos para armar la sustituta) o trae el mensaje fijo del
     * motivo.
     */
    public record Evaluacion(boolean elegible, String mensaje, NotaFactura nota, String numeroNota,
                             String codigoSustitucion, ComprobanteElectronico comprobanteOriginal) {

        static Evaluacion no(String mensaje) {
            return new Evaluacion(false, mensaje, null, null, null, null);
        }
    }

    /** Evaluación completa, con la regla «una sola sustituta no anulada». */
    public Evaluacion evaluar(Factura original) {
        Evaluacion base = evaluarSinSustituta(original);
        if (!base.elegible()) {
            return base;
        }
        Optional<Factura> existente = facturaRepository
            .findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(original.getIdFactura());
        if (existente.isPresent()) {
            return Evaluacion.no(MENSAJE_YA_TIENE_SUSTITUTA + existente.get().getNumeroFactura());
        }
        return base;
    }

    /** Igual que {@link #evaluar} sin mirar si ya existe una sustituta (la etapa a evalúa a la propia sustituta). */
    public Evaluacion evaluarSinSustituta(Factura original) {
        if (original == null || !Boolean.TRUE.equals(original.getAnulada())) {
            return Evaluacion.no(MENSAJE_NO_ANULADA);
        }
        Optional<NotaFactura> aplicada = notaRepository.findByFactura_IdFacturaOrderByIdAsc(original.getIdFactura())
            .stream()
            .filter(n -> n.getTipo() == TipoNotaFactura.CREDITO && n.getEstado() == EstadoNotaFactura.APLICADA)
            .filter(n -> CodigoReferenciaNota.ANULA_DOCUMENTO.getCodigo().equals(n.getCodigoReferencia())
                || CodigoReferenciaNota.ANULA_DOCUMENTO_ERROR_MATERIAL.getCodigo().equals(n.getCodigoReferencia()))
            .findFirst();
        if (aplicada.isEmpty()) {
            return Evaluacion.no(MENSAJE_NO_ANULADA);
        }
        NotaFactura nota = aplicada.get();
        Optional<ComprobanteElectronico> comprobanteNota =
            comprobanteRepository.findFirstByNotaFactura_IdAndVigenteTrue(nota.getId());
        if (comprobanteNota.isEmpty() || comprobanteNota.get().getEstado() != EstadoComprobante.ACEPTADO) {
            return Evaluacion.no(MENSAJE_NOTA_NO_ACEPTADA);
        }
        ComprobanteElectronico origen = nota.getComprobanteReferencia();
        if (origen == null) {
            origen = comprobanteRepository.findByFactura_IdFacturaOrderByIdAsc(original.getIdFactura()).stream()
                .filter(c -> c.getEstado() == EstadoComprobante.ACEPTADO).findFirst().orElse(null);
        }
        if (origen == null || origen.getEstado() != EstadoComprobante.ACEPTADO
            || origen.getClaveNumerica() == null || origen.getClaveNumerica().isBlank()
            || origen.getFechaEmision() == null) {
            return Evaluacion.no(MENSAJE_ORIGEN_NO_ACEPTADO);
        }
        String codigo = CodigoReferenciaNota.ANULA_DOCUMENTO.getCodigo().equals(nota.getCodigoReferencia())
            ? CodigoReferenciaNota.SUSTITUYE_COMPROBANTE.getCodigo()
            : CodigoReferenciaNota.SUSTITUYE_COMPROBANTE_ERROR_MATERIAL.getCodigo();
        return new Evaluacion(true, null, nota, numeroDeNota(nota, comprobanteNota.get()), codigo, origen);
    }

    public boolean puedeEmitirSustituta(Factura original) {
        return evaluar(original).elegible();
    }

    /**
     * Número de la nota de crédito que se muestra al usuario: el consecutivo de su comprobante vigente; si falta,
     * «NC-» y el id de la nota (la nota no tiene número propio).
     */
    public static String numeroDeNota(NotaFactura nota, ComprobanteElectronico comprobanteNota) {
        if (comprobanteNota != null && comprobanteNota.getConsecutivo() != null
            && !comprobanteNota.getConsecutivo().isBlank()) {
            return comprobanteNota.getConsecutivo();
        }
        return "NC-" + nota.getId();
    }
}
