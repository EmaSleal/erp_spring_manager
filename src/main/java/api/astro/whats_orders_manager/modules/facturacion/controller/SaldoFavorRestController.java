package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.dto.SaldoFavorDTO;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.SaldoFavorConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.SaldoFavorPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.AplicacionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.service.SaldoFavorService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
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

import java.math.BigDecimal;
import java.util.NoSuchElementException;

/**
 * Saldos a favor del cliente (T-048, ADR-053, ADR-057): consulta para el modal de pago y el estado de cuenta,
 * aplicación manual a otra factura y reversión. Bajo /api/facturas/** (SecurityConfig: ADMIN, USER, VENDEDOR) más el
 * permiso de pagos: ver (consultas), crear (aplicar) y anular (reversar, el mismo que anular un pago).
 *
 * Errores: 409 con {@code {success:false, message}} y mensaje fijo (validaciones, TC del día, parámetro contable), 400
 * (formato, razón fuera de 3 a 180 caracteres), 404 (no existe), 500 genérico (el detalle queda en el log).
 */
@RestController
@RequestMapping("/api/facturas/saldos-favor")
@RequiredArgsConstructor
@Slf4j
public class SaldoFavorRestController {

    static final String MENSAJE_FORMATO = "El pedido no tiene el formato esperado";
    static final String MENSAJE_ERROR = "No se pudo completar la operación con el saldo a favor; intente de nuevo o "
        + "contacte al administrador";

    private static final String PUEDE_VER =
        "@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER') "
            + "or @permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_CREAR')";

    private final SaldoFavorService service;
    private final UsuarioService usuarioService;

    /** Cuerpo de la aplicación: factura destino y monto (en la moneda del saldo, dos decimales). */
    public record AplicarSaldoRequest(Integer idFactura, BigDecimal monto) {
    }

    /** Cuerpo de la reversión: razón de 3 a 180 caracteres. */
    public record ReversarSaldoRequest(String razon) {
    }

    /** Saldos VIGENTES del cliente en la moneda (CRC, USD). */
    @GetMapping
    @PreAuthorize(PUEDE_VER)
    public ResponseEntity<ResponseDTO> listar(@RequestParam Integer idCliente, @RequestParam String moneda,
                                              Authentication authentication) {
        return ResponseEntity.ok(ResponseDTO.success("OK", service.listarVigentes(idCliente, moneda)));
    }

    /** Para el modal de pago de la factura: saldos VIGENTES de su cliente y moneda (sin los que ella originó). */
    @GetMapping("/para-factura/{idFactura}")
    @PreAuthorize(PUEDE_VER)
    public ResponseEntity<ResponseDTO> paraFactura(@PathVariable Integer idFactura, Authentication authentication) {
        return ResponseEntity.ok(ResponseDTO.success("OK", service.paraFactura(idFactura)));
    }

    /** Todos los saldos del cliente (VIGENTES y AGOTADOS, por moneda) con sus aplicaciones. */
    @GetMapping("/cliente/{idCliente}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER')")
    public ResponseEntity<ResponseDTO> porCliente(@PathVariable Integer idCliente, Authentication authentication) {
        return ResponseEntity.ok(ResponseDTO.success("OK", service.porCliente(idCliente)));
    }

    @PostMapping("/{idSaldo}/aplicaciones")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_CREAR')")
    public ResponseEntity<ResponseDTO> aplicar(@PathVariable Long idSaldo, @RequestBody AplicarSaldoRequest pedido,
                                               Authentication authentication) {
        if (pedido == null) {
            throw new SaldoFavorPedidoInvalidoException("La factura y el monto son obligatorios");
        }
        AplicacionSaldoFavor aplicacion = service.aplicar(idSaldo, pedido.idFactura(), pedido.monto(),
            idUsuario(authentication));
        SaldoFavorDTO.Aplicacion resumen = service.resumen(aplicacion.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDTO.success("Saldo a favor aplicado", resumen));
    }

    @PostMapping("/aplicaciones/{idAplicacion}/reversion")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_ANULAR')")
    public ResponseEntity<ResponseDTO> reversar(@PathVariable Long idAplicacion,
                                                @RequestBody ReversarSaldoRequest pedido,
                                                Authentication authentication) {
        AplicacionSaldoFavor aplicacion = service.reversar(idAplicacion, pedido != null ? pedido.razon() : null,
            idUsuario(authentication));
        return ResponseEntity.ok(ResponseDTO.success("Aplicación reversada", service.resumen(aplicacion.getId())));
    }

    private Integer idUsuario(Authentication authentication) {
        return usuarioService.findByEmail(authentication.getName()).map(Usuario::getIdUsuario).orElse(null);
    }

    @ExceptionHandler({SaldoFavorConflictoException.class, NotaFacturaConflictoException.class})
    public ResponseEntity<ResponseDTO> conflicto(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
    }

    @ExceptionHandler(SaldoFavorPedidoInvalidoException.class)
    public ResponseEntity<ResponseDTO> pedidoInvalido(SaldoFavorPedidoInvalidoException e) {
        return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ResponseDTO> formatoInvalido(Exception e) {
        log.warn("Pedido de saldo a favor con formato inválido: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ResponseDTO.error(MENSAJE_FORMATO));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseDTO> noExiste(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(e.getMessage()));
    }

    /** Errores no previstos: mensaje genérico, detalle en el log. */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class, DataAccessException.class})
    public ResponseEntity<ResponseDTO> errorInesperado(RuntimeException e) {
        log.error("Error con un saldo a favor", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.error(MENSAJE_ERROR));
    }
}
