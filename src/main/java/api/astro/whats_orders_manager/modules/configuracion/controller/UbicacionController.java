package api.astro.whats_orders_manager.modules.configuracion.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import api.astro.whats_orders_manager.modules.configuracion.dto.CantonDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.DistritoDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.ProvinciaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.UbicacionCompletaDTO;
import api.astro.whats_orders_manager.modules.configuracion.service.UbicacionService;

import java.util.List;
import java.util.Map;

/**
 * REST Controller para gestión de ubicaciones de Costa Rica
 * Endpoints para provincias, cantones y distritos
 */
@RestController
@RequestMapping("/api/ubicaciones")
@RequiredArgsConstructor
@Slf4j
public class UbicacionController {
    
    private final UbicacionService ubicacionService;
    
    // ========== ENDPOINTS DE PROVINCIAS ==========
    
    /**
     * GET /api/ubicaciones/provincias
     * Obtener todas las provincias
     */
    @GetMapping("/provincias")
    public ResponseEntity<List<ProvinciaDTO>> listarProvincias() {
        log.debug("GET /api/ubicaciones/provincias");
        List<ProvinciaDTO> provincias = ubicacionService.obtenerProvincias();
        return ResponseEntity.ok(provincias);
    }
    
    /**
     * GET /api/ubicaciones/provincias/{codigo}
     * Obtener provincia por código
     */
    @GetMapping("/provincias/{codigo}")
    public ResponseEntity<ProvinciaDTO> obtenerProvincia(@PathVariable String codigo) {
        log.debug("GET /api/ubicaciones/provincias/{}", codigo);
        ProvinciaDTO provincia = ubicacionService.obtenerProvinciaPorCodigo(codigo);
        
        if (provincia != null) {
            return ResponseEntity.ok(provincia);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // ========== ENDPOINTS DE CANTONES ==========
    
    /**
     * GET /api/ubicaciones/cantones?provincia={codigo}
     * Obtener cantones de una provincia
     */
    @GetMapping("/cantones")
    public ResponseEntity<List<CantonDTO>> listarCantones(@RequestParam String provincia) {
        log.debug("GET /api/ubicaciones/cantones?provincia={}", provincia);
        List<CantonDTO> cantones = ubicacionService.obtenerCantonesPorProvincia(provincia);
        return ResponseEntity.ok(cantones);
    }
    
    /**
     * GET /api/ubicaciones/cantones/{provinciaCodigo}/{cantonCodigo}
     * Obtener cantón específico
     */
    @GetMapping("/cantones/{provinciaCodigo}/{cantonCodigo}")
    public ResponseEntity<CantonDTO> obtenerCanton(
            @PathVariable String provinciaCodigo,
            @PathVariable String cantonCodigo) {
        log.debug("GET /api/ubicaciones/cantones/{}/{}", provinciaCodigo, cantonCodigo);
        CantonDTO canton = ubicacionService.obtenerCanton(provinciaCodigo, cantonCodigo);
        
        if (canton != null) {
            return ResponseEntity.ok(canton);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * GET /api/ubicaciones/cantones/buscar?nombre={nombre}
     * Buscar cantones por nombre
     */
    @GetMapping("/cantones/buscar")
    public ResponseEntity<List<CantonDTO>> buscarCantones(@RequestParam String nombre) {
        log.debug("GET /api/ubicaciones/cantones/buscar?nombre={}", nombre);
        List<CantonDTO> cantones = ubicacionService.buscarCantonesPorNombre(nombre);
        return ResponseEntity.ok(cantones);
    }
    
    // ========== ENDPOINTS DE DISTRITOS ==========
    
    /**
     * GET /api/ubicaciones/distritos?provincia={codigo}&canton={codigo}
     * Obtener distritos de un cantón
     */
    @GetMapping("/distritos")
    public ResponseEntity<List<DistritoDTO>> listarDistritos(
            @RequestParam String provincia,
            @RequestParam String canton) {
        log.debug("GET /api/ubicaciones/distritos?provincia={}&canton={}", provincia, canton);
        List<DistritoDTO> distritos = ubicacionService.obtenerDistritosPorCanton(provincia, canton);
        return ResponseEntity.ok(distritos);
    }
    
    /**
     * GET /api/ubicaciones/distritos/{provinciaCodigo}/{cantonCodigo}/{distritoCodigo}
     * Obtener distrito específico
     */
    @GetMapping("/distritos/{provinciaCodigo}/{cantonCodigo}/{distritoCodigo}")
    public ResponseEntity<DistritoDTO> obtenerDistrito(
            @PathVariable String provinciaCodigo,
            @PathVariable String cantonCodigo,
            @PathVariable String distritoCodigo) {
        log.debug("GET /api/ubicaciones/distritos/{}/{}/{}", provinciaCodigo, cantonCodigo, distritoCodigo);
        DistritoDTO distrito = ubicacionService.obtenerDistrito(provinciaCodigo, cantonCodigo, distritoCodigo);
        
        if (distrito != null) {
            return ResponseEntity.ok(distrito);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * GET /api/ubicaciones/distritos/buscar?nombre={nombre}
     * Buscar distritos por nombre
     */
    @GetMapping("/distritos/buscar")
    public ResponseEntity<List<DistritoDTO>> buscarDistritos(@RequestParam String nombre) {
        log.debug("GET /api/ubicaciones/distritos/buscar?nombre={}", nombre);
        List<DistritoDTO> distritos = ubicacionService.buscarDistritosPorNombre(nombre);
        return ResponseEntity.ok(distritos);
    }
    
    // ========== ENDPOINTS COMBINADOS ==========
    
    /**
     * GET /api/ubicaciones/completa?provincia={p}&canton={c}&distrito={d}
     * Obtener ubicación completa con todos los nombres
     */
    @GetMapping("/completa")
    public ResponseEntity<UbicacionCompletaDTO> obtenerUbicacionCompleta(
            @RequestParam String provincia,
            @RequestParam String canton,
            @RequestParam String distrito) {
        log.debug("GET /api/ubicaciones/completa?provincia={}&canton={}&distrito={}", provincia, canton, distrito);
        UbicacionCompletaDTO ubicacion = ubicacionService.obtenerUbicacionCompleta(provincia, canton, distrito);
        
        if (ubicacion != null) {
            return ResponseEntity.ok(ubicacion);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * GET /api/ubicaciones/validar?provincia={p}&canton={c}&distrito={d}
     * Validar que una ubicación existe
     */
    @GetMapping("/validar")
    public ResponseEntity<Map<String, Boolean>> validarUbicacion(
            @RequestParam String provincia,
            @RequestParam(required = false) String canton,
            @RequestParam(required = false) String distrito) {
        log.debug("GET /api/ubicaciones/validar?provincia={}&canton={}&distrito={}", provincia, canton, distrito);
        boolean valida = ubicacionService.validarUbicacion(provincia, canton, distrito);
        return ResponseEntity.ok(Map.of("valida", valida));
    }
    
    /**
     * GET /api/ubicaciones/estadisticas
     * Obtener estadísticas de ubicaciones
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        log.debug("GET /api/ubicaciones/estadisticas");
        
        long totalProvincias = ubicacionService.obtenerProvincias().size();
        long totalCantones = 0;
        long totalDistritos = 0;
        
        // Contar cantones y distritos por provincia
        for (ProvinciaDTO provincia : ubicacionService.obtenerProvincias()) {
            long cantones = ubicacionService.contarCantonesPorProvincia(provincia.getCodigo());
            totalCantones += cantones;
            
            for (CantonDTO canton : ubicacionService.obtenerCantonesPorProvincia(provincia.getCodigo())) {
                totalDistritos += ubicacionService.contarDistritosPorCanton(provincia.getCodigo(), canton.getCodigo());
            }
        }
        
        Map<String, Object> estadisticas = Map.of(
            "totalProvincias", totalProvincias,
            "totalCantones", totalCantones,
            "totalDistritos", totalDistritos
        );
        
        return ResponseEntity.ok(estadisticas);
    }
}
