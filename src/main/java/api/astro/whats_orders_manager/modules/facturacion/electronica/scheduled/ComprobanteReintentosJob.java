package api.astro.whats_orders_manager.modules.facturacion.electronica.scheduled;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Job programado para reintentar envíos fallidos de comprobantes electrónicos.
 * Ejecuta cada 15 minutos buscando comprobantes con estado ERROR o PENDIENTE
 * que no hayan alcanzado el límite máximo de reintentos.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComprobanteReintentosJob {
    
    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final ComprobanteElectronicoService comprobanteService;
    
    private static final int MAX_INTENTOS = 5;
    
    /**
     * Ejecuta cada 15 minutos para reintentar envíos fallidos.
     * Cron: 0 15 * * * * = cada 15 minutos
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void reintentarEnviosPendientes() {
        log.info("🔄 Iniciando job de reintentos de comprobantes electrónicos");
        
        try {
            // Buscar comprobantes pendientes de reintento
            List<ComprobanteElectronico> pendientes = comprobanteRepository.findPendientesReintento();
            
            if (pendientes.isEmpty()) {
                log.info("✅ No hay comprobantes pendientes de reintento");
                return;
            }
            
            log.info("📋 Comprobantes pendientes de reintento: {}", pendientes.size());
            
            int exitosos = 0;
            int fallidos = 0;
            int alcanzaronLimite = 0;
            
            for (ComprobanteElectronico comprobante : pendientes) {
                try {
                    // Verificar límite de intentos
                    if (comprobante.getIntentosEnvio() >= MAX_INTENTOS) {
                        log.warn("⚠️ Comprobante {} alcanzó límite de {} reintentos", 
                            comprobante.getId(), MAX_INTENTOS);
                        
                        comprobante.setEstado(EstadoComprobante.ERROR);
                        comprobante.setUltimoError(
                            String.format("Límite de %d reintentos alcanzado. Último error: %s", 
                                MAX_INTENTOS, comprobante.getUltimoError())
                        );
                        comprobanteRepository.save(comprobante);
                        alcanzaronLimite++;
                        continue;
                    }
                    
                    log.info("🔄 Reintentando comprobante {} (intento {}/{})", 
                        comprobante.getId(), 
                        comprobante.getIntentosEnvio() + 1, 
                        MAX_INTENTOS);
                    
                    // Determinar qué hacer según el estado
                    if (comprobante.getEstado() == EstadoComprobante.GENERADO) {
                        // Necesita firma
                        comprobanteService.firmar(comprobante.getId());
                        comprobanteService.enviarAHacienda(comprobante.getId());
                    } else if (comprobante.getEstado() == EstadoComprobante.FIRMADO || 
                               comprobante.getEstado() == EstadoComprobante.ERROR) {
                        // Solo necesita envío
                        comprobanteService.reenviar(comprobante.getId());
                    }
                    
                    exitosos++;
                    log.info("✅ Comprobante {} reenviado exitosamente", comprobante.getId());
                    
                } catch (Exception e) {
                    fallidos++;
                    log.error("❌ Error reenviando comprobante {}: {}", 
                        comprobante.getId(), e.getMessage());
                    
                    // Actualizar error y contador
                    comprobante.setUltimoError("Reintento " + (comprobante.getIntentosEnvio() + 1) + 
                        ": " + e.getMessage());
                    comprobante.incrementarIntentos();
                    comprobanteRepository.save(comprobante);
                }
            }
            
            log.info("📊 Resumen de reintentos: {} exitosos, {} fallidos, {} alcanzaron límite", 
                exitosos, fallidos, alcanzaronLimite);
            
        } catch (Exception e) {
            log.error("❌ Error crítico en job de reintentos: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Job que consulta estados de comprobantes enviados pero sin respuesta.
     * Ejecuta cada hora.
     * Cron: 0 0 * * * * = cada hora en punto
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void consultarEstadosPendientes() {
        log.info("🔍 Iniciando consulta de estados pendientes");
        
        try {
            // Buscar comprobantes enviados hace más de 5 minutos sin respuesta
            List<ComprobanteElectronico> enviados = comprobanteRepository
                .findByEstadoAndFechaEnvioLessThan(
                    EstadoComprobante.ENVIADO,
                    LocalDateTime.now().minusMinutes(5)
                );
            
            if (enviados.isEmpty()) {
                log.info("✅ No hay estados pendientes de consultar");
                return;
            }
            
            log.info("📋 Comprobantes con estado pendiente: {}", enviados.size());
            
            int consultados = 0;
            for (ComprobanteElectronico comprobante : enviados) {
                try {
                    comprobanteService.consultarYActualizarEstado(comprobante.getId());
                    consultados++;
                } catch (Exception e) {
                    log.error("❌ Error consultando estado de {}: {}", 
                        comprobante.getId(), e.getMessage());
                }
            }
            
            log.info("📊 Estados consultados: {}/{}", consultados, enviados.size());
            
        } catch (Exception e) {
            log.error("❌ Error en consulta de estados: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Limpieza de comprobantes muy antiguos con error.
     * Ejecuta diariamente a las 2:00 AM.
     * Cron: 0 0 2 * * * = todos los días a las 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void limpiezaComprobantesAntiguos() {
        log.info("🧹 Iniciando limpieza de comprobantes antiguos");
        
        try {
            // Archivar comprobantes con error de más de 30 días
            LocalDateTime fechaLimite = LocalDateTime.now().minusDays(30);
            
            List<ComprobanteElectronico> antiguos = comprobanteRepository
                .findByEstadoAndCreatedAtLessThan(EstadoComprobante.ERROR, fechaLimite);
            
            if (antiguos.isEmpty()) {
                log.info("✅ No hay comprobantes antiguos para limpiar");
                return;
            }
            
            log.info("📋 Comprobantes antiguos encontrados: {}", antiguos.size());
            
            for (ComprobanteElectronico comprobante : antiguos) {
                // Marcar como archivado agregando nota
                String errorActual = comprobante.getUltimoError() != null ? 
                    comprobante.getUltimoError() : "Error desconocido";
                comprobante.setUltimoError(
                    "[ARCHIVADO " + LocalDateTime.now() + "] " + errorActual
                );
                comprobanteRepository.save(comprobante);
            }
            
            log.info("✅ Comprobantes archivados: {}", antiguos.size());
            
        } catch (Exception e) {
            log.error("❌ Error en limpieza de comprobantes: {}", e.getMessage(), e);
        }
    }
}
