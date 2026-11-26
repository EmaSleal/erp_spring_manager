package api.astro.whats_orders_manager.modules.compras.controller;

import api.astro.whats_orders_manager.modules.compras.dto.CompraContadoDTO;
import api.astro.whats_orders_manager.modules.compras.service.AdjuntoComprobante;
import api.astro.whats_orders_manager.modules.compras.service.AdjuntoComprobanteService;
import api.astro.whats_orders_manager.modules.compras.service.CompraContadoService;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorDuplicadoException;
import api.astro.whats_orders_manager.modules.contabilidad.service.OrdenCompraNoAdmiteFacturaException;
import api.astro.whats_orders_manager.modules.contabilidad.service.ProrrataNoConfiguradaException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/** Compra de contado y descarga de su adjunto (T-058, ADR-077 §1-2, §7-9). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CompraContadoRestController {

    private final CompraContadoService compraContadoService;
    private final AdjuntoComprobanteService adjuntoService;
    private final UsuarioService usuarioService;

    /**
     * Registra la CxP y su pago en un solo envío: parte JSON {@code datos} y, opcional, el archivo {@code adjunto}. Exige el permiso
     * de crear CxP y el de registrar pagos. 409 con comprobante duplicado, prorrata faltante o fecha sin tramo de régimen; 400 con
     * cualquier otro dato inválido (incluido el adjunto, con mensaje fijo).
     */
    @PostMapping(value = "/compras/contado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_CREAR')"
        + " and @permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_REGISTRAR_PAGO')")
    public ResponseEntity<ResponseDTO> registrar(
            @RequestPart("datos") CompraContadoDTO datos,
            @RequestPart(value = "adjunto", required = false) MultipartFile adjunto,
            Authentication authentication
    ) {
        try {
            AdjuntoComprobante validado = adjuntoService.validar(adjunto);
            Usuario usuario = usuarioService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + authentication.getName()));
            var resultado = compraContadoService.registrar(datos, validado, usuario);
            return ResponseEntity.ok(ResponseDTO.success(
                "Compra de contado registrada: " + resultado.numeroCuentaPorPagar(), resultado));
        } catch (ComprobanteProveedorDuplicadoException | ProrrataNoConfiguradaException
                 | OrdenCompraNoAdmiteFacturaException | RegimenNoDefinidoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage()));
        }
    }

    /** Descarga el adjunto del comprobante de una CxP. Solo con permiso de ver CxP; la ruta sale de lo guardado, no de la petición. */
    @GetMapping("/cuentas-pagar/{id}/adjunto")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CUENTA_PAGAR_VER')")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, Authentication authentication) {
        return adjuntoService.cargar(id)
            .map(d -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.tipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename(d.nombreOriginal(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .<Resource>body(new FileSystemResource(d.archivo())))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
