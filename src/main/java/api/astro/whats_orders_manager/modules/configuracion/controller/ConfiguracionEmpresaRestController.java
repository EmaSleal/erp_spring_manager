package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.configuracion.dto.CabysBusquedaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.ConfiguracionEmpresaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.mapper.ConfiguracionEmpresaMapper;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================================================
 * CONFIGURACION EMPRESA REST CONTROLLER
 * ERP Orders Manager
 * ============================================================================
 * Controlador REST para gestión de configuración de empresa
 * Endpoints protegidos por autenticación
 *
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 4 - Fase 1.5
 * ============================================================================
 */
@RestController
@RequestMapping("/api/configuracion/empresa")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ConfiguracionEmpresaRestController {

    private final ConfiguracionEmpresaService configuracionEmpresaService;
    private final HaciendaApiService haciendaConsultaService;
    private final ConfiguracionEmpresaMapper mapper;

    /**
     * Busca códigos CABYS por descripción o palabra clave
     *
     * GET /api/configuracion/empresa/hacienda/cabys/buscar?q={termino}&top={cantidad}
     */
    @GetMapping("/hacienda/cabys/buscar")
    public ResponseEntity<?> buscarCabys(
            @RequestParam("q") String termino,
            @RequestParam(value = "top", defaultValue = "10") Integer top) {
        try {
            log.info("GET /api/configuracion/empresa/hacienda/cabys/buscar?q={}&top={}", termino, top);

            CabysBusquedaDTO resultado = haciendaConsultaService.buscarCabys(termino, top);

            if (resultado.getExitosa() && resultado.tieneResultados()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", resultado
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", resultado.getMensajeError() != null ?
                            resultado.getMensajeError() : "No se encontraron resultados"
                    ));
            }

        } catch (Exception e) {
            log.error("Error al buscar códigos CABYS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Error al buscar códigos CABYS: " + e.getMessage()
                ));
        }
    }

    /**
     * Consulta datos de un contribuyente en la API de Hacienda Costa Rica
     *
     * GET /api/configuracion/empresa/hacienda/consultar/{numeroIdentificacion}
     */
    @GetMapping("/hacienda/consultar/{numeroIdentificacion}")
    public ResponseEntity<?> consultarHacienda(@PathVariable String numeroIdentificacion) {
        try {
            log.info("GET /api/configuracion/empresa/hacienda/consultar/{} - Consultando API Hacienda",
                numeroIdentificacion);

            HaciendaConsultaDTO resultado = haciendaConsultaService.consultarContribuyente(numeroIdentificacion);

            log.info("Resultado de consulta Hacienda: {}", resultado);

            if (resultado.getExitosa()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", resultado
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "message", resultado.getMensajeError()
                    ));
            }

        } catch (Exception e) {
            log.error("Error al consultar API de Hacienda", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Error al consultar Hacienda: " + e.getMessage()
                ));
        }
    }

    /**
     * Obtiene la configuración de la empresa
     *
     * GET /api/configuracion/empresa
     */
    @GetMapping
    public ResponseEntity<?> obtenerConfiguracion() {
        try {
            log.info("GET /api/configuracion/empresa - Obteniendo configuración de empresa");

            ConfiguracionEmpresa configuracion = configuracionEmpresaService.obtenerOCrearConfiguracion();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", mapper.toDTO(configuracion)
            ));

        } catch (Exception e) {
            log.error("Error al obtener configuración de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Crea una nueva configuración de empresa
     *
     * POST /api/configuracion/empresa
     */
    @PostMapping
    public ResponseEntity<?> crearConfiguracion(@RequestBody ConfiguracionEmpresaDTO dto) {
        try {
            log.info("POST /api/configuracion/empresa - Creando configuración de empresa");

            ConfiguracionEmpresa configuracion = mapper.toEntity(dto);
            ConfiguracionEmpresa guardada = configuracionEmpresaService.saveOrUpdate(configuracion);

            log.info("Configuración de empresa guardada: ID {}", guardada.getIdConfiguracion());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración guardada exitosamente",
                    "data", mapper.toDTO(guardada)
            ));

        } catch (IllegalArgumentException e) {
            log.error("Error de validación al crear configuración: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al crear configuración de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al guardar la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Actualiza la configuración de empresa existente
     *
     * PUT /api/configuracion/empresa
     */
    @PutMapping
    public ResponseEntity<?> actualizarConfiguracion(@RequestBody ConfiguracionEmpresaDTO dto) {
        try {
            log.info("PUT /api/configuracion/empresa - Actualizando configuración de empresa");

            ConfiguracionEmpresa configuracion = mapper.toEntity(dto);
            ConfiguracionEmpresa guardada = configuracionEmpresaService.saveOrUpdate(configuracion);

            log.info("Configuración de empresa actualizada: ID {}", guardada.getIdConfiguracion());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuración actualizada exitosamente",
                    "data", mapper.toDTO(guardada)
            ));

        } catch (IllegalArgumentException e) {
            log.error("Error de validación al actualizar configuración: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error al actualizar configuración de empresa", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al actualizar la configuración: " + e.getMessage()
                    ));
        }
    }

    /**
     * Valida los datos fiscales de la empresa
     *
     * GET /api/configuracion/empresa/validar-fiscales
     */
    @GetMapping("/validar-fiscales")
    public ResponseEntity<?> validarDatosFiscales() {
        try {
            log.info("GET /api/configuracion/empresa/validar-fiscales");

            boolean validos = configuracionEmpresaService.validarDatosFiscales();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "validos", validos,
                    "message", validos ? "Datos fiscales completos" : "Faltan datos fiscales"
            ));

        } catch (Exception e) {
            log.error("Error al validar datos fiscales", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al validar: " + e.getMessage()
                    ));
        }
    }
}
