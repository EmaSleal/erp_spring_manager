package api.astro.whats_orders_manager.modules.facturacion.electronica.scheduled;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.AmbienteDistintoException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final EnvioHaciendaService envioService;
    private final Clock clock;
    /** T-044 (P-099): ambiente de la configuración activa; el job solo trabaja con los comprobantes de ese ambiente. */
    private final AmbienteComprobanteGuard ambienteGuard;

    /** Consultas seguidas con ind-estado "error" tras las que un ERROR_HACIENDA deja de consultarse (revisar en ATV). */
    @Value("${facturacion.hacienda.max-consultas-error:"
        + ComprobanteElectronicoServiceImpl.MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO + "}")
    private int maxConsultasErrorHacienda = ComprobanteElectronicoServiceImpl.MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO;
    
    private static final int MAX_INTENTOS = 5;

    /** Estados con los que un reintento cuenta como exitoso: Hacienda ya recibió el comprobante (ENVIADO o posterior). */
    private static final java.util.Set<EstadoComprobante> EXITOSOS = java.util.EnumSet.of(EstadoComprobante.ENVIADO,
        EstadoComprobante.PROCESANDO, EstadoComprobante.ACEPTADO, EstadoComprobante.RECHAZADO,
        EstadoComprobante.ERROR_HACIENDA);

    /**
     * ENVIADO o posterior. ENVIO_INCIERTO precede a ENVIADO (ADR-043): no es exitoso ni fallido, se cuenta aparte y se
     * resuelve por consulta.
     */
    static boolean esReintentoExitoso(EstadoComprobante estado) {
        return EXITOSOS.contains(estado);
    }
    
    /**
     * Ejecuta cada 15 minutos para reintentar envíos fallidos.
     * Cron: 0 15 * * * * = cada 15 minutos
     *
     * Sin transacción de lote (T-032, criterio 29): cada reenvío corre en su propia transacción
     * ({@link ComprobanteElectronicoService#reenviar}), de modo que un comprobante que falla no revierte los demás. El
     * registro del fallo se guarda también en una transacción propia.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void reintentarEnviosPendientes() {
        log.info("🔄 Iniciando job de reintentos de comprobantes electrónicos");
        
        try {
            // Buscar comprobantes pendientes de reintento
            // T-044: solo los del ambiente de la configuración activa; los demás ni se cargan ni cuentan intentos.
            // Si esa configuración no se puede leer, se conserva el comportamiento de T-041 (sin filtro) y se registra.
            Optional<AmbienteHacienda> activo = ambienteGuard.ambienteActivo();
            List<ComprobanteElectronico> pendientes;
            if (activo.isPresent()) {
                pendientes = comprobanteRepository.findPendientesReintento(activo.get());
                log.info("Reintentos: {} excluidos por ambiente (ambiente activo {})",
                    comprobanteRepository.countPendientesReintentoDeOtroAmbiente(activo.get()), activo.get());
            } else {
                log.warn("Reintentos: ambiente activo no disponible; sin filtro por ambiente (comportamiento de T-041)");
                pendientes = comprobanteRepository.findPendientesReintentoSinFiltroDeAmbiente();
            }
            
            if (pendientes.isEmpty()) {
                log.info("✅ No hay comprobantes pendientes de reintento");
                return;
            }
            
            log.info("📋 Comprobantes pendientes de reintento: {}", pendientes.size());
            
            int exitosos = 0;
            int fallidos = 0;
            int alcanzaronLimite = 0;
            int inciertos = 0;
            
            for (ComprobanteElectronico comprobante : pendientes) {
                try {
                    // Límite de intentos: se deja de reintentar sin cambiar el estado (sigue reenviable a mano).
                    // No se marca ERROR: ERROR es solo una falla local antes de transmitir (ADR-030).
                    if (comprobante.getIntentosEnvio() >= MAX_INTENTOS) {
                        log.warn("⚠️ Comprobante {} alcanzó límite de {} reintentos",
                            comprobante.getId(), MAX_INTENTOS);
                        alcanzaronLimite++;
                        continue;
                    }

                    log.info("🔄 Reintentando comprobante {} ({}, intento {}/{})",
                        comprobante.getId(),
                        comprobante.getEstado(),
                        comprobante.getIntentosEnvio() + 1,
                        MAX_INTENTOS);

                    // GENERADO, FIRMADO, ERROR o NO_TRANSMITIDO: reenviar regenera/firma si hace falta y envía
                    // con la misma clave (ADR-030)
                    int intentosAntes = comprobante.getIntentosEnvio() != null ? comprobante.getIntentosEnvio() : 0;
                    ComprobanteElectronicoDTO resultado = envioService.reenviar(comprobante.getId());

                    // Solo cuenta como exitoso un reintento que no deja el comprobante reenviable (ENVIADO o posterior)
                    if (esReintentoExitoso(resultado.getEstado())) {
                        exitosos++;
                        log.info("✅ Comprobante {} reintentado → {}", comprobante.getId(), resultado.getEstado());
                    } else if (resultado.getEstado() == EstadoComprobante.ENVIO_INCIERTO) {
                        inciertos++;
                        log.info("Comprobante {} reintentado → ENVIO_INCIERTO (se resolverá por consulta)",
                            comprobante.getId());
                    } else {
                        fallidos++;
                        log.warn("⚠️ Comprobante {} reintentado → {}: {}", comprobante.getId(), resultado.getEstado(),
                            resultado.getUltimoError());
                        int intentosDespues = resultado.getIntentosEnvio() != null ? resultado.getIntentosEnvio() : 0;
                        if (intentosDespues <= intentosAntes) {
                            // Falla local antes de la marca de envío, que no contó el intento: se cuenta para que el
                            // job se detenga al llegar al límite (la marca de envío ya cuenta los demás)
                            envioService.registrarFalloDeReintento(comprobante.getId(),
                                "Reintento " + (intentosAntes + 1) + ": " + resultado.getUltimoError());
                        }
                    }

                } catch (AmbienteDistintoException e) {
                    // T-044: la configuración activa cambió entre la consulta y el envío; no es una falla del
                    // documento: ni intento contado ni ultimoError (nunca registrarFalloDeReintento)
                    log.info("Comprobante {} es de otro ambiente que el activo; se omite: {}", comprobante.getId(),
                        e.getMessage());
                } catch (EnvioHaciendaService.ComprobanteNoEnviableException e) {
                    // Otro proceso lo marcó (envío en curso) o Hacienda ya lo tiene: no se toca
                    log.info("Comprobante {} ya no es transmisible ({}); se omite", comprobante.getId(), e.getEstado());
                } catch (Exception e) {
                    fallidos++;
                    log.error("❌ Error reenviando comprobante {}: {}", 
                        comprobante.getId(), e.getMessage());
                    // Relee la fila: nunca guardar la copia cargada al inicio del job, que pisaría la marca de otro envío
                    envioService.registrarFalloDeReintento(comprobante.getId(),
                        "Reintento " + (comprobante.getIntentosEnvio() + 1) + ": " + e.getMessage());
                }
            }
            
            log.info("📊 Resumen de reintentos: {} exitosos, {} fallidos, {} inciertos, {} alcanzaron límite",
                exitosos, fallidos, inciertos, alcanzaronLimite);
            
        } catch (Exception e) {
            log.error("❌ Error crítico en job de reintentos: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Job que consulta estados de comprobantes enviados pero sin respuesta.
     * Ejecuta cada hora.
     * Cron: 0 0 * * * * = cada hora en punto
     *
     * Sin transacción de lote (T-032, criterio 27): cada consulta corre en su propia transacción
     * ({@link ComprobanteElectronicoService#consultarYActualizarEstado}). Si al aceptarse una nota de crédito sus
     * efectos fallan, solo ese comprobante se revierte (queda sin ACEPTADO y su nota EN_TRAMITE) y los demás se
     * guardan; la siguiente corrida lo vuelve a consultar.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void consultarEstadosPendientes() {
        log.info("🔍 Iniciando consulta de estados pendientes");
        
        try {
            // Comprobantes recibidos por Hacienda (ENVIADO), en proceso o con ind-estado "error" (ERROR_HACIENDA,
            // hasta el máximo de consultas seguidas), enviados hace más de 5 minutos. ENVIO_INCIERTO no se consulta
            // aquí: se resuelve con "Consultar estado en Hacienda" (ADR-030, ADR-031).
            // T-044: solo los del ambiente activo; con la configuración ilegible, sin filtro (comportamiento de T-041)
            LocalDateTime limite = LocalDateTime.now(clock).minusMinutes(5);
            Optional<AmbienteHacienda> activo = ambienteGuard.ambienteActivo();
            List<ComprobanteElectronico> enviados;
            if (activo.isPresent()) {
                enviados = comprobanteRepository.findParaSondeo(limite, maxConsultasErrorHacienda, activo.get());
                log.info("Consulta de estados: {} excluidos por ambiente (ambiente activo {})",
                    comprobanteRepository.countParaSondeoDeOtroAmbiente(limite, maxConsultasErrorHacienda, activo.get()),
                    activo.get());
            } else {
                log.warn("Consulta de estados: ambiente activo no disponible; sin filtro por ambiente (T-041)");
                enviados = comprobanteRepository.findParaSondeoSinFiltroDeAmbiente(limite, maxConsultasErrorHacienda);
            }
            
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
                } catch (AmbienteDistintoException e) {
                    log.info("Comprobante {} es de otro ambiente que el activo; no se consulta", comprobante.getId());
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
     *
     * Conserva la transacción de lote: solo antepone una marca al último error de cada comprobante, sin lógica que
     * pueda lanzar por elemento.
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
