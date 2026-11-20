package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.SustitucionRefDTO;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Crea la factura sustituta de una factura anulada por nota de crédito (ADR-045 §1, T-039). La sustituta es una factura
 * nueva sin comprobante, editable como cualquier otra (ADR-030), con cliente, líneas y condiciones copiados y ligada a
 * la original. Pasa por {@link FacturaService#save} (el número) y por las líneas de la pantalla (totales y asiento de
 * venta), como toda factura nueva.
 */
@Service
@RequiredArgsConstructor
public class FacturaSustitutaService {

    public static final int RAZON_MIN = 3;
    public static final int RAZON_MAX = 180;
    public static final String MENSAJE_RAZON_INVALIDA = "La razón debe tener entre 3 y 180 caracteres";

    private final FacturaRepository facturaRepository;
    private final FacturaService facturaService;
    private final LineaFacturaService lineaFacturaService;
    private final SustitucionElegibilidad elegibilidad;
    private final AtribucionFiscalService atribucionFiscal;

    /**
     * @param idOriginal factura anulada que se sustituye
     * @param razon      razón de la referencia (3 a 180 caracteres); null o en blanco usa el texto por defecto
     * @return la factura sustituta ya guardada
     * @throws NoSuchElementException           la factura no existe (404)
     * @throws NotaFacturaPedidoInvalidoException la razón no cumple el largo (400)
     * @throws NotaFacturaConflictoException    la factura no admite sustituta o ya tiene una (409)
     */
    @Transactional
    public Factura crear(Integer idOriginal, String razon) {
        Factura original = facturaRepository.findByIdParaActualizar(idOriginal)
            .orElseThrow(() -> new NoSuchElementException("Factura no encontrada"));
        SustitucionElegibilidad.Evaluacion evaluacion = elegibilidad.evaluar(original);
        if (!evaluacion.elegible()) {
            throw new NotaFacturaConflictoException(evaluacion.mensaje());
        }
        // T-044 (P-099): el comprobante de la original debe ser del ambiente activo; 409 antes de crear la factura
        // sustituta y, por tanto, de cualquier consecutivo
        elegibilidad.motivoAmbiente(evaluacion).ifPresent(motivo -> {
            throw new NotaFacturaConflictoException(motivo);
        });
        String razonFinal = razonFinal(razon, original.getNumeroFactura(), evaluacion.numeroNota());

        Factura sustituta = new Factura();
        sustituta.setCliente(original.getCliente());
        sustituta.setMonedaFE(original.getMonedaFE());
        sustituta.setTipoCambio(original.getTipoCambio());
        sustituta.setCondicionVentaFE(original.getCondicionVentaFE());
        sustituta.setMedioPagoFE(original.getMedioPagoFE());
        sustituta.setPlazoCredito(original.getPlazoCredito());
        sustituta.setFacturaSustituida(original);
        sustituta.setRazonSustitucion(razonFinal);
        sustituta.setCodigoSustitucion(evaluacion.codigoSustitucion());
        Factura guardada;
        try {
            guardada = facturaService.save(sustituta);
        } catch (DataIntegrityViolationException e) {
            // Carrera con otra sustituta de la misma original: la restricción única de V44
            throw new NotaFacturaConflictoException(
                SustitucionElegibilidad.MENSAJE_YA_TIENE_SUSTITUTA + numeroDeLaExistente(idOriginal));
        }
        // ADR-050: fecha del asiento y mes del IVA, fijos desde la creación. La fecha de emisión de la sustituta es su
        // createDate (auditoría); se deriva de ella tras guardar para que fecha contable y emisión no puedan divergir.
        CodigoReferenciaNota codigo = CodigoReferenciaNota.deCodigo(evaluacion.codigoSustitucion()).orElseThrow();
        LocalDate emision = guardada.getFechaEmision();
        guardada.setFechaContable(atribucionFiscal.fechaContable(emision));
        guardada.setPeriodoFiscalAtribucion(atribucionFiscal.periodoTexto(codigo, emision,
            evaluacion.comprobanteOriginal().getFechaEmision()));
        AtribucionFiscalService.exigirAtribucion(guardada);
        facturaRepository.save(guardada);
        // Las líneas entran por el mismo camino que la pantalla (primera carga de una factura sin líneas): se calculan
        // los totales y se crea el asiento de venta una sola vez. Cargarlas con la cabecera registraría de nuevo las
        // salidas de inventario de una venta que la original ya registró (la devolución es de v1.1).
        List<LineaFacturaR> lineas = new ArrayList<>();
        int nuevo = 0;
        for (LineaFactura l : original.getLineas()) {
            lineas.add(new LineaFacturaR(--nuevo, l.getNumeroLinea(), l.getProducto().getIdProducto(),
                guardada.getIdFactura(), null, l.getCantidad(), l.getPrecioUnitario(), null, null, null, null, null));
        }
        if (!lineas.isEmpty()) {
            lineaFacturaService.updateLineas(lineas);
        }
        return guardada;
    }

    private String numeroDeLaExistente(Integer idOriginal) {
        try {
            return facturaRepository.findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(idOriginal)
                .map(Factura::getNumeroFactura).orElse("existente");
        } catch (RuntimeException e) {
            return "existente";
        }
    }

    /** Razón validada (3 a 180) o el texto por defecto, truncado a 180. */
    static String razonFinal(String razon, String numeroFactura, String numeroNota) {
        if (razon == null || razon.isBlank()) {
            String porDefecto = "Sustituye la factura " + numeroFactura + " anulada por la nota " + numeroNota;
            return porDefecto.length() > RAZON_MAX ? porDefecto.substring(0, RAZON_MAX) : porDefecto;
        }
        String limpia = razon.trim();
        if (limpia.length() < RAZON_MIN || limpia.length() > RAZON_MAX) {
            throw new NotaFacturaPedidoInvalidoException(MENSAJE_RAZON_INVALIDA);
        }
        return limpia;
    }

    /** Completa el detalle con la bandera de la acción y los vínculos de sustitución (ADR-045). */
    @Transactional(readOnly = true)
    public void completarDetalle(Factura factura, FacturaDetalleDTO detalle) {
        detalle.setPuedeEmitirSustituta(elegibilidad.puedeEmitirSustituta(factura));
        detalle.setMotivoSustitutaNoDisponible(elegibilidad.motivoSustitutaNoDisponible(factura).orElse(null));
        // T-044: ambiente del comprobante vigente para el bloque FE del modal
        elegibilidad.ambienteDeLaFactura(factura).ifPresent(a -> {
            detalle.setAmbienteComprobante(a.ambiente());
            detalle.setAmbienteComprobanteCoincide(a.coincide());
            detalle.setMotivoAmbienteComprobante(a.motivo());
        });
        Factura original = factura.getFacturaSustituida();
        if (original != null) {
            detalle.setFechaContable(factura.getFechaContable());
            detalle.setPeriodoFiscalAtribucion(factura.getPeriodoFiscalAtribucion());
            SustitucionElegibilidad.Evaluacion e = elegibilidad.evaluarSinSustituta(original);
            detalle.setSustituye(new SustitucionRefDTO(original.getIdFactura(), original.getNumeroFactura(),
                e.elegible() ? e.numeroNota() : null));
        }
        facturaRepository.findFirstByFacturaSustituida_IdFacturaAndAnuladaFalse(factura.getIdFactura())
            .ifPresent(s -> detalle.setSustituidaPor(new SustitucionRefDTO(s.getIdFactura(), s.getNumeroFactura(),
                null)));
    }
}
