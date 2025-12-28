package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.events.NotificacionEvent;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.models.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de facturas con integración
 * de configuración automática de numeración y cálculo de impuestos.
 * 
 * @author ASTRO
 * @version 2.0 - Sprint 2 - Fase 2
 */
@Slf4j
@Service
@Transactional
public class FacturaServiceImpl implements FacturaService {
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Autowired
    private ConfiguracionFacturacionService configuracionFacturacionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<Factura> findAll() { 
        log.debug("Obteniendo todas las facturas");
        return facturaRepository.findAll(); 
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Factura> findAll(Pageable pageable) { 
        log.debug("Obteniendo facturas paginadas - Página: {}, Tamaño: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        return facturaRepository.findAll(pageable); 
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Factura> findById(Integer id) { 
        log.debug("Buscando factura por ID: {}", id);
        return facturaRepository.findById(id); 
    }

    @Override
    @Transactional
    public Factura save(Factura factura) {
        log.debug("Guardando nueva factura");
        
        // Obtener configuración de facturación
        ConfiguracionFacturacion config = configuracionFacturacionService.getOrCreateConfiguracion();
        log.debug("Configuración de facturación obtenida: Serie={}, Número={}", 
                config.getSerieFactura(), config.getNumeroActual());
        
        // ✅ MODIFICADO: Solo generar si no viene del formulario
        if (factura.getNumeroFactura() == null || factura.getNumeroFactura().trim().isEmpty()) {
            String numeroFactura = config.generarNumeroFactura();
            factura.setNumeroFactura(numeroFactura);
            log.info("Número de factura generado automáticamente: {}", numeroFactura);
        } else {
            log.info("Número de factura proporcionado manualmente: {}", factura.getNumeroFactura());
        }
        
        // ✅ MODIFICADO: Solo generar serie si no viene del formulario
        if (factura.getSerie() == null || factura.getSerie().trim().isEmpty()) {
            factura.setSerie(config.getSerieFactura());
            log.info("Serie generada automáticamente: {}", config.getSerieFactura());
        } else {
            log.info("Serie proporcionada manualmente: {}", factura.getSerie());
        }
        
        // Calcular IGV y total si no están establecidos
        if (factura.getSubtotal() != null && factura.getSubtotal().compareTo(BigDecimal.ZERO) > 0) {
            
            // Si el IGV no está calculado, calcularlo
            if (factura.getIgv() == null || factura.getIgv().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal igvCalculado = config.calcularIgv(factura.getSubtotal());
                factura.setIgv(igvCalculado);
                log.debug("IGV calculado: {} ({}%)", igvCalculado, config.getIgv());
            }
            
            // Si el total no está calculado, calcularlo
            if (factura.getTotal() == null || factura.getTotal().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal totalCalculado = config.calcularTotal(factura.getSubtotal());
                factura.setTotal(totalCalculado);
                log.debug("Total calculado: {}", totalCalculado);
            }
        }
        
        // Guardar la factura
        Factura facturaGuardada = facturaRepository.save(factura);
        log.info("Factura guardada exitosamente con ID: {} y número: {}", 
                facturaGuardada.getIdFactura(), facturaGuardada.getNumeroFactura());
        
        // Incrementar el número de factura DESPUÉS de guardar exitosamente
        try {
            configuracionFacturacionService.incrementarNumeroFactura();
            log.debug("Número de factura incrementado. Próximo número: {}", config.getNumeroActual() + 1);
        } catch (Exception e) {
            log.error("Error al incrementar número de factura: {}", e.getMessage());
            // No lanzamos la excepción para no revertir la transacción de la factura
            // El número se puede ajustar manualmente si es necesario
        }
        
        // ✅ NUEVO: Publicar evento de factura generada
        publicarEventoFacturaGenerada(facturaGuardada);
        
        return facturaGuardada;
    }

    @Override
    @Transactional
    public void deleteById(Integer id) { 
        log.debug("Eliminando factura con ID: {}", id);
        facturaRepository.deleteById(id); 
        log.info("Factura eliminada exitosamente con ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean existsById(Integer id) {
        log.debug("Verificando existencia de factura con ID: {}", id);
        return facturaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() { 
        log.debug("Contando total de facturas");
        return facturaRepository.count(); 
    }

    @Override
    @Transactional(readOnly = true)
    public long countByFechaToday() { 
        log.debug("Contando facturas de hoy");
        return facturaRepository.countByFechaToday(); 
    }

    @Override
    @Transactional(readOnly = true)
    public java.math.BigDecimal sumTotalPendiente() { 
        log.debug("Sumando total de facturas pendientes");
        return facturaRepository.sumTotalPendiente(); 
    }
    
    /**
     * Obtiene la configuración de facturación actual.
     * Útil para mostrar en las vistas (símbolo de moneda, formato, etc.)
     * 
     * @return Configuración de facturación activa
     */
    @Transactional(readOnly = true)
    public ConfiguracionFacturacion getConfiguracionFacturacion() {
        log.debug("Obteniendo configuración de facturación para vistas");
        return configuracionFacturacionService.getOrCreateConfiguracion();
    }
    
    /**
     * Obtiene un preview del próximo número de factura sin incrementarlo.
     * Útil para mostrar en formularios antes de crear la factura.
     * 
     * @return Número de factura que se generará
     */
    @Transactional(readOnly = true)
    public String getPreviewNumeroFactura() {
        log.debug("Generando preview de número de factura");
        return configuracionFacturacionService.generarSiguienteNumeroFactura();
    }
    
    /**
     * Publica evento de factura generada para notificaciones
     * 
     * @param factura Factura recién creada
     */
    private void publicarEventoFacturaGenerada(Factura factura) {
        try {
            // Determinar destinatario (empresa o cliente)
            var usuario = factura.getCliente() != null && factura.getCliente().getUsuario() != null
                ? factura.getCliente().getUsuario()
                : null;
            
            if (usuario != null) {
                eventPublisher.publishEvent(
                    NotificacionEvent.builder(this)
                        .tipo(TipoNotificacion.FACTURA_CREADA)
                        .usuario(usuario)
                        .titulo("Nueva Factura Generada")
                        .mensaje("Se ha generado la factura " + factura.getNumeroFactura() + 
                                " por un total de " + factura.getTotal())
                        .urlAccion("/facturas/detalle/" + factura.getIdFactura())
                        .textoBoton("Ver Factura")
                        .agregarDato("idFactura", factura.getIdFactura())
                        .agregarDato("numeroFactura", factura.getNumeroFactura())
                        .agregarDato("total", factura.getTotal())
                        .build()
                );
                log.info("📢 Evento de factura generada publicado: {}", factura.getNumeroFactura());
            } else {
                log.warn("⚠️ No se pudo publicar evento: factura sin cliente/usuario asociado");
            }
        } catch (Exception e) {
            log.error("❌ Error al publicar evento de factura generada: {}", e.getMessage());
            // No lanzar excepción para no afectar la creación de la factura
        }
    }
}
