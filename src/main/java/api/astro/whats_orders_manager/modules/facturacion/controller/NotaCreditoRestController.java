package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.dto.EmitirNotaCreditoRequest;
import api.astro.whats_orders_manager.modules.facturacion.dto.NotaFacturaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.NotaFacturaService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * API de notas de crédito electrónicas (ADR-037, T-032): emitir la nota total de una factura con comprobante ACEPTADO
 * y consultarla. Emitir exige el mismo permiso que anular una factura (FACTURA_ANULAR).
 *
 * Respuestas: 201 al emitir; 400 pedido inválido; 403 sin permiso; 404 factura o nota inexistente; 409 la factura no
 * admite la nota. Los mensajes son los del dominio; un error inesperado responde un mensaje genérico (el detalle queda
 * en el log).
 */
@RestController
@RequestMapping("/api/facturas/notas-credito")
@RequiredArgsConstructor
@Slf4j
public class NotaCreditoRestController {

    static final String MENSAJE_FORMATO = "El pedido no tiene el formato esperado";
    static final String MENSAJE_FACTURA_NO_EXISTE = "La factura no existe";
    static final String MENSAJE_NOTA_NO_EXISTE = "La nota no existe";
    static final String MENSAJE_ERROR = "No se pudo emitir la nota de crédito; intente de nuevo o contacte al "
        + "administrador";

    private final NotaFacturaService notaService;
    private final ComprobanteElectronicoService comprobanteService;

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_ANULAR')")
    public ResponseEntity<ResponseDTO> emitir(@RequestBody EmitirNotaCreditoRequest pedido,
                                              Authentication authentication) {
        if (pedido == null || pedido.idFactura() == null) {
            throw new NotaFacturaPedidoInvalidoException("La factura es obligatoria");
        }
        NotaFactura nota = notaService.emitirNotaCreditoTotal(pedido.idFactura(), pedido.tipo(),
            pedido.codigoReferencia(), pedido.razon(), authentication.getName());

        NotaFacturaDTO emitida = consultar(nota.getId());
        if (emitida.idComprobante() != null && emitida.estadoComprobante() == EstadoComprobante.FIRMADO) {
            // Mismo flujo que la factura: envío a Hacienda y sondeo; clasifica su propio resultado y no lanza
            comprobanteService.procesarFacturaAsync(emitida.idComprobante());
            emitida = consultar(nota.getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.success("Nota de crédito emitida", emitida));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public ResponseEntity<ResponseDTO> obtener(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ResponseDTO.success("OK", consultar(id)));
    }

    private NotaFacturaDTO consultar(Long id) {
        return notaService.obtener(id)
            .map(n -> NotaFacturaDTO.de(n.nota(), n.comprobante()))
            .orElseThrow(NotaNoEncontradaException::new);
    }

    /** La nota pedida no existe (404). */
    static class NotaNoEncontradaException extends RuntimeException {
    }

    @ExceptionHandler(NotaFacturaPedidoInvalidoException.class)
    public ResponseEntity<ResponseDTO> pedidoInvalido(NotaFacturaPedidoInvalidoException e) {
        return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ResponseDTO> formatoInvalido(Exception e) {
        log.warn("Pedido de nota de crédito con formato inválido: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ResponseDTO.error(MENSAJE_FORMATO));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseDTO> facturaNoEncontrada(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(MENSAJE_FACTURA_NO_EXISTE));
    }

    @ExceptionHandler(NotaNoEncontradaException.class)
    public ResponseEntity<ResponseDTO> notaNoEncontrada(NotaNoEncontradaException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(MENSAJE_NOTA_NO_EXISTE));
    }

    @ExceptionHandler(NotaFacturaConflictoException.class)
    public ResponseEntity<ResponseDTO> conflicto(NotaFacturaConflictoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
    }

    /** Errores no previstos (configuración de Hacienda, firma, BD): mensaje genérico, detalle en el log. */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class,
        DataAccessException.class})
    public ResponseEntity<ResponseDTO> errorInesperado(RuntimeException e) {
        log.error("Error emitiendo o consultando una nota de crédito", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.error(MENSAJE_ERROR));
    }
}
