package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaPendienteDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
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
    private NotificacionService notificacionService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private LineaFacturaService lineaFacturaService;

    @Autowired
    private FacturaMapper facturaMapper;

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
    public Page<Factura> buscarConFiltros(Pageable pageable, LocalDate startDate, LocalDate endDate,
                                           Boolean entregado, Boolean pagada, Boolean sinFE, EstadoComprobante estadoFE) {
        log.debug("Buscando facturas con filtros - startDate: {}, endDate: {}, entregado: {}, pagada: {}, sinFE: {}, estadoFE: {}",
            startDate, endDate, entregado, pagada, sinFE, estadoFE);
        return facturaRepository.buscarConFiltros(startDate, endDate, entregado, pagada, sinFE, estadoFE, pageable);
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
            while (facturaRepository.existsByNumeroFactura(numeroFactura)) {
                log.warn("El número de factura {} ya existe. Se sincronizará el contador y se intentará nuevamente.", numeroFactura);
                config.incrementarNumero();
                numeroFactura = config.generarNumeroFactura();
            }
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
        
        // Ensure monedaFE defaults to CRC and tipoCambio is consistent
        if (factura.getMonedaFE() == null || factura.getMonedaFE() == MonedaFE.CRC) {
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
        } else if (factura.getTipoCambio() == null || factura.getTipoCambio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                "tipoCambio es obligatorio y debe ser mayor a 0 para facturas en " + factura.getMonedaFE().getCodigo()
            );
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
        
        // Capture isNew before save — after save() the ID is always assigned
        boolean isNew = factura.getIdFactura() == null;

        // Guardar la factura
        Factura facturaGuardada = facturaRepository.save(factura);
        log.info("Factura guardada exitosamente con ID: {} y número: {}",
                facturaGuardada.getIdFactura(), facturaGuardada.getNumeroFactura());

        // Incrementar el contador solo al crear (no en updates)
        if (isNew) {
            try {
                configuracionFacturacionService.incrementarNumeroFactura();
                log.debug("Contador de factura incrementado. Próximo: {}", config.getNumeroActual() + 1);
            } catch (Exception e) {
                log.error("Error al incrementar número de factura: {}", e.getMessage());
            }
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
            String telefono = factura.getCliente() != null ? factura.getCliente().getTelefono() : null;
            if (telefono == null || telefono.isBlank()) {
                log.warn("No se pudo notificar factura generada: cliente sin telefono — factura {}",
                        factura.getNumeroFactura());
                return;
            }
            String titulo = "Nueva Factura Generada";
            String mensaje = "Se ha generado la factura " + factura.getNumeroFactura() +
                    " por un total de " + factura.getTotal();
            notificacionService.enviarNotificacionExterna(
                    telefono,
                    TipoNotificacion.FACTURA_CREADA,
                    CanalNotificacion.WHATSAPP,
                    titulo,
                    mensaje
            );
            log.info("Notificacion externa enviada por factura generada: {}", factura.getNumeroFactura());
        } catch (Exception e) {
            log.error("Error al notificar factura generada: {}", e.getMessage());
            // Do not rethrow — notification failure must not affect invoice creation
        }
    }

    @Override
    public Optional<List<Factura>> findByClienteId(Integer idCliente) {
        log.debug("Buscando facturas por ID de cliente: {}", idCliente);
        return facturaRepository.findByClienteId(idCliente);
    }

    @Override
    @Transactional
    public void eliminarConValidacion(Integer id) {
        Factura factura = facturaRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("La factura no existe"));
        if (!pagoService.findByFacturaId(id).isEmpty())
            throw new IllegalStateException("No se puede eliminar: la factura tiene pagos registrados");
        if (Boolean.TRUE.equals(factura.getEntregado()))
            throw new IllegalStateException("No se puede eliminar: la factura está marcada como entregada");
        List<LineaFactura> lineas = factura.getLineas();
        if (lineas != null) lineas.forEach(l -> lineaFacturaService.deleteById(l.getIdLineaFactura()));
        facturaRepository.deleteById(id);
        log.info("Factura eliminada con validación, ID: {}", id);
    }

    @Override
    @Transactional
    public void actualizarEstado(Integer id, Boolean entregado, String descripcion, String fechaEntregaStr) {
        Factura factura = facturaRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Factura no encontrada"));
        factura.setEntregado(entregado);
        if (descripcion != null) factura.setDescripcion(descripcion);
        if (fechaEntregaStr != null)
            factura.setFechaEntrega(fechaEntregaStr.isBlank() ? null : java.sql.Date.valueOf(fechaEntregaStr));
        facturaRepository.save(factura); // direct repo save — avoids side effects in service.save()
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaPendienteDTO> obtenerPendientesPorCliente(Integer idCliente) {
        return facturaRepository.findByClienteId(idCliente).orElseGet(List::of).stream()
            .filter(f -> f.calcularSaldoPendiente().compareTo(BigDecimal.ZERO) > 0)
            .map(facturaMapper::toPendingDTO)
            .toList();
    }
}
