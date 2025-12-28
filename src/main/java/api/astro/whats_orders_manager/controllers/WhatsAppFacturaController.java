package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.models.MensajeWhatsApp;
import api.astro.whats_orders_manager.services.WhatsAppFacturaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para integración WhatsApp-Facturación
 * Permite enviar facturas y recordatorios por WhatsApp
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.5
 */
@RestController
@RequestMapping("/api/whatsapp/facturas")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@Slf4j
public class WhatsAppFacturaController {
    
    private final WhatsAppFacturaService facturaService;
    
    public WhatsAppFacturaController(WhatsAppFacturaService facturaService) {
        this.facturaService = facturaService;
    }
    
    // ========================================
    // ENDPOINTS DE ENVÍO
    // ========================================
    
    /**
     * POST /api/whatsapp/facturas/enviar
     * Envía una factura por WhatsApp (PDF + mensaje)
     * 
     * @param request Datos para enviar factura
     * @return Respuesta con éxito/error
     */
    @PostMapping("/enviar")
    public ResponseEntity<Map<String, Object>> enviarFactura(@Valid @RequestBody EnviarFacturaRequest request) {
        try {
            log.info("📨 Enviando factura {} por WhatsApp", request.getIdFactura());
            
            MensajeWhatsApp mensaje = facturaService.enviarFacturaPorWhatsApp(request.getIdFactura());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Factura enviada exitosamente por WhatsApp");
            response.put("idMensaje", mensaje.getIdMensajeWhatsapp());
            response.put("estado", mensaje.getEstado().name());
            response.put("telefono", mensaje.getTelefono());
            
            log.info("✅ Factura {} enviada exitosamente", request.getIdFactura());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Error al enviar factura: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (IllegalStateException e) {
            log.error("❌ Estado inválido al enviar factura: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar factura", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar factura: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * POST /api/whatsapp/facturas/recordatorio
     * Envía un recordatorio de pago por WhatsApp
     * 
     * @param request Datos para enviar recordatorio
     * @return Respuesta con éxito/error
     */
    @PostMapping("/recordatorio")
    public ResponseEntity<Map<String, Object>> enviarRecordatorio(@Valid @RequestBody EnviarRecordatorioRequest request) {
        try {
            log.info("🔔 Enviando recordatorio de pago para factura {}", request.getIdFactura());
            
            MensajeWhatsApp mensaje = facturaService.enviarRecordatorioPago(request.getIdFactura());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recordatorio enviado exitosamente");
            response.put("idMensaje", mensaje.getIdMensajeWhatsapp());
            response.put("estado", mensaje.getEstado().name());
            response.put("telefono", mensaje.getTelefono());
            
            log.info("✅ Recordatorio enviado para factura {}", request.getIdFactura());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Error al enviar recordatorio: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (IllegalStateException e) {
            log.error("❌ Estado inválido al enviar recordatorio: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar recordatorio", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar recordatorio: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * POST /api/whatsapp/facturas/notificacion-nueva
     * Envía notificación de factura nueva usando plantilla
     * 
     * @param request Datos para enviar notificación
     * @return Respuesta con éxito/error
     */
    @PostMapping("/notificacion-nueva")
    public ResponseEntity<Map<String, Object>> enviarNotificacionNueva(@Valid @RequestBody NotificacionNuevaRequest request) {
        try {
            log.info("📢 Enviando notificación de factura nueva {} con plantilla {}", 
                    request.getIdFactura(), request.getNombrePlantilla());
            
            MensajeWhatsApp mensaje = facturaService.enviarNotificacionFacturaNueva(
                    request.getIdFactura(),
                    request.getNombrePlantilla()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notificación enviada exitosamente");
            response.put("idMensaje", mensaje.getIdMensajeWhatsapp());
            response.put("estado", mensaje.getEstado().name());
            response.put("telefono", mensaje.getTelefono());
            
            log.info("✅ Notificación enviada para factura {}", request.getIdFactura());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Error al enviar notificación: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar notificación", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar notificación: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * POST /api/whatsapp/facturas/confirmacion-pago
     * Envía confirmación de pago por WhatsApp
     * 
     * @param request Datos para enviar confirmación
     * @return Respuesta con éxito/error
     */
    @PostMapping("/confirmacion-pago")
    public ResponseEntity<Map<String, Object>> enviarConfirmacionPago(@Valid @RequestBody ConfirmacionPagoRequest request) {
        try {
            log.info("✅ Enviando confirmación de pago para factura {}", request.getIdFactura());
            
            MensajeWhatsApp mensaje = facturaService.enviarConfirmacionPago(request.getIdFactura());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Confirmación de pago enviada exitosamente");
            response.put("idMensaje", mensaje.getIdMensajeWhatsapp());
            response.put("estado", mensaje.getEstado().name());
            response.put("telefono", mensaje.getTelefono());
            
            log.info("✅ Confirmación enviada para factura {}", request.getIdFactura());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Error al enviar confirmación: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar confirmación", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar confirmación: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * POST /api/whatsapp/facturas/resumen-cuenta
     * Envía resumen de cuenta con facturas pendientes
     * 
     * @param request Datos para enviar resumen
     * @return Respuesta con éxito/error
     */
    @PostMapping("/resumen-cuenta")
    public ResponseEntity<Map<String, Object>> enviarResumenCuenta(@Valid @RequestBody ResumenCuentaRequest request) {
        try {
            log.info("📊 Enviando resumen de cuenta para cliente {}", request.getIdCliente());
            
            MensajeWhatsApp mensaje = facturaService.enviarResumenCuenta(request.getIdCliente());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Resumen de cuenta enviado exitosamente");
            response.put("idMensaje", mensaje.getIdMensajeWhatsapp());
            response.put("estado", mensaje.getEstado().name());
            response.put("telefono", mensaje.getTelefono());
            
            log.info("✅ Resumen enviado para cliente {}", request.getIdCliente());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Error al enviar resumen: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (IllegalStateException e) {
            log.error("❌ Estado inválido al enviar resumen: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar resumen", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al enviar resumen: " + e.getMessage()
                    ));
        }
    }
    
    // ========================================
    // ENDPOINTS DE CONSULTA
    // ========================================
    
    /**
     * GET /api/whatsapp/facturas/pendientes/{idCliente}
     * Obtiene facturas pendientes de un cliente
     * 
     * @param idCliente ID del cliente
     * @return Lista de facturas pendientes
     */
    @GetMapping("/pendientes/{idCliente}")
    public ResponseEntity<Map<String, Object>> obtenerFacturasPendientes(@PathVariable Integer idCliente) {
        try {
            log.info("📋 Obteniendo facturas pendientes para cliente {}", idCliente);
            
            List<Factura> facturas = facturaService.obtenerFacturasPendientes(idCliente);
            
            // Calcular total pendiente
            BigDecimal totalPendiente = facturas.stream()
                    .map(Factura::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cantidad", facturas.size());
            response.put("totalPendiente", totalPendiente);
            response.put("facturas", facturas);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener facturas pendientes", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error al obtener facturas: " + e.getMessage()
                    ));
        }
    }
    
    /**
     * POST /api/whatsapp/facturas/enviar-masivo-recordatorios
     * Envía recordatorios a todos los clientes con facturas pendientes
     * 
     * @return Resultado del envío masivo
     */
    @PostMapping("/enviar-masivo-recordatorios")
    public ResponseEntity<Map<String, Object>> enviarRecordatoriosMasivos() {
        try {
            log.info("🚀 Iniciando envío masivo de recordatorios");
            
            // TODO: Implementar lógica para envío masivo
            // - Obtener todos los clientes con facturas pendientes
            // - Enviar recordatorio a cada uno
            // - Recopilar estadísticas
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Funcionalidad de envío masivo pendiente de implementación");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error en envío masivo de recordatorios", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error en envío masivo: " + e.getMessage()
                    ));
        }
    }
    
    // ========================================
    // CLASES INTERNAS - REQUEST BODIES
    // ========================================
    
    @Data
    static class EnviarFacturaRequest {
        @NotNull(message = "El ID de la factura es requerido")
        private Integer idFactura;
    }
    
    @Data
    static class EnviarRecordatorioRequest {
        @NotNull(message = "El ID de la factura es requerido")
        private Integer idFactura;
    }
    
    @Data
    static class NotificacionNuevaRequest {
        @NotNull(message = "El ID de la factura es requerido")
        private Integer idFactura;
        
        @NotBlank(message = "El nombre de la plantilla es requerido")
        private String nombrePlantilla;
    }
    
    @Data
    static class ConfirmacionPagoRequest {
        @NotNull(message = "El ID de la factura es requerido")
        private Integer idFactura;
    }
    
    @Data
    static class ResumenCuentaRequest {
        @NotNull(message = "El ID del cliente es requerido")
        private Integer idCliente;
    }
}
