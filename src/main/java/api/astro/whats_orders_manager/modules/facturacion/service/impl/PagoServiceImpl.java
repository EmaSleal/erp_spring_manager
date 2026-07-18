package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.facturacion.dto.EstadoCuentaClienteDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.ReporteCajaDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.NumeroPagoGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de pagos.
 * Gestiona el ciclo de vida completo de los pagos y su relación con las facturas.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
@Slf4j
@Service
@Transactional
public class PagoServiceImpl implements PagoService {
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private NumeroPagoGeneratorService numeroPagoGenerator;

    @Autowired
    private PagoMapper pagoMapper;

    @Autowired
    private FacturaMapper facturaMapper;

    // ==================== OPERACIONES CRUD ====================
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findAll() {
        log.debug("Obteniendo todos los pagos");
        return pagoRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Pago> findAll(Pageable pageable) {
        log.debug("Obteniendo pagos paginados - Página: {}, Tamaño: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        return pagoRepository.findAll(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PagoDTO> findAllAsDTO(Pageable pageable) {
        log.debug("Obteniendo pagos como DTOs paginados - Página: {}, Tamaño: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        return pagoRepository.findAllAsDTO(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Pago> findById(Long id) {
        log.debug("Buscando pago por ID: {}", id);
        return pagoRepository.findById(id);
    }
    
    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("Eliminando pago con ID: {}", id);
        
        Optional<Pago> pagoOpt = pagoRepository.findById(id);
        if (pagoOpt.isPresent()) {
            Pago pago = pagoOpt.get();
            Factura factura = pago.getFactura();
            
            // Eliminar el pago
            pagoRepository.deleteById(id);
            log.info("Pago {} eliminado exitosamente", id);
            
            // Actualizar estado de la factura
            if (factura != null) {
                factura.actualizarEstadoPago();
                facturaRepository.save(factura);
                log.info("Estado de factura {} actualizado después de eliminar pago", factura.getIdFactura());
            }
        } else {
            log.warn("Pago con ID {} no encontrado para eliminar", id);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Boolean existsById(Long id) {
        return pagoRepository.existsById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        return pagoRepository.count();
    }
    
    // ==================== OPERACIONES DE NEGOCIO ====================
    
    @Override
    @Transactional
    public Pago registrarPago(Pago pago) {
        log.info("Registrando nuevo pago - Cliente: {}, Factura: {}, Tipo: {}, Monto: {}, Método: {}",
            pago.getCliente() != null ? pago.getCliente().getIdCliente() : "N/A",
            pago.getFactura() != null ? pago.getFactura().getIdFactura() : "N/A",
            pago.getTipoPago(),
            pago.getMonto(), 
            pago.getMetodoPago());
        
        // Validar que el cliente exista
        if (pago.getCliente() == null || pago.getCliente().getIdCliente() == null) {
            throw new IllegalArgumentException("El cliente es obligatorio para registrar un pago");
        }
        
        Cliente cliente = clienteRepository.findById(pago.getCliente().getIdCliente())
            .orElseThrow(() -> new EntityNotFoundException(
                "Cliente no encontrado con ID: " + pago.getCliente().getIdCliente()));
        
        // Asignar el cliente completo al pago
        pago.setCliente(cliente);
        
        // Validar y asignar factura si existe (no obligatorio para adelantos)
        if (pago.getFactura() != null && pago.getFactura().getIdFactura() != null) {
            Factura factura = facturaRepository.findById(pago.getFactura().getIdFactura())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Factura no encontrada con ID: " + pago.getFactura().getIdFactura()));
            
            // Asignar la factura completa al pago
            pago.setFactura(factura);
        
            // Validar que el monto no exceda el saldo pendiente
            BigDecimal saldoPendiente = factura.calcularSaldoPendiente();
            if (pago.getMonto().compareTo(saldoPendiente) > 0) {
                throw new IllegalArgumentException(
                    String.format("El monto del pago (%.2f) excede el saldo pendiente (%.2f)", 
                        pago.getMonto(), saldoPendiente));
            }
        }
        
        // Validar que si el tipo de pago requiere factura, esté presente
        pago.validarFacturaRequerida();
        
        // Validar referencia si es obligatoria
        pago.validarReferencia();
        
        // Validar duplicidad de referencia
        if (pago.getReferenciaBancaria() != null && !pago.getReferenciaBancaria().trim().isEmpty()) {
            if (pagoRepository.existsByReferenciaBancaria(pago.getReferenciaBancaria())) {
                throw new IllegalArgumentException(
                    "Ya existe un pago con la referencia: " + pago.getReferenciaBancaria());
            }
        }
        
        // Generar número de pago automáticamente si no está definido
        if (pago.getNumeroPago() == null || pago.getNumeroPago().isEmpty()) {
            String numeroPago = numeroPagoGenerator.generarNumeroPago();
            pago.setNumeroPago(numeroPago);
            log.debug("Número de pago generado: {}", numeroPago);
        } else {
            // Validar que no exista el número si fue proporcionado manualmente
            if (pagoRepository.existsByNumeroPago(pago.getNumeroPago())) {
                throw new IllegalArgumentException(
                    "Ya existe un pago con el número: " + pago.getNumeroPago());
            }
        }
        
        // Establecer fecha de pago si no está definida
        if (pago.getFechaPago() == null) {
            pago.setFechaPago(LocalDateTime.now());
        }
        
        // Estado por defecto: CONFIRMADO
        if (pago.getEstado() == null) {
            pago.setEstado(EstadoPago.CONFIRMADO);
        }
        
        // Guardar el pago
        Pago pagoGuardado = pagoRepository.save(pago);
        log.info("Pago {} registrado exitosamente - ID: {}, Tipo: {}", 
            pagoGuardado.getNumeroPago(), pagoGuardado.getIdPago(), pagoGuardado.getTipoPago());
        
        // Actualizar estado de la factura (si tiene factura asignada)
        if (pagoGuardado.getFactura() != null) {
            Factura facturaActualizar = pagoGuardado.getFactura();
            facturaActualizar.actualizarEstadoPago();
            facturaRepository.save(facturaActualizar);
            log.info("Estado de factura {} actualizado a: {}", 
                facturaActualizar.getIdFactura(), facturaActualizar.getEstadoPago());
        }
        
        return pagoGuardado;
    }
    
    @Override
    @Transactional
    public Pago registrarPago(PagoDTO dto) {
        log.info("Registrando pago desde DTO - Cliente: {}, Factura: {}", dto.getIdCliente(), dto.getIdFactura());

        Pago pago = pagoMapper.toEntity(dto);

        // Resolve Cliente (mandatory)
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + dto.getIdCliente()));
        pago.setCliente(cliente);

        // Resolve Factura (optional — null idFactura means advance payment)
        if (dto.getIdFactura() != null) {
            Factura factura = facturaRepository.findById(dto.getIdFactura())
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + dto.getIdFactura()));
            pago.setFactura(factura);
        }

        return registrarPago(pago);
    }

