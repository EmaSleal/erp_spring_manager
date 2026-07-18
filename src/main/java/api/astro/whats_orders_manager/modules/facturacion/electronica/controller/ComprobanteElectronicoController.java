package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.TendenciaEnviosDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para gestión de comprobantes electrónicos.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@RestController
@RequestMapping("/api/facturas/electronica/comprobantes")
@RequiredArgsConstructor
@Slf4j
public class ComprobanteElectronicoController {
    
    private final ComprobanteElectronicoService comprobanteService;
    private final FacturaRepository facturaRepository;
    
    /**
     * Procesa una factura completa: genera, firma y envía a Hacienda.
     * POST /api/facturas/comprobantes/procesar/{facturaId}
     */
    @PostMapping("/procesar/{facturaId}")
    public ResponseEntity<Map<String, Object>> procesarFacturaCompleta(@PathVariable Long facturaId) {
        log.info("Procesando factura completa {} para facturación electrónica", facturaId);
        try {
            ComprobanteElectronicoDTO comprobante = comprobanteService.procesarFactura(facturaId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Factura enviada a Hacienda exitosamente",
                "comprobante", comprobante
            ));
        } catch (Exception e) {
            log.error("Error procesando factura {}: {}", facturaId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Genera comprobante electrónico desde una factura.
     * POST /api/facturacion/comprobantes/generar
     */
    @PostMapping("/generar")
    public ResponseEntity<Map<String, Object>> generarDesdeFactura(
            @RequestParam Long facturaId) {
        log.info("Generando comprobante electrónico para factura {}", facturaId);
        try {
            ComprobanteElectronicoDTO comprobante = comprobanteService.generarDesdeFactura(facturaId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Comprobante generado exitosamente",
                "comprobante", comprobante
            ));
        } catch (Exception e) {
            log.error("Error generando comprobante: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error generando comprobante: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Firma un comprobante electrónico.
     * POST /api/facturacion/comprobantes/{id}/firmar
     */
    @PostMapping("/{id}/firmar")
    public ResponseEntity<Map<String, Object>> firmar(@PathVariable Long id) {
        log.info("Firmando comprobante {}", id);
        try {
            ComprobanteElectronicoDTO firmado = comprobanteService.firmar(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Comprobante firmado exitosamente",
                "comprobante", firmado
            ));
        } catch (Exception e) {
            log.error("Error firmando comprobante: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error firmando comprobante: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Envía comprobante a Hacienda.
     * POST /api/facturacion/comprobantes/{id}/enviar
     */
    @PostMapping("/{id}/enviar")
    public ResponseEntity<Map<String, Object>> enviarAHacienda(@PathVariable Long id) {
        log.info("Enviando comprobante {} a Hacienda", id);
        try {
            ComprobanteElectronicoDTO enviado = comprobanteService.enviarAHacienda(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Comprobante enviado exitosamente",
                "comprobante", enviado
            ));
        } catch (Exception e) {
            log.error("Error enviando comprobante: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error enviando comprobante: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Reenvía comprobante que tuvo error.
     * POST /api/facturacion/comprobantes/{id}/reenviar
     */
    @PostMapping("/{id}/reenviar")
    public ResponseEntity<Map<String, Object>> reenviar(@PathVariable Long id) {
        log.info("Reenviando comprobante {}", id);
        try {
            ComprobanteElectronicoDTO reenviado = comprobanteService.reenviar(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Comprobante reenviado exitosamente",
                "comprobante", reenviado
            ));
        } catch (Exception e) {
            log.error("Error reenviando comprobante: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error reenviando comprobante: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Consulta estado del comprobante en Hacienda.
     * GET /api/facturacion/comprobantes/{id}/consultar-estado
     */
    @GetMapping("/{id}/consultar-estado")
    public ResponseEntity<Map<String, Object>> consultarEstado(@PathVariable Long id) {
        log.info("Consultando estado de comprobante {}", id);
        try {
            ComprobanteElectronicoDTO actualizado = comprobanteService.consultarEstado(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "comprobante", actualizado
            ));
        } catch (Exception e) {
            log.error("Error consultando estado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error consultando estado: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Envía comprobante por email al cliente.
     * POST /api/facturacion/comprobantes/{id}/enviar-email
     */
    @PostMapping("/{id}/enviar-email")
    public ResponseEntity<Map<String, Object>> enviarPorEmail(
            @PathVariable Long id,
            @RequestParam(required = false) String emailAdicional) {
        log.info("Enviando comprobante {} por email", id);
        try {
            comprobanteService.enviarPorEmail(id, emailAdicional);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Comprobante enviado por email exitosamente"
            ));
        } catch (Exception e) {
            log.error("Error enviando email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error enviando email: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Anula un comprobante.
     * POST /api/facturacion/comprobantes/{id}/anular
     */
    @PostMapping("/{id}/anular")
    public ResponseEntity<Map<String, Object>> anular(
            @PathVariable Long id,
            @RequestParam String motivo) {
        log.info("Anulando comprobante {} - Motivo: {}", id, motivo);
        try {
            ComprobanteElectronicoDTO anulado = comprobanteService.anular(id, motivo);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Comprobante anulado exitosamente",
                "comprobante", anulado
            ));
        } catch (Exception e) {
            log.error("Error anulando comprobante: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error anulando comprobante: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Descarga el XML del comprobante.
     * GET /api/facturacion/comprobantes/{id}/xml
     */
    @GetMapping("/{id}/xml")
    public ResponseEntity<String> descargarXml(@PathVariable Long id) {
        log.info("📎 Descargando XML del comprobante {}", id);
        try {
            String xml = comprobanteService.descargarXml(id);
            
            // Obtener comprobante para el nombre del archivo
            ComprobanteElectronicoDTO comprobante = comprobanteService.obtenerPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment", 
                comprobante.getClaveNumerica() + ".xml");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(xml);
                
        } catch (Exception e) {
            log.error("❌ Error descargando XML: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<?xml version=\"1.0\"?><error>" + e.getMessage() + "</error>");
        }
    }
    
    /**
     * Obtiene comprobante por ID.
     * GET /api/facturacion/comprobantes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ComprobanteElectronicoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("Obteniendo comprobante {}", id);
        return comprobanteService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Obtiene comprobante por clave numérica.
     * GET /api/facturacion/comprobantes/clave/{claveNumerica}
     */
    @GetMapping("/clave/{claveNumerica}")
    public ResponseEntity<ComprobanteElectronicoDTO> obtenerPorClave(@PathVariable String claveNumerica) {
        log.info("Obteniendo comprobante por clave {}", claveNumerica);
        return comprobanteService.obtenerPorClaveNumerica(claveNumerica)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Obtiene comprobante de una factura.
     * GET /api/facturacion/comprobantes/factura/{facturaId}
     */
    @GetMapping("/factura/{facturaId}")
    public ResponseEntity<ComprobanteElectronicoDTO> obtenerPorFactura(@PathVariable Long facturaId) {
        log.info("Obteniendo comprobante de factura {}", facturaId);
        return comprobanteService.obtenerPorFactura(facturaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lista comprobantes de una empresa.
     * GET /api/facturacion/comprobantes/empresa/{empresaId}
     */
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<ComprobanteElectronicoDTO>> listarPorEmpresa(
            @PathVariable Integer empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listando comprobantes de empresa {} - page: {}, size: {}", empresaId, page, size);
        List<ComprobanteElectronicoDTO> comprobantes = comprobanteService.listarPorEmpresa(empresaId, page, size);
        return ResponseEntity.ok(comprobantes);
    }
    
    /**
     * Lista comprobantes por estado.
     * GET /api/facturacion/comprobantes/empresa/{empresaId}/estado/{estado}
     */
    @GetMapping("/empresa/{empresaId}/estado/{estado}")
    public ResponseEntity<List<ComprobanteElectronicoDTO>> listarPorEstado(
            @PathVariable Integer empresaId,
            @PathVariable EstadoComprobante estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listando comprobantes {} de empresa {}", estado, empresaId);
        List<ComprobanteElectronicoDTO> comprobantes = comprobanteService.listarPorEstado(empresaId, estado, page, size);
        return ResponseEntity.ok(comprobantes);
    }
    
    /**
     * Lista comprobantes por rango de fechas.
     * GET /api/facturacion/comprobantes/empresa/{empresaId}/fechas
     */
    @GetMapping("/empresa/{empresaId}/fechas")
    public ResponseEntity<List<ComprobanteElectronicoDTO>> listarPorFechas(
            @PathVariable Integer empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Listando comprobantes de empresa {} desde {} hasta {}", empresaId, fechaInicio, fechaFin);
        
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);
        
        List<ComprobanteElectronicoDTO> comprobantes = comprobanteService.listarPorRangoFechas(
                empresaId, inicio, fin, page, size);
        return ResponseEntity.ok(comprobantes);
    }
    
    /**
     * Obtiene estadísticas de comprobantes.
     * GET /api/facturacion/comprobantes/empresa/{empresaId}/estadisticas
     */
    @GetMapping("/empresa/{empresaId}/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(@PathVariable Integer empresaId) {
        log.info("Obteniendo estadísticas de comprobantes para empresa {}", empresaId);
        
        Map<String, Object> estadisticas = Map.of(
            "generados", comprobanteService.contarPorEstado(empresaId, EstadoComprobante.GENERADO),
            "firmados", comprobanteService.contarPorEstado(empresaId, EstadoComprobante.FIRMADO),
            "enviados", comprobanteService.contarPorEstado(empresaId, EstadoComprobante.ENVIADO),
            "procesando", comprobanteService.contarPorEstado(empresaId, EstadoComprobante.PROCESANDO),
            "aceptados", comprobanteService.contarPorEstado(empresaId, EstadoComprobante.ACEPTADO),
            "rechazados", comprobanteService.contarPorEstado(empresaId, EstadoComprobante.RECHAZADO),
            "errores", comprobanteService.contarPorEstado(empresaId, EstadoComprobante.ERROR),
            "anulados", comprobanteService.contarPorEstado(empresaId, EstadoComprobante.ANULADO)
        );
        
        return ResponseEntity.ok(estadisticas);
    }
    
    /**
     * Obtiene XML del comprobante (sin descargar).
     * GET /api/facturacion/comprobantes/{id}/xml-content
     */
    @GetMapping("/{id}/xml-content")
    public ResponseEntity<Map<String, String>> obtenerXmlContent(@PathVariable Long id) {
        log.info("Obteniendo contenido XML de comprobante {}", id);
        return comprobanteService.obtenerPorId(id)
                .map(comp -> ResponseEntity.ok(Map.of(
                    "xmlComprobante", comp.getXmlComprobante() != null ? comp.getXmlComprobante() : "",
                    "xmlRespuesta", comp.getXmlRespuesta() != null ? comp.getXmlRespuesta() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Descarga XML de respuesta de Hacienda.
     * GET /api/facturacion/comprobantes/{id}/xml-respuesta
     */
    @GetMapping("/{id}/xml-respuesta")
    public ResponseEntity<String> descargarXmlRespuesta(@PathVariable Long id) {
        log.info("Descargando XML de respuesta de comprobante {}", id);
        return comprobanteService.obtenerPorId(id)
                .filter(comp -> comp.getXmlRespuesta() != null)
                .map(comp -> ResponseEntity.ok()
                        .header("Content-Type", "application/xml")
                        .header("Content-Disposition", "attachment; filename=\"respuesta-" + 
                                comp.getClaveNumerica() + ".xml\"")
                        .body(comp.getXmlRespuesta()))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Obtiene facturas pendientes de generar comprobante electrónico para una empresa.
     * GET /api/facturas/comprobantes/empresa/{empresaId}/pendientes
     */
    @GetMapping("/empresa/{empresaId}/pendientes")
    public ResponseEntity<List<Map<String, Object>>> obtenerFacturasPendientes(
            @PathVariable Integer empresaId,
            @RequestParam(required = false) Integer clienteId) {
        log.info("Obteniendo facturas pendientes para empresa {} y cliente {}", empresaId, clienteId);
        try {
            // Obtener todas las facturas (o del cliente específico)
            List<Factura> facturas;
            if (clienteId != null) {
                facturas = facturaRepository.findByClienteIdCliente(clienteId);
            } else {
                facturas = facturaRepository.findAll();
            }
            
            // Primero obtener todos los comprobantes para evitar múltiples consultas
            List<ComprobanteElectronicoDTO> comprobantes = comprobanteService.listarPorEmpresa(empresaId, 0, 10000);
            
            // Crear un Set con los IDs de facturas que ya tienen comprobante
            var facturasConComprobante = comprobantes.stream()
                .filter(c -> c.getFacturaId() != null)
                .map(ComprobanteElectronicoDTO::getFacturaId)
                .collect(java.util.stream.Collectors.toSet());
            
            // Filtrar solo las que NO tienen comprobante electrónico Y cuyo cliente requiere FE
            List<Map<String, Object>> facturasPendientes = facturas.stream()
                .filter(factura -> !facturasConComprobante.contains(factura.getIdFactura().longValue()))
                .filter(factura -> factura.getCliente() != null && 
                        Boolean.TRUE.equals(factura.getCliente().getRequiereFacturaElectronica())) // Solo clientes que requieren FE
                .map(factura -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idFactura", factura.getIdFactura());
                    map.put("id_factura", factura.getIdFactura());
                    map.put("numeroFactura", factura.getNumeroFactura());
                    map.put("numero_factura", factura.getNumeroFactura());
                    map.put("fechaCreacion", factura.getCreateDate());
                    map.put("fecha_creacion", factura.getCreateDate());
                    map.put("total", factura.getTotal());
                    
                    if (factura.getCliente() != null) {
                        Map<String, Object> cliente = new HashMap<>();
                        cliente.put("idCliente", factura.getCliente().getIdCliente());
                        cliente.put("id_cliente", factura.getCliente().getIdCliente());
                        cliente.put("nombre", factura.getCliente().getNombre());
                        cliente.put("requiereFacturaElectronica", factura.getCliente().getRequiereFacturaElectronica());
                        map.put("cliente", cliente);
                    }
                    
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("Facturas pendientes encontradas: {}", facturasPendientes.size());
            return ResponseEntity.ok(facturasPendientes);
        } catch (Exception e) {
            log.error("Error obteniendo facturas pendientes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtiene estadísticas de tendencia de envíos para gráficos.
     * GET /api/facturas/comprobantes/estadisticas/tendencia
     * 
     * @param dias Número de días hacia atrás (default: 30)
     * @return TendenciaEnviosDTO con datos para gráfico
     */
    @GetMapping("/estadisticas/tendencia")
    public ResponseEntity<TendenciaEnviosDTO> getTendenciaEnvios(
            @RequestParam(defaultValue = "30") int dias) {
        
        log.info("Obteniendo tendencia de envíos para últimos {} días", dias);
        
        try {
            LocalDate fechaFin = LocalDate.now();
            LocalDate fechaInicio = fechaFin.minusDays(dias - 1);
            
            TendenciaEnviosDTO tendencia = TendenciaEnviosDTO.builder()
                .fechas(new ArrayList<>())
                .aceptados(new ArrayList<>())
                .rechazados(new ArrayList<>())
                .pendientes(new ArrayList<>())
                .errores(new ArrayList<>())
                .total(new ArrayList<>())
                .build();
            
            // Obtener todos los comprobantes del período
            List<ComprobanteElectronicoDTO> comprobantes = comprobanteService.listarPorRangoFechas(
                1, // empresaId - TODO: obtener de sesión
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(23, 59, 59),
                0,
                10000
            );
            
            // Agrupar por fecha y estado
            Map<LocalDate, Map<EstadoComprobante, Long>> estadisticasPorDia = new HashMap<>();
            
            for (ComprobanteElectronicoDTO comp : comprobantes) {
                LocalDate fecha = comp.getFechaEmision().toLocalDate();
                estadisticasPorDia.putIfAbsent(fecha, new HashMap<>());
                Map<EstadoComprobante, Long> estadosDelDia = estadisticasPorDia.get(fecha);
                estadosDelDia.put(
                    comp.getEstado(),
                    estadosDelDia.getOrDefault(comp.getEstado(), 0L) + 1
                );
            }
            
            // Generar datos para cada día del período
            for (LocalDate fecha = fechaInicio; !fecha.isAfter(fechaFin); fecha = fecha.plusDays(1)) {
                String fechaStr = fecha.toString();
                Map<EstadoComprobante, Long> estadosDelDia = estadisticasPorDia.getOrDefault(fecha, new HashMap<>());
                
                int aceptados = estadosDelDia.getOrDefault(EstadoComprobante.ACEPTADO, 0L).intValue();
                int rechazados = estadosDelDia.getOrDefault(EstadoComprobante.RECHAZADO, 0L).intValue();
                int pendientes = estadosDelDia.getOrDefault(EstadoComprobante.ENVIADO, 0L).intValue()
                    + estadosDelDia.getOrDefault(EstadoComprobante.PROCESANDO, 0L).intValue()
                    + estadosDelDia.getOrDefault(EstadoComprobante.GENERADO, 0L).intValue()
                    + estadosDelDia.getOrDefault(EstadoComprobante.FIRMADO, 0L).intValue();
                int errores = estadosDelDia.getOrDefault(EstadoComprobante.ERROR, 0L).intValue();
                
                tendencia.agregarDia(fechaStr, aceptados, rechazados, pendientes, errores);
            }
            
            log.info("Tendencia generada para {} días", dias);
            return ResponseEntity.ok(tendencia);
            
        } catch (Exception e) {
            log.error("Error obteniendo tendencia: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exporta comprobantes a Excel.
     * GET /api/facturas/comprobantes/exportar/excel
     * 
     * @param fechaInicio Fecha de inicio (opcional)
     * @param fechaFin Fecha de fin (opcional)
     * @param estado Estado a filtrar (opcional)
     * @return Archivo Excel con comprobantes
     */
    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String estado) {
        
        log.info("Exportando comprobantes a Excel - Inicio: {}, Fin: {}, Estado: {}", 
            fechaInicio, fechaFin, estado);
        
        try {
            // Establecer fechas por defecto si no se proporcionan
            if (fechaInicio == null) {
                fechaInicio = LocalDate.now().minusMonths(1);
            }
            if (fechaFin == null) {
                fechaFin = LocalDate.now();
            }
            
            // Obtener comprobantes
            List<ComprobanteElectronicoDTO> comprobantes;
            if (estado != null && !estado.isEmpty()) {
                comprobantes = comprobanteService.listarPorEstado(
                    1, // TODO: obtener empresaId de sesión
                    EstadoComprobante.valueOf(estado),
                    0,
                    10000
                );
            } else {
                comprobantes = comprobanteService.listarPorRangoFechas(
                    1, // TODO: obtener empresaId de sesión
                    fechaInicio.atStartOfDay(),
                    fechaFin.atTime(23, 59, 59),
                    0,
                    10000
                );
            }
            
            // TODO: Implementar generación de Excel usando Apache POI
            // Por ahora retornar respuesta temporal
            String mensaje = String.format("Exportación Excel pendiente. Comprobantes encontrados: %d", 
                comprobantes.size());
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=comprobantes.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(mensaje.getBytes());
                
        } catch (Exception e) {
            log.error("Error exportando a Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Exporta comprobantes a PDF.
     * GET /api/facturas/comprobantes/exportar/pdf
     * 
     * @param fechaInicio Fecha de inicio (opcional)
     * @param fechaFin Fecha de fin (opcional)
     * @param estado Estado a filtrar (opcional)
     * @return Archivo PDF con comprobantes
     */
    @GetMapping("/exportar/pdf")
    public ResponseEntity<byte[]> exportarPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String estado) {
        
        log.info("Exportando comprobantes a PDF - Inicio: {}, Fin: {}, Estado: {}", 
            fechaInicio, fechaFin, estado);
        
        try {
            // Establecer fechas por defecto si no se proporcionan
            if (fechaInicio == null) {
                fechaInicio = LocalDate.now().minusMonths(1);
            }
            if (fechaFin == null) {
                fechaFin = LocalDate.now();
            }
            
            // Obtener comprobantes
            List<ComprobanteElectronicoDTO> comprobantes;
            if (estado != null && !estado.isEmpty()) {
                comprobantes = comprobanteService.listarPorEstado(
                    1, // TODO: obtener empresaId de sesión
                    EstadoComprobante.valueOf(estado),
                    0,
                    10000
                );
            } else {
                comprobantes = comprobanteService.listarPorRangoFechas(
                    1, // TODO: obtener empresaId de sesión
                    fechaInicio.atStartOfDay(),
                    fechaFin.atTime(23, 59, 59),
                    0,
                    10000
                );
            }
            
            // TODO: Implementar generación de PDF usando iText o Flying Saucer
            // Por ahora retornar respuesta temporal
            String mensaje = String.format("Exportación PDF pendiente. Comprobantes encontrados: %d", 
                comprobantes.size());
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=comprobantes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(mensaje.getBytes());
                
        } catch (Exception e) {
            log.error("Error exportando a PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
