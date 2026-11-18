package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.dto.EmitirSustitutaRequest;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaSustitutaService;
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

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Acción «Emitir factura sustituta» (ADR-045, T-039): crea la factura nueva, editable, que reemplaza a una factura
 * anulada por nota de crédito total. Exige el permiso de crear facturas. 201 con el id y el número de la nueva; 400 razón
 * inválida; 404 factura inexistente; 409 la factura no admite sustituta o ya tiene una. Los mensajes son fijos; un error
 * inesperado responde un texto genérico y el detalle queda en el log.
 */
@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
@Slf4j
public class FacturaSustitutaRestController {

    static final String MENSAJE_FORMATO = "El pedido no tiene el formato esperado";
    static final String MENSAJE_NO_EXISTE = "La factura no existe";
    static final String MENSAJE_ERROR = "No se pudo crear la factura sustituta; intente de nuevo o contacte al "
        + "administrador";

    private final FacturaSustitutaService sustitutaService;

    @PostMapping("/{id}/sustituta")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_CREAR')")
    public ResponseEntity<ResponseDTO> emitir(@PathVariable Integer id,
                                              @RequestBody(required = false) EmitirSustitutaRequest pedido,
                                              Authentication authentication) {
        Factura nueva = sustitutaService.crear(id, pedido == null ? null : pedido.razon());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDTO.success("Factura sustituta creada",
            Map.of("idFactura", nueva.getIdFactura(), "numeroFactura", nueva.getNumeroFactura())));
    }

    @ExceptionHandler(NotaFacturaPedidoInvalidoException.class)
    public ResponseEntity<ResponseDTO> razonInvalida(NotaFacturaPedidoInvalidoException e) {
        return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ResponseDTO> formatoInvalido(Exception e) {
        log.warn("Pedido de factura sustituta con formato inválido: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ResponseDTO.error(MENSAJE_FORMATO));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseDTO> noEncontrada(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(MENSAJE_NO_EXISTE));
    }

    @ExceptionHandler(NotaFacturaConflictoException.class)
    public ResponseEntity<ResponseDTO> conflicto(NotaFacturaConflictoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class, DataAccessException.class})
    public ResponseEntity<ResponseDTO> errorInesperado(RuntimeException e) {
        log.error("Error creando la factura sustituta", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.error(MENSAJE_ERROR));
    }
}
