package api.astro.whats_orders_manager.schedulers;

import api.astro.whats_orders_manager.models.Factura;
import api.astro.whats_orders_manager.repositories.FacturaRepository;
import api.astro.whats_orders_manager.services.EmailService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ============================================================================
 * RECORDATORIO DE PAGO SCHEDULER
 * WhatsApp Orders Manager
 * ============================================================================
 * Tarea programada para enviar recordatorios de pago automáticos.
 * 
 * Funcionalidad:
 * - Se ejecuta todos los días a las 9:00 AM
 * - Busca facturas con fecha de pago vencida
 * - Envía recordatorio por email a cada cliente
 * - Registra logs detallados de cada envío
 * 
 * Criterios de facturas a recordar:
 * - fechaPago < fecha actual
 * - entregado = true (producto/servicio entregado)
 * - tipoFactura = PENDIENTE (no ha sido pagada)
 * - cliente con email configurado
 * ============================================================================
 */
@Slf4j
@Component
public class RecordatorioPagoScheduler {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private EmailService emailService;

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
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void enviarRecordatoriosPago() {
        log.info("⏰ ========================================");
        log.info("⏰ INICIANDO PROCESO DE RECORDATORIOS DE PAGO");
        log.info("⏰ ========================================");
        
        try {
            // Buscar facturas con pago vencido
            List<Factura> facturasVencidas = facturaRepository.findFacturasConPagoVencido();
            
            if (facturasVencidas.isEmpty()) {
                log.info("✅ No hay facturas con pago vencido. Proceso finalizado.");
                return;
            }
            
            log.info("📋 Se encontraron {} factura(s) con pago vencido", facturasVencidas.size());
            
            int enviados = 0;
            int fallidos = 0;
            
            // Enviar recordatorio a cada factura
            for (Factura factura : facturasVencidas) {
                try {
                    log.info("📧 Procesando factura {} - Cliente: {} ({})", 
                        factura.getNumeroFactura(),
                        factura.getCliente().getNombre(),
                        factura.getCliente().getEmail()
                    );
                    
                    emailService.enviarRecordatorioPago(factura);
                    enviados++;
                    
                } catch (MessagingException e) {
                    fallidos++;
                    log.error("❌ Error al enviar recordatorio para factura {}: {}", 
                        factura.getNumeroFactura(), e.getMessage());
                } catch (Exception e) {
                    fallidos++;
                    log.error("❌ Error inesperado al procesar factura {}: {}", 
                        factura.getNumeroFactura(), e.getMessage(), e);
                }
            }
            
            log.info("⏰ ========================================");
            log.info("⏰ PROCESO DE RECORDATORIOS FINALIZADO");
            log.info("⏰ Total facturas procesadas: {}", facturasVencidas.size());
            log.info("⏰ Emails enviados: {} ✅", enviados);
            log.info("⏰ Emails fallidos: {} ❌", fallidos);
            log.info("⏰ ========================================");
            
        } catch (Exception e) {
            log.error("❌ Error crítico en el scheduler de recordatorios: {}", e.getMessage(), e);
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
