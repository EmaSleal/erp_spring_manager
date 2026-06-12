package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.dto.ParametroSistemaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.mapper.ParametroSistemaMapper;
import api.astro.whats_orders_manager.modules.configuracion.enums.CategoriaParametro;
import api.astro.whats_orders_manager.modules.configuracion.model.ParametroSistema;
import api.astro.whats_orders_manager.modules.configuracion.service.ParametroSistemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * PARAMETRO SISTEMA REST CONTROLLER
 * ERP Orders Manager
 * ============================================================================
 * Controlador REST para gestión de parámetros del sistema
 * Endpoints protegidos por autenticación
 *
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 4 - Fase 1.5
 * ============================================================================
 */
@RestController
@RequestMapping("/api/configuracion/parametros")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ParametroSistemaRestController {

    private final ParametroSistemaService parametroService;
    private final ParametroSistemaMapper mapper;

    /**
     * Obtiene todos los parámetros del sistema
     *
     * GET /api/configuracion/parametros
     */
    @GetMapping
    public ResponseEntity<?> obtenerTodos() {
        try {
            log.info("GET /api/configuracion/parametros - Obteniendo todos los parámetros");

            List<ParametroSistema> parametros = parametroService.obtenerTodos();
            List<ParametroSistemaDTO> dtos = parametros.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dtos,
                    "total", dtos.size()
            ));

        } catch (Exception e) {
            log.error("Error al obtener parámetros", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener parámetros: " + e.getMessage()
                    ));
        }
    }

    /**
     * Obtiene parámetros por categoría
     *
     * GET /api/configuracion/parametros/categoria/{categoria}
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<?> obtenerPorCategoria(@PathVariable String categoria) {
        try {
            log.info("GET /api/configuracion/parametros/categoria/{} - Obteniendo parámetros", categoria);

            CategoriaParametro cat = CategoriaParametro.valueOf(categoria.toUpperCase());
            List<ParametroSistema> parametros = parametroService.obtenerPorCategoria(cat);
            List<ParametroSistemaDTO> dtos = parametros.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dtos,
                    "total", dtos.size()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Categoría inválida: {}", categoria);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Categoría inválida: " + categoria
                    ));
        } catch (Exception e) {
            log.error("Error al obtener parámetros por categoría", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener parámetros: " + e.getMessage()
                    ));
        }
    }

    /**
     * Obtiene un parámetro por su clave
     *
     * GET /api/configuracion/parametros/{clave}
     */
    @GetMapping("/{clave}")
    public ResponseEntity<?> obtenerPorClave(@PathVariable String clave) {
        try {
            log.info("GET /api/configuracion/parametros/{} - Obteniendo parámetro", clave);

            return parametroService.obtenerPorClave(clave)
                    .map(parametro -> ResponseEntity.ok(Map.of(
                            "success", true,
                            "data", mapper.toDTO(parametro)
                    )))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                    "success", false,
                                    "message", "Parámetro no encontrado: " + clave
                            )));

        } catch (Exception e) {
            log.error("Error al obtener parámetro por clave", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener parámetro: " + e.getMessage()
                    ));
        }
    }

    /**
     * Obtiene solo los parámetros editables
     *
     * GET /api/configuracion/parametros/editables
     */
    @GetMapping("/editables/lista")
    public ResponseEntity<?> obtenerEditables() {
        try {
            log.info("GET /api/configuracion/parametros/editables - Obteniendo parámetros editables");

            List<ParametroSistema> parametros = parametroService.obtenerEditables();
            List<ParametroSistemaDTO> dtos = parametros.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dtos,
                    "total", dtos.size()
            ));

        } catch (Exception e) {
            log.error("Error al obtener parámetros editables", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener parámetros: " + e.getMessage()
                    ));
        }
    }

    /**
     * Crea un nuevo parámetro
     *
     * POST /api/configuracion/parametros
     */
    @PostMapping
    public ResponseEntity<?> crearParametro(@RequestBody ParametroSistemaDTO dto) {
        try {
            log.info("POST /api/configuracion/parametros - Creando parámetro: {}", dto.getClave());

            if (dto.getClave() == null || dto.getClave().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "La clave del parámetro es obligatoria"
                        ));
            }

            ParametroSistema parametro = parametroService.crearParametro(
                    dto.getClave(),
                    dto.getValor(),
                    dto.getTipoDato(),
                    dto.getCategoria(),
                    dto.getDescripcion(),
                    dto.getEditable()
            );

            log.info("Parámetro creado: {}", parametro.getClave());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Parámetro creado exitosamente",
                            "data", mapper.toDTO(parametro)
                    ));

        } catch (IllegalArgumentException e) {
            log.error("Error de validación al crear parámetro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al crear parámetro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al crear parámetro: " + e.getMessage()
                    ));
        }
    }

    /**
     * Actualiza el valor de un parámetro
     *
     * PUT /api/configuracion/parametros/{clave}
     * PATCH /api/configuracion/parametros/{clave}
     */
    @RequestMapping(value = "/{clave}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> actualizarValor(
            @PathVariable String clave,
            @RequestBody Map<String, String> request) {
        try {
            String nuevoValor = request.get("valor");

            if (nuevoValor == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "El valor es obligatorio"
                        ));
            }

            log.info("PUT/PATCH /api/configuracion/parametros/{} - Actualizando valor", clave);

            ParametroSistema actualizado = parametroService.actualizarValor(clave, nuevoValor);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Parámetro actualizado exitosamente",
                    "data", mapper.toDTO(actualizado)
            ));

        } catch (IllegalArgumentException e) {
            log.error("Error al actualizar parámetro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al actualizar parámetro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al actualizar parámetro: " + e.getMessage()
                    ));
        }
    }

    /**
     * Elimina un parámetro
     *
     * DELETE /api/configuracion/parametros/{clave}
     */
    @DeleteMapping("/{clave}")
    public ResponseEntity<?> eliminarParametro(@PathVariable String clave) {
        try {
            log.info("DELETE /api/configuracion/parametros/{} - Eliminando parámetro", clave);

            parametroService.eliminarParametro(clave);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Parámetro eliminado exitosamente"
            ));

        } catch (IllegalArgumentException e) {
            log.error("Error al eliminar parámetro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al eliminar parámetro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al eliminar parámetro: " + e.getMessage()
                    ));
        }
    }

    /**
     * Inicializa los parámetros por defecto del sistema
     *
     * POST /api/configuracion/parametros/inicializar
     */
    @PostMapping("/inicializar")
    public ResponseEntity<?> inicializarParametros() {
        try {
            log.info("POST /api/configuracion/parametros/inicializar - Inicializando parámetros por defecto");

            parametroService.inicializarParametrosPorDefecto();

            List<ParametroSistema> parametros = parametroService.obtenerTodos();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Parámetros inicializados exitosamente",
                    "total", parametros.size()
            ));

        } catch (Exception e) {
            log.error("Error al inicializar parámetros", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al inicializar parámetros: " + e.getMessage()
                    ));
        }
    }
}