    @Override
    @Transactional
    public Pago confirmarPago(Long idPago) {
        log.info("Confirmando pago con ID: {}", idPago);
        
        Pago pago = pagoRepository.findById(idPago)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + idPago));
        
        if (pago.getEstado() == EstadoPago.CONFIRMADO) {
            log.warn("El pago {} ya está confirmado", idPago);
            return pago;
        }
        
        pago.setEstado(EstadoPago.CONFIRMADO);
        Pago pagoConfirmado = pagoRepository.save(pago);
        
        // Actualizar estado de la factura
        Factura factura = pago.getFactura();
        if (factura != null) {
            factura.actualizarEstadoPago();
            facturaRepository.save(factura);
        }
        
        log.info("Pago {} confirmado exitosamente", idPago);
        return pagoConfirmado;
    }
    
    @Override
    @Transactional
    public Pago conciliarPago(Long idPago) {
        log.info("Conciliando pago con ID: {}", idPago);
        
        Pago pago = pagoRepository.findById(idPago)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + idPago));
        
        if (pago.getEstado() == EstadoPago.CONCILIADO) {
            log.warn("El pago {} ya está conciliado", idPago);
            return pago;
        }
        
        if (pago.getEstado() == EstadoPago.RECHAZADO) {
            throw new IllegalStateException("No se puede conciliar un pago rechazado");
        }
        
        pago.conciliar();
        Pago pagoConciliado = pagoRepository.save(pago);
        
        // Actualizar estado de la factura
        Factura factura = pago.getFactura();
        if (factura != null) {
            factura.actualizarEstadoPago();
            facturaRepository.save(factura);
        }
        
        log.info("Pago {} conciliado exitosamente en: {}", idPago, pago.getFechaConciliacion());
        return pagoConciliado;
    }
    
    @Override
    @Transactional
    public Pago rechazarPago(Long idPago, String motivo) {
        log.info("Rechazando pago con ID: {} - Motivo: {}", idPago, motivo);
        
        Pago pago = pagoRepository.findById(idPago)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + idPago));
        
        if (pago.getEstado() == EstadoPago.CONCILIADO) {
            throw new IllegalStateException("No se puede rechazar un pago ya conciliado");
        }
        
        pago.rechazar(motivo);
        Pago pagoRechazado = pagoRepository.save(pago);
        
        // Actualizar estado de la factura
        Factura factura = pago.getFactura();
        if (factura != null) {
            factura.actualizarEstadoPago();
            facturaRepository.save(factura);
        }
        
        log.warn("Pago {} rechazado - Motivo: {}", idPago, motivo);
        return pagoRechazado;
    }
    
    @Override
    @Transactional
    public Pago anularPago(Long idPago, String motivo, Integer usuarioId) {
        log.info("Anulando pago con ID: {} - Motivo: {} - Usuario: {}", idPago, motivo, usuarioId);
        
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo de anulación es obligatorio");
        }
        
        if (usuarioId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio para anular");
        }
        
        Pago pago = pagoRepository.findById(idPago)
            .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado con ID: " + idPago));
        
        if (pago.getEstado() == EstadoPago.ANULADO) {
            log.warn("El pago {} ya está anulado", idPago);
            return pago;
        }
        
        if (pago.getEstado() == EstadoPago.CONCILIADO) {
            throw new IllegalStateException("No se puede anular un pago ya conciliado. Primero debe desconciliarse.");
        }
        
        // Usar método de negocio del modelo
        pago.anular(motivo, usuarioId);
        Pago pagoAnulado = pagoRepository.save(pago);
        
        // Actualizar estado de la factura si tiene factura asignada
        Factura factura = pago.getFactura();
        if (factura != null) {
            factura.actualizarEstadoPago();
            facturaRepository.save(factura);
            log.info("Estado de factura {} actualizado después de anular pago", factura.getIdFactura());
        }
        
        log.warn("Pago {} ({}) anulado por usuario {} - Motivo: {}", 
            idPago, pago.getNumeroPago(), usuarioId, motivo);
        return pagoAnulado;
    }
    
    @Override
    @Transactional
    public List<Pago> conciliarPagosLote(List<Long> idsPagos) {
        log.info("Conciliando {} pagos en lote", idsPagos.size());
        
        return idsPagos.stream()
            .map(this::conciliarPago)
            .collect(Collectors.toList());
    }
    
    // ==================== CONSULTAS POR FACTURA ====================
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findByFacturaId(Integer idFactura) {
        log.debug("Buscando pagos de la factura: {}", idFactura);
        return pagoRepository.findByFacturaId(idFactura);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findPagosValidosByFacturaId(Integer idFactura) {
        log.debug("Buscando pagos válidos de la factura: {}", idFactura);
        return pagoRepository.findPagosValidosByFacturaId(idFactura);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalPagadoPorFactura(Integer idFactura) {
        log.debug("Calculando total pagado de la factura: {}", idFactura);
        return pagoRepository.calcularTotalPagadoPorFactura(idFactura);
    }
    
    // ==================== CONSULTAS POR CRITERIOS ====================
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findByMetodoPago(MetodoPago metodoPago) {
        log.debug("Buscando pagos por método: {}", metodoPago);
        return pagoRepository.findByMetodoPago(metodoPago);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findByEstado(EstadoPago estado) {
        log.debug("Buscando pagos por estado: {}", estado);
        return pagoRepository.findByEstado(estado);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findPagosPendientesConciliacion() {
        log.debug("Buscando pagos pendientes de conciliación");
        return pagoRepository.findPagosPendientesConciliacion();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findByFechaPagoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        log.debug("Buscando pagos entre {} y {}", fechaInicio, fechaFin);
        return pagoRepository.findByFechaPagoBetween(fechaInicio, fechaFin);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Pago> findByReferencia(String referencia) {
        log.debug("Buscando pago por referencia: {}", referencia);
        return pagoRepository.findByReferenciaBancaria(referencia);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByReferencia(String referencia) {
        return pagoRepository.existsByReferenciaBancaria(referencia);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Pago> findByNumeroPago(String numeroPago) {
        log.debug("Buscando pago por número: {}", numeroPago);
        return pagoRepository.findByNumeroPago(numeroPago);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findByClienteId(Integer idCliente) {
        log.debug("Buscando pagos del cliente: {}", idCliente);
        return pagoRepository.findByClienteId(idCliente);
    }
    
    // ==================== REPORTES Y ESTADÍSTICAS ====================
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumIngresosDiaActual() {
        log.debug("Calculando ingresos del día actual");
        return pagoRepository.sumIngresosDiaActual();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Pago> findPagosDelDia() {
        log.debug("Obteniendo pagos del día actual");
        return pagoRepository.findPagosDelDia();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countPagosDelDia() {
        log.debug("Contando pagos del día actual");
        return pagoRepository.countPagosDelDia();
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumByMetodoPagoAndFecha(MetodoPago metodoPago, 
                                              LocalDateTime fechaInicio, 
                                              LocalDateTime fechaFin) {
        log.debug("Calculando total de pagos por método {} entre {} y {}", 
            metodoPago, fechaInicio, fechaFin);
        return pagoRepository.sumByMetodoPagoAndFecha(metodoPago, fechaInicio, fechaFin);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByMetodoPagoAndFecha(MetodoPago metodoPago,
                                          LocalDateTime fechaInicio,
                                          LocalDateTime fechaFin) {
        log.debug("Contando pagos por método {} entre {} y {}",
            metodoPago, fechaInicio, fechaFin);
        return pagoRepository.countByMetodoPagoAndFecha(metodoPago, fechaInicio, fechaFin);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteCajaDTO buildReporteCaja(LocalDate fecha) {
        log.info("Building cash report for date: {}", fecha);

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);
        List<EstadoPago> estadosValidos = List.of(EstadoPago.CONFIRMADO, EstadoPago.CONCILIADO);

        // Single grouped query replaces the 12-query fan-out (6 sums + 6 counts).
        // Column order: [0] MetodoPago, [1] BigDecimal total (SUM), [2] Long count (COUNT)
        List<Object[]> totales = pagoRepository.findTotalesByMetodoPagoAndFecha(inicio, fin, estadosValidos);

        ReporteCajaDTO reporte = ReporteCajaDTO.builder()
            .fecha(fecha)
            .fechaGeneracion(LocalDateTime.now())
            .build();

        for (Object[] row : totales) {
            MetodoPago metodo = (MetodoPago) row[0];
            BigDecimal total = (BigDecimal) row[1];
            Long cantidad = (Long) row[2];
            switch (metodo) {
                case EFECTIVO      -> { reporte.setTotalEfectivo(total);      reporte.setCantidadEfectivo(cantidad); }
                case TARJETA       -> { reporte.setTotalTarjeta(total);       reporte.setCantidadTarjeta(cantidad); }
                case CHEQUE        -> { reporte.setTotalCheque(total);        reporte.setCantidadCheque(cantidad); }
                case TRANSFERENCIA -> { reporte.setTotalTransferencia(total); reporte.setCantidadTransferencia(cantidad); }
                case RECAUDADO     -> { reporte.setTotalRecaudado(total);     reporte.setCantidadRecaudado(cantidad); }
                case OTROS         -> { reporte.setTotalOtros(total);         reporte.setCantidadOtros(cantidad); }
            }
        }

        // pagosDetalle: all payments in range regardless of estado (pre-existing behavior — do not filter)
        List<PagoDTO> detalle = pagoMapper.toDTOList(findByFechaPagoBetween(inicio, fin));
        reporte.setPagosDetalle(detalle);
        reporte.calcularTotales();

        return reporte;
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoCuentaClienteDTO buildEstadoCuenta(Integer idCliente) {
        log.info("Building account statement for client: {}", idCliente);

        Cliente cliente = clienteRepository.findById(idCliente)
            .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + idCliente));

        List<Factura> facturaEntities = facturaRepository.findByClienteId(idCliente)
            .orElse(Collections.emptyList());

        List<FacturaDetalleDTO> facturas = facturaEntities.stream()
            .map(facturaMapper::toDetalleDTO)
            .collect(Collectors.toList());

        List<PagoDTO> pagos = pagoMapper.toDTOList(findByClienteId(idCliente));

        BigDecimal totalFacturado = facturaEntities.stream()
            .map(f -> f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Only CONFIRMADO and CONCILIADO payments count toward the paid total
        BigDecimal totalPagado = pagos.stream()
            .filter(PagoDTO::esValido)
            .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoPendiente = totalFacturado.subtract(totalPagado);

        int facturasPendientes = (int) facturaEntities.stream()
            .filter(f -> f.getEstadoPago() != null && f.getEstadoPago().tieneSaldoPendiente())
            .count();

        return EstadoCuentaClienteDTO.builder()
            .idCliente(cliente.getIdCliente())
            .nombreCliente(cliente.getNombre())
            .identificacion(cliente.getIdentificacion())
            .email(cliente.getEmail())
            .telefono(cliente.getTelefono())
            .totalFacturado(totalFacturado)
            .totalPagado(totalPagado)
            .saldoPendiente(saldoPendiente)
            .totalFacturas(facturaEntities.size())
            .facturasPendientes(facturasPendientes)
            .facturas(facturas)
            .pagos(pagos)
            .build();
    }
}
