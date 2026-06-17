package api.astro.whats_orders_manager.modules.facturacion.scheduler;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * ============================================================================
 * RECORDATORIO DE PAGO SCHEDULER
 * ERP Orders Manager - Sprint 4 Fase 3.6
 * ============================================================================
 * Tarea programada para enviar recordatorios de pago automáticos.
 * 
 * Funcionalidad:
 * - Se ejecuta todos los días a las 9:00 AM
 * - Busca facturas con fecha de pago vencida o próximas a vencer
 * - Publica eventos de notificación para el sistema
 * - Los listeners se encargan del envío por canales configurados
 * 
 * Tipos de notificaciones:
 * - FACTURA_VENCIDA: Fecha de pago ya pasó
 * - FACTURA_POR_VENCER: 3 días o menos para vencimiento
 * 
 * Criterios:
 * - entregado = true (producto/servicio entregado)
 * - tipoFactura = PENDIENTE (no ha sido pagada)
 * - cliente con teléfono registrado
 * 
 * @author EmaSleal
 * @version 2.0 - Sprint 4 Fase 3.6 (Event-Driven)
 * ============================================================================
 */
@Slf4j
@Component
public class RecordatorioPagoScheduler {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private NotificacionService notificacionService;
    
    private static final int DIAS_AVISO_VENCIMIENTO = 3;

    /**
     * Tarea programada que se ejecuta todos los días a las 9:00 AM
     * 
     * Cron expression: "0 0 9 * * *"
     * - Segundo: 0
     * - Minuto: 0
     * - Hora: 9
     * - Día del mes: * (todos)
     * - Mes: * (todos)
     * - Día de la semana: * (todos)
     * 
     * ✅ ACTUALIZADO Sprint 4 Fase 3.6: Ahora publica eventos en lugar de enviar emails directamente
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void enviarRecordatoriosPago() {
        log.info("⏰ ========================================");
        log.info("⏰ INICIANDO PROCESO DE RECORDATORIOS DE PAGO");
        log.info("⏰ ========================================");
        
        try {
            // Buscar facturas con pago vencido
            List<Factura> facturasVencidas = facturaRepository.findFacturasConPagoVencido();
            
            // Buscar facturas próximas a vencer (3 días o menos)
            List<Factura> facturasPorVencer = buscarFacturasPorVencer();
            
            if (facturasVencidas.isEmpty() && facturasPorVencer.isEmpty()) {
                log.info("✅ No hay facturas vencidas ni próximas a vencer. Proceso finalizado.");
                return;
            }
            
            log.info("📋 Facturas vencidas: {}", facturasVencidas.size());
            log.info("📋 Facturas próximas a vencer: {}", facturasPorVencer.size());
            
            int eventosPublicados = 0;
            int fallidos = 0;
            
            // Procesar facturas vencidas
            for (Factura factura : facturasVencidas) {
                if (publicarEventoFacturaVencida(factura)) {
                    eventosPublicados++;
                } else {
                    fallidos++;
                }
            }
            
            // Procesar facturas próximas a vencer
            for (Factura factura : facturasPorVencer) {
                if (publicarEventoFacturaPorVencer(factura)) {
                    eventosPublicados++;
                } else {
                    fallidos++;
                }
            }
            
            log.info("⏰ ========================================");
            log.info("⏰ PROCESO DE RECORDATORIOS FINALIZADO");
            log.info("⏰ Total facturas procesadas: {}", facturasVencidas.size() + facturasPorVencer.size());
            log.info("⏰ Eventos publicados: {} ✅", eventosPublicados);
            log.info("⏰ Eventos fallidos: {} ❌", fallidos);
            log.info("⏰ ========================================");
            
        } catch (Exception e) {
            log.error("❌ Error crítico en el scheduler de recordatorios: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Busca facturas próximas a vencer (3 días o menos)
     */
    private List<Factura> buscarFacturasPorVencer() {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(DIAS_AVISO_VENCIMIENTO);
        
        return facturaRepository.findAll().stream()
            .filter(f -> f.getFechaPago() != null)
            .filter(f -> f.getEntregado() != null && f.getEntregado())
            .filter(f -> "PENDIENTE".equals(f.getTipoFactura()))
            .filter(f -> {
                LocalDate fechaPago = f.getFechaPago().toLocalDate();
                return fechaPago.isAfter(hoy) && !fechaPago.isAfter(fechaLimite);
            })
            .toList();
    }
    
    /**
     * Publica evento de factura vencida
     */
    private boolean publicarEventoFacturaVencida(Factura factura) {
        try {
            String telefono = factura.getCliente() != null ? factura.getCliente().getTelefono() : null;
            if (telefono == null || telefono.isBlank()) {
                log.warn("Factura {} sin telefono de cliente, saltando", factura.getNumeroFactura());
                return false;
            }

            long diasVencida = ChronoUnit.DAYS.between(
                factura.getFechaPago().toLocalDate(),
                LocalDate.now()
            );

            String titulo = "Factura Vencida";
            String mensaje = String.format(
                "La factura %s está vencida desde hace %d día(s). Total: %s",
                factura.getNumeroFactura(),
                diasVencida,
                factura.getTotal()
            );
            notificacionService.enviarNotificacionExterna(
                    telefono,
                    TipoNotificacion.FACTURA_VENCIDA,
                    CanalNotificacion.WHATSAPP,
                    titulo,
                    mensaje
            );

            log.info("Notificacion FACTURA_VENCIDA enviada: {}", factura.getNumeroFactura());
            return true;

        } catch (Exception e) {
            log.error("Error al notificar factura vencida {}: {}",
                factura.getNumeroFactura(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Publica evento de factura próxima a vencer
     */
    private boolean publicarEventoFacturaPorVencer(Factura factura) {
        try {
            String telefono = factura.getCliente() != null ? factura.getCliente().getTelefono() : null;
            if (telefono == null || telefono.isBlank()) {
                log.warn("Factura {} sin telefono de cliente, saltando", factura.getNumeroFactura());
                return false;
            }

            long diasRestantes = ChronoUnit.DAYS.between(
                LocalDate.now(),
                factura.getFechaPago().toLocalDate()
            );

            String titulo = "Factura Próxima a Vencer";
            String mensaje = String.format(
                "La factura %s vence en %d día(s). Total: %s",
                factura.getNumeroFactura(),
                diasRestantes,
                factura.getTotal()
            );
            notificacionService.enviarNotificacionExterna(
                    telefono,
                    TipoNotificacion.FACTURA_PROXIMA_VENCER,
                    CanalNotificacion.WHATSAPP,
                    titulo,
                    mensaje
            );

            log.info("Notificacion FACTURA_PROXIMA_VENCER enviada: {}", factura.getNumeroFactura());
            return true;

        } catch (Exception e) {
            log.error("Error al notificar factura proxima a vencer {}: {}",
                factura.getNumeroFactura(), e.getMessage());
            return false;
        }
    }

    /**
     * Método para ejecutar el recordatorio manualmente (testing)
     * Puede ser llamado desde un endpoint de administración
     */
    public void ejecutarManualmente() {
        log.info("🔧 Ejecución MANUAL del scheduler de recordatorios");
        enviarRecordatoriosPago();
    }
}
