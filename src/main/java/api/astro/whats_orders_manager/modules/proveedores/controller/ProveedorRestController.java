package api.astro.whats_orders_manager.modules.proveedores.controller;

import api.astro.whats_orders_manager.modules.proveedores.dto.ProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.exception.CedulaProveedorDuplicadaException;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * API de proveedores. T-030 (ADR-035 §3): ningún texto de excepción llega al cliente; los errores de validación
 * responden 400 con un mensaje por campo y los inesperados, 500 genérico registrado en el log.
 */
@Slf4j
@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorRestController {

    static final String MENSAJE_DUPLICADO = "Ya existe un proveedor con la cédula indicada";
    static final String MENSAJE_NO_ENCONTRADO = "Proveedor no encontrado";
    static final String MENSAJE_CONFLICTO = "Los datos del proveedor entran en conflicto con un registro existente";
    static final String MENSAJE_FORMATO = "Los datos enviados no tienen un formato válido";
    static final String MENSAJE_INESPERADO = "Error inesperado al procesar el proveedor; intente de nuevo";

    private final ProveedorService proveedorService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_VER')")
    public ResponseEntity<ResponseDTO> listar(
            @RequestParam(defaultValue = "false") boolean soloActivos,
            Authentication authentication
    ) {
        List<Proveedor> lista = soloActivos
            ? proveedorService.findActivos()
            : proveedorService.findAll();
        return ResponseEntity.ok(ResponseDTO.success("OK", lista));
    }

    @GetMapping("/buscar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_VER')")
    public ResponseEntity<ResponseDTO> buscar(
            @RequestParam String q,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success("OK", proveedorService.buscar(q)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_VER')")
    public ResponseEntity<ResponseDTO> findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success("OK", proveedorService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_CREAR')")
    public ResponseEntity<ResponseDTO> crear(
            @Valid @RequestBody ProveedorDTO dto,
            Authentication authentication
    ) {
        Proveedor p = proveedorService.crear(dto);
        return ResponseEntity.ok(ResponseDTO.success("Proveedor creado exitosamente", p));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_EDITAR')")
    public ResponseEntity<ResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorDTO dto,
            Authentication authentication
    ) {
        Proveedor p = proveedorService.actualizar(id, dto);
        return ResponseEntity.ok(ResponseDTO.success("Proveedor actualizado exitosamente", p));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_ELIMINAR')")
    public ResponseEntity<ResponseDTO> desactivar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        proveedorService.desactivar(id);
        return ResponseEntity.ok(ResponseDTO.success("Proveedor desactivado exitosamente"));
    }

    // ==================== ERRORES (T-030) ====================

    /** 400 con el mensaje de cada campo inválido; data = {campo: mensaje}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO> datosInvalidos(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ResponseDTO.error(String.join("; ", campos.values()), campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ResponseDTO> formatoInvalido(Exception e) {
        log.warn("Petición de proveedor ilegible: {}", e.getClass().getSimpleName());
        return ResponseEntity.badRequest().body(ResponseDTO.error(MENSAJE_FORMATO));
    }

    @ExceptionHandler(CedulaProveedorDuplicadaException.class)
    public ResponseEntity<ResponseDTO> cedulaDuplicada(CedulaProveedorDuplicadaException e) {
        return ResponseEntity.badRequest().body(ResponseDTO.error(MENSAJE_DUPLICADO));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseDTO> noEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseDTO.error(MENSAJE_NO_ENCONTRADO));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseDTO> conflicto(DataIntegrityViolationException e) {
        log.warn("Restricción de BD violada al guardar un proveedor", e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ResponseDTO.error(MENSAJE_CONFLICTO));
    }

    /** Un rechazo de @PreAuthorize no es un error inesperado: sigue hacia Spring Security (403 JSON, ADR-031 §2). */
    @ExceptionHandler(AccessDeniedException.class)
    public void accesoDenegado(AccessDeniedException e) {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> inesperado(Exception e) {
        if (e instanceof ErrorResponse respuesta && respuesta.getStatusCode().is4xxClientError()) {
            // Errores de la petición que Spring ya clasifica (parámetro ausente, método o tipo de contenido no admitido)
            return ResponseEntity.status(respuesta.getStatusCode()).body(ResponseDTO.error(MENSAJE_FORMATO));
        }
        log.error("Error inesperado en la API de proveedores", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseDTO.error(MENSAJE_INESPERADO));
    }
}
