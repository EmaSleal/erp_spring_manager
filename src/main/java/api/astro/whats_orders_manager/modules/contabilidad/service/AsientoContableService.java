package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.nomina.model.DetalleNomina;
import api.astro.whats_orders_manager.modules.nomina.model.Nomina;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de asientos contables.
 * Maneja la lógica de negocio para el registro de partida doble.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AsientoContableService {
    
    private final AsientoContableRepository asientoRepository;
    private final DetalleAsientoRepository detalleRepository;
    private final CuentaContableRepository cuentaRepository;
    private final ParametroContableRepository parametroContableRepository;
    
    /**
     * Obtiene todos los asientos con paginación.
     * @param pageable Configuración de paginación
     * @return Página de asientos
     */
    public Page<AsientoContable> obtenerTodos(Pageable pageable) {
        log.debug("Obteniendo todos los asientos");
        return asientoRepository.findAllByOrderByFechaDesc(pageable);
    }
    
    /**
     * Busca un asiento por ID.
     * @param id ID del asiento
     * @return Asiento si existe
     */
    public Optional<AsientoContable> obtenerPorId(Long id) {
        log.debug("Buscando asiento con ID: {}", id);
        return asientoRepository.findById(id);
    }
    
    /**
     * Busca un asiento por número.
     * @param numero Número del asiento
     * @return Asiento si existe
     */
    public Optional<AsientoContable> obtenerPorNumero(String numero) {
        log.debug("Buscando asiento con número: {}", numero);
        return asientoRepository.findByNumero(numero);
    }
    
    /**
     * Obtiene asientos por estado.
     * @param estado Estado del asiento
     * @param pageable Configuración de paginación
     * @return Página de asientos
     */
    public Page<AsientoContable> obtenerPorEstado(EstadoAsiento estado, Pageable pageable) {
        log.debug("Obteniendo asientos con estado: {}", estado);
        return asientoRepository.findByEstado(estado, pageable);
    }
    
    /**
     * Obtiene asientos en un rango de fechas.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de asientos
     */
    public List<AsientoContable> obtenerPorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        log.debug("Obteniendo asientos entre {} y {}", fechaInicio, fechaFin);
        return asientoRepository.findByFechaBetween(fechaInicio, fechaFin);
    }
    
    /**
     * Obtiene asientos contabilizados en un período.
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de asientos contabilizados
     */
    public List<AsientoContable> obtenerContabilizadosPorPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        log.debug("Obteniendo asientos contabilizados entre {} y {}", fechaInicio, fechaFin);
        return asientoRepository.findAsientosContabilizados(fechaInicio, fechaFin);
    }
    
    /**
     * Obtiene asientos relacionados con una factura.
     * @param facturaId ID de la factura
     * @return Lista de asientos
     */
    public List<AsientoContable> obtenerPorFactura(Long facturaId) {
        log.debug("Obteniendo asientos de factura ID: {}", facturaId);
        return asientoRepository.findByFacturaId(facturaId);
    }
    
    /**
     * Obtiene asientos relacionados con un pago.
     * @param pagoId ID del pago
     * @return Lista de asientos
     */
    public List<AsientoContable> obtenerPorPago(Long pagoId) {
        log.debug("Obteniendo asientos de pago ID: {}", pagoId);
        return asientoRepository.findByPagoId(pagoId);
    }
    
    /**
     * Busca asientos por concepto.
     * @param concepto Concepto o parte del concepto
     * @return Lista de asientos que coinciden
     */
    public List<AsientoContable> buscarPorConcepto(String concepto) {
        log.debug("Buscando asientos con concepto: {}", concepto);
        return asientoRepository.findByConceptoContaining(concepto);
    }
    
    /**
     * Crea un nuevo asiento contable en borrador.
     * @param asiento Asiento a crear
     * @return Asiento creado
     */
    @Transactional
    public AsientoContable crear(AsientoContable asiento) {
        log.info("Creando nuevo asiento");
        
        // Generar número si no tiene
        if (asiento.getNumero() == null || asiento.getNumero().isEmpty()) {
            asiento.setNumero(generarNumeroAsiento());
        } else {
            // Validar que el número no exista
            if (asientoRepository.existsByNumero(asiento.getNumero())) {
                throw new IllegalArgumentException("Ya existe un asiento con el número: " + asiento.getNumero());
            }
        }
        
        // Establecer fecha actual si no tiene
        if (asiento.getFecha() == null) {
            asiento.setFecha(LocalDate.now());
        }
        
        // Estado inicial: BORRADOR
        asiento.setEstado(EstadoAsiento.BORRADOR);
        
        // Validar y establecer relación bidireccional con detalles
        if (asiento.getDetalles() != null && !asiento.getDetalles().isEmpty()) {
            validarDetalles(asiento.getDetalles());
            // IMPORTANTE: Establecer la referencia del asiento en cada detalle
            asiento.getDetalles().forEach(detalle -> detalle.setAsiento(asiento));
        }
        
        AsientoContable guardado = asientoRepository.save(asiento);
        log.info("Asiento creado: {} - {}", guardado.getNumero(), guardado.getConcepto());
        
        return guardado;
    }
    
    /**
     * Actualiza un asiento existente.
     * Solo se pueden actualizar asientos en borrador.
     * @param id ID del asiento
     * @param asientoActualizado Datos actualizados
     * @return Asiento actualizado
     */
    @Transactional
    public AsientoContable actualizar(Long id, AsientoContable asientoActualizado) {
        log.info("Actualizando asiento ID: {}", id);
        
        AsientoContable asiento = asientoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));
        
        if (!asiento.puedeModificarse()) {
            throw new IllegalArgumentException(
                "Solo se pueden modificar asientos en estado BORRADOR. Estado actual: " + asiento.getEstado()
            );
        }
        
        // Actualizar campos
        asiento.setFecha(asientoActualizado.getFecha());
        asiento.setConcepto(asientoActualizado.getConcepto());
        asiento.setTipo(asientoActualizado.getTipo());
        
        // Actualizar detalles
        if (asientoActualizado.getDetalles() != null) {
            validarDetalles(asientoActualizado.getDetalles());
            asiento.getDetalles().clear();
            // IMPORTANTE: Establecer la referencia del asiento en cada detalle
            asientoActualizado.getDetalles().forEach(detalle -> detalle.setAsiento(asiento));
            asiento.getDetalles().addAll(asientoActualizado.getDetalles());
        }
        
        AsientoContable guardado = asientoRepository.save(asiento);
        log.info("Asiento actualizado: {}", guardado.getNumero());
        
        return guardado;
    }
    
    /**
     * Agrega un detalle a un asiento en borrador.
     * @param asientoId ID del asiento
     * @param detalle Detalle a agregar
     * @return Asiento actualizado
     */
    @Transactional
    public AsientoContable agregarDetalle(Long asientoId, DetalleAsiento detalle) {
        log.info("Agregando detalle a asiento ID: {}", asientoId);
        
        AsientoContable asiento = asientoRepository.findById(asientoId)
            .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));
        
        if (!asiento.puedeModificarse()) {
            throw new IllegalArgumentException("No se pueden agregar detalles a un asiento " + asiento.getEstado());
        }
        
        // Validar cuenta
        CuentaContable cuenta = cuentaRepository.findById(detalle.getCuenta().getIdCuenta())
            .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        
        if (!cuenta.puedeRecibirMovimientos()) {
            throw new IllegalArgumentException(
                "La cuenta " + cuenta.getCodigo() + " no acepta movimientos directos"
            );
        }
        
        detalle.setAsiento(asiento);
        asiento.getDetalles().add(detalle);
        
        return asientoRepository.save(asiento);
    }
    
    /**
     * Contabiliza un asiento (lo pone en estado CONTABILIZADO).
     * Valida que esté cuadrado antes de contabilizar.
     * @param id ID del asiento
     * @return Asiento contabilizado
     */
    @Transactional
    public AsientoContable contabilizar(Long id) {
        log.info("Contabilizando asiento ID: {}", id);
        
        AsientoContable asiento = asientoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));
        
        if (!asiento.puedeContabilizarse()) {
            throw new IllegalArgumentException(
                "El asiento no puede contabilizarse. Estado actual: " + asiento.getEstado()
            );
        }
        
        // Validar que esté cuadrado
        if (!asiento.estaCuadrado()) {
            throw new IllegalArgumentException(
                String.format("El asiento no está cuadrado. Debe: %s, Haber: %s",
                    asiento.getTotalDebe(), asiento.getTotalHaber())
            );
        }
        
        // Validar que tenga al menos 2 detalles
        if (asiento.getDetalles() == null || asiento.getDetalles().size() < 2) {
            throw new IllegalArgumentException("El asiento debe tener al menos 2 movimientos");
        }
        
        asiento.contabilizar();
        
        AsientoContable guardado = asientoRepository.save(asiento);
        log.info("Asiento contabilizado: {}", guardado.getNumero());
        
        return guardado;
    }
    
    /**
     * Anula un asiento contabilizado.
     * @param id ID del asiento
     * @param motivo Motivo de la anulación
     * @return Asiento anulado
     */
    @Transactional
    public AsientoContable anular(Long id, String motivo) {
        log.info("Anulando asiento ID: {}", id);
        
        AsientoContable asiento = asientoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));
        
        if (asiento.getEstado() != EstadoAsiento.CONTABILIZADO) {
            throw new IllegalArgumentException(
                "Solo se pueden anular asientos contabilizados. Estado actual: " + asiento.getEstado()
            );
        }
        
        asiento.anular(motivo, null);
        
        // Agregar motivo al concepto
        if (motivo != null && !motivo.isEmpty()) {
            asiento.setConcepto(asiento.getConcepto() + " [ANULADO: " + motivo + "]");
        }
        
        AsientoContable guardado = asientoRepository.save(asiento);
        log.info("Asiento anulado: {}", guardado.getNumero());
        
        return guardado;
    }
    
    /**
     * Elimina un asiento en borrador.
     * @param id ID del asiento
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando asiento ID: {}", id);
        
        AsientoContable asiento = asientoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));
        
        if (asiento.getEstado() != EstadoAsiento.BORRADOR) {
            throw new IllegalArgumentException(
                "Solo se pueden eliminar asientos en borrador. Estado actual: " + asiento.getEstado()
            );
        }
        
        asientoRepository.delete(asiento);
        log.info("Asiento eliminado: {}", asiento.getNumero());
    }
    
    /**
     * Genera un asiento automático desde una factura.
     * @param factura Factura origen
     * @param cuentaClienteId ID de la cuenta de clientes
     * @param cuentaVentaId ID de la cuenta de ventas
     * @param cuentaIvaId ID de la cuenta de IVA por pagar
     * @return Asiento generado
     */
    @Transactional
    public AsientoContable generarAsientoVenta(Factura factura, Long cuentaClienteId, 
                                               Long cuentaVentaId, Long cuentaIvaId) {
        log.info("Generando asiento automático para factura: {}", factura.getNumeroFactura());
        
        // Verificar que no exista ya un asiento para esta factura
        if (asientoRepository.existeAsientoVentaParaFactura(Long.getLong(factura.getIdFactura().toString()))) {
            throw new IllegalArgumentException("Ya existe un asiento contable para esta factura");
        }
        
        CuentaContable cuentaCliente = cuentaRepository.findById(cuentaClienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta de clientes no encontrada"));
        CuentaContable cuentaVenta = cuentaRepository.findById(cuentaVentaId)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta de ventas no encontrada"));
        CuentaContable cuentaIva = cuentaRepository.findById(cuentaIvaId)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta de IVA no encontrada"));
        
        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(factura.getFechaEmision());
        asiento.setConcepto("Venta según factura " + factura.getNumeroFactura() + " - " + factura.getCliente().getNombre());
        asiento.setTipo(TipoAsiento.AUTOMATICO_VENTA);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setFactura(factura);
        
        List<DetalleAsiento> detalles = new ArrayList<>();
        
        // Débito: Cuentas por Cobrar (Total factura)
        DetalleAsiento debitoCxC = new DetalleAsiento();
        debitoCxC.setCuenta(cuentaCliente);
        debitoCxC.setDebe(factura.getTotal());
        debitoCxC.setHaber(BigDecimal.ZERO);
        debitoCxC.setDescripcion("Cliente: " + factura.getCliente().getNombre());
        debitoCxC.setAsiento(asiento);
        detalles.add(debitoCxC);
        
        // Crédito: Ventas (Subtotal)
        DetalleAsiento creditoVentas = new DetalleAsiento();
        creditoVentas.setCuenta(cuentaVenta);
        creditoVentas.setDebe(BigDecimal.ZERO);
        creditoVentas.setHaber(factura.getSubtotal());
        creditoVentas.setDescripcion("Venta de mercadería");
        creditoVentas.setAsiento(asiento);
        detalles.add(creditoVentas);
        
        // Crédito: IVA por Pagar (Impuesto)
        if (factura.getImpuesto().compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento creditoIva = new DetalleAsiento();
            creditoIva.setCuenta(cuentaIva);
            creditoIva.setDebe(BigDecimal.ZERO);
            creditoIva.setHaber(factura.getImpuesto());
            creditoIva.setDescripcion("IVA 13%");
            creditoIva.setAsiento(asiento);
            detalles.add(creditoIva);
        }
        
        asiento.setDetalles(detalles);
        
        AsientoContable guardado = asientoRepository.save(asiento);
        
        // Contabilizar automáticamente
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);
        
        log.info("Asiento de venta generado y contabilizado: {}", guardado.getNumero());
        return guardado;
    }
    
    /**
     * Genera un asiento automático desde un pago.
     * @param pago Pago origen
     * @param cuentaClienteId ID de la cuenta de clientes
     * @param cuentaCajaId ID de la cuenta de caja/banco según método de pago
     * @return Asiento generado
     */
    @Transactional
    public AsientoContable generarAsientoPago(Pago pago, Long cuentaClienteId, Long cuentaCajaId) {
        log.info("Generando asiento automático para pago ID: {}", pago.getIdPago());
        
        // Verificar que no exista ya un asiento para este pago
        if (asientoRepository.existeAsientoPagoParaPago(pago.getIdPago())) {
            throw new IllegalArgumentException("Ya existe un asiento contable para este pago");
        }
        
        CuentaContable cuentaCliente = cuentaRepository.findById(cuentaClienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta de clientes no encontrada"));
        CuentaContable cuentaCaja = cuentaRepository.findById(cuentaCajaId)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta de caja/banco no encontrada"));
        
        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(pago.getFechaPago().toLocalDate());
        asiento.setConcepto("Recibo de pago - " + pago.getMetodoPago().getDescripcion() + 
                           " - Factura " + pago.getFactura().getNumeroFactura());
        asiento.setTipo(TipoAsiento.AUTOMATICO_PAGO);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setPago(pago);
        
        List<DetalleAsiento> detalles = new ArrayList<>();
        
        // Débito: Caja/Banco (Monto recibido)
        DetalleAsiento debitoCaja = new DetalleAsiento();
        debitoCaja.setCuenta(cuentaCaja);
        debitoCaja.setDebe(pago.getMonto());
        debitoCaja.setHaber(BigDecimal.ZERO);
        debitoCaja.setDescripcion("Recibo: " + pago.getMetodoPago().getDescripcion());
        debitoCaja.setAsiento(asiento);
        detalles.add(debitoCaja);
        
        // Crédito: Cuentas por Cobrar (Aplicado a cuenta)
        DetalleAsiento creditoCxC = new DetalleAsiento();
        creditoCxC.setCuenta(cuentaCliente);
        creditoCxC.setDebe(BigDecimal.ZERO);
        creditoCxC.setHaber(pago.getMonto());
        creditoCxC.setDescripcion("Cliente: " + pago.getFactura().getCliente().getNombre());
        creditoCxC.setAsiento(asiento);
        detalles.add(creditoCxC);
        
        asiento.setDetalles(detalles);
        
        AsientoContable guardado = asientoRepository.save(asiento);
        
        // Contabilizar automáticamente
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);
        
        log.info("Asiento de pago generado y contabilizado: {}", guardado.getNumero());
        return guardado;
    }
    
    // ==================== CPP / SUPPLIER PAYMENT GENERATORS ====================

    /**
     * Generates a balanced purchase journal entry for a CPP created from a
     * received purchase order.
     *
     * Entry structure:
     *   DR Compras         — oc.subtotal
     *   DR IVA Crédito     — oc.impuesto (only when > 0)
     *   CR Cuentas x Pagar — oc.total
     *
     * No-op (returns null) when {@code cpp.getOrdenCompra()} is null, because
     * manually created CPPs carry no subtotal/IVA breakdown.
     *
     * @param cpp the newly saved CuentaPorPagar
     * @return the posted AsientoContable, or null if skipped
     */
    @Transactional
    public AsientoContable generarAsientoCompra(CuentaPorPagar cpp) {
        if (cpp.getOrdenCompra() == null) {
            log.warn("CPP {} has no OrdenCompra — skipping purchase journal entry", cpp.getNumero());
            return null;
        }

        var oc = cpp.getOrdenCompra();

        CuentaContable cuentaCompras   = resolverCuenta("CPP_CUENTA_COMPRAS");
        CuentaContable cuentaPorPagar  = resolverCuenta("CPP_CUENTA_POR_PAGAR");

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(java.time.LocalDate.now());
        asiento.setConcepto("Compra según OC " + oc.getNumero() + " - " + cpp.getProveedor().getNombre());
        asiento.setTipo(TipoAsiento.AUTOMATICO_COMPRA);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setCuentaPorPagar(cpp);

        java.util.List<DetalleAsiento> detalles = new java.util.ArrayList<>();

        // DR Compras (subtotal)
        DetalleAsiento drCompras = new DetalleAsiento();
        drCompras.setCuenta(cuentaCompras);
        drCompras.setDebe(oc.getSubtotal());
        drCompras.setHaber(java.math.BigDecimal.ZERO);
        drCompras.setDescripcion("Compras — OC " + oc.getNumero());
        drCompras.setAsiento(asiento);
        detalles.add(drCompras);

        // DR IVA Crédito Fiscal (when applicable)
        if (oc.getImpuesto() != null && oc.getImpuesto().compareTo(java.math.BigDecimal.ZERO) > 0) {
            CuentaContable cuentaIva = resolverCuenta("CPP_IVA_CREDITO_FISCAL");
            DetalleAsiento drIva = new DetalleAsiento();
            drIva.setCuenta(cuentaIva);
            drIva.setDebe(oc.getImpuesto());
            drIva.setHaber(java.math.BigDecimal.ZERO);
            drIva.setDescripcion("IVA Crédito Fiscal — OC " + oc.getNumero());
            drIva.setAsiento(asiento);
            detalles.add(drIva);
        }

        // CR Cuentas por Pagar (total)
        DetalleAsiento crCxP = new DetalleAsiento();
        crCxP.setCuenta(cuentaPorPagar);
        crCxP.setDebe(java.math.BigDecimal.ZERO);
        crCxP.setHaber(oc.getTotal());
        crCxP.setDescripcion("Cuentas por Pagar — " + cpp.getProveedor().getNombre());
        crCxP.setAsiento(asiento);
        detalles.add(crCxP);

        asiento.setDetalles(detalles);

        if (!asiento.estaCuadrado()) {
            throw new IllegalStateException(
                String.format("Purchase journal entry is not balanced — debe: %s, haber: %s",
                    asiento.getTotalDebe(), asiento.getTotalHaber()));
        }

        AsientoContable guardado = asientoRepository.save(asiento);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Purchase journal entry {} posted for CPP {}", guardado.getNumero(), cpp.getNumero());
        return guardado;
    }

    /**
     * Generates a balanced supplier-payment journal entry.
     *
     * Entry structure:
     *   DR Cuentas x Pagar — pago.monto
     *   CR Banco/Caja       — pago.monto (account resolved from MetodoPagoProveedor)
     *
     * @param pago the newly saved PagoProveedor
     * @return the posted AsientoContable
     */
    @Transactional
    public AsientoContable generarAsientoPagoProveedor(PagoProveedor pago) {
        String bancoKey = switch (pago.getMetodoPago()) {
            case TRANSFERENCIA -> "CPP_BANCO_TRANSFERENCIA";
            case CHEQUE        -> "CPP_BANCO_CHEQUE";
            case EFECTIVO      -> "CPP_BANCO_EFECTIVO";
            case SINPE         -> "CPP_BANCO_SINPE";
            case DEPOSITO      -> "CPP_BANCO_DEPOSITO";
        };

        CuentaContable cuentaPorPagar = resolverCuenta("CPP_CUENTA_POR_PAGAR");
        CuentaContable cuentaBanco    = resolverCuenta(bancoKey);

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(pago.getFecha() != null ? pago.getFecha() : java.time.LocalDate.now());
        asiento.setConcepto("Pago proveedor " + pago.getNumero() + " — " + pago.getMetodoPago());
        asiento.setTipo(TipoAsiento.AUTOMATICO_GASTO);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setPagoProveedor(pago);

        java.util.List<DetalleAsiento> detalles = new java.util.ArrayList<>();

        // DR Cuentas por Pagar
        DetalleAsiento drCxP = new DetalleAsiento();
        drCxP.setCuenta(cuentaPorPagar);
        drCxP.setDebe(pago.getMonto());
        drCxP.setHaber(java.math.BigDecimal.ZERO);
        drCxP.setDescripcion("Pago a proveedor — " + pago.getNumero());
        drCxP.setAsiento(asiento);
        detalles.add(drCxP);

        // CR Banco/Caja
        DetalleAsiento crBanco = new DetalleAsiento();
        crBanco.setCuenta(cuentaBanco);
        crBanco.setDebe(java.math.BigDecimal.ZERO);
        crBanco.setHaber(pago.getMonto());
        crBanco.setDescripcion(pago.getMetodoPago() + " — " + pago.getNumero());
        crBanco.setAsiento(asiento);
        detalles.add(crBanco);

        asiento.setDetalles(detalles);

        if (!asiento.estaCuadrado()) {
            throw new IllegalStateException(
                String.format("Supplier payment journal entry is not balanced — debe: %s, haber: %s",
                    asiento.getTotalDebe(), asiento.getTotalHaber()));
        }

        AsientoContable guardado = asientoRepository.save(asiento);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Supplier payment journal entry {} posted for pago {}", guardado.getNumero(), pago.getNumero());
        return guardado;
    }

    /**
     * Generates a balanced payroll journal entry for a Nomina in APROBADA state.
     *
     * Entry structure:
     *   DR NOMINA_GASTO_SUELDOS        — nomina.totalBruto
     *   DR NOMINA_CCSS_PATRONAL_POR_PAGAR — nomina.totalCcssPatronal
     *   CR NOMINA_CCSS_POR_PAGAR       — sum(detalles.ccssObrero)
     *   CR NOMINA_INS_POR_PAGAR        — sum(detalles.ins)
     *   CR NOMINA_RENTA_POR_PAGAR      — sum(detalles.impuestoRenta)
     *   CR NOMINA_SALARIOS_POR_PAGAR   — nomina.totalNeto
     *
     * @param nomina the approved Nomina to post
     * @return the posted AsientoContable
     * @throws IllegalStateException if any NOMINA_* key is not configured, or entry does not balance
     */
    @Transactional
    public AsientoContable generarAsientoNomina(Nomina nomina) {
        // Resolve all 6 accounts — fail fast if any missing
        CuentaContable cuentaGastoSueldos  = resolverCuenta("NOMINA_GASTO_SUELDOS");
        CuentaContable cuentaCcssPatronal  = resolverCuenta("NOMINA_CCSS_PATRONAL_POR_PAGAR");
        CuentaContable cuentaCcssObrero    = resolverCuenta("NOMINA_CCSS_POR_PAGAR");
        CuentaContable cuentaIns           = resolverCuenta("NOMINA_INS_POR_PAGAR");
        CuentaContable cuentaRenta         = resolverCuenta("NOMINA_RENTA_POR_PAGAR");
        CuentaContable cuentaSalarios      = resolverCuenta("NOMINA_SALARIOS_POR_PAGAR");

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(LocalDate.now());
        asiento.setConcepto("Nómina " + nomina.getNumero() + " — período "
                + nomina.getPeriodoInicio() + " / " + nomina.getPeriodoFin());
        asiento.setTipo(TipoAsiento.AUTOMATICO_NOMINA);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setNomina(nomina);

        List<DetalleAsiento> detalles = new ArrayList<>();

        // DR 1: Gasto Sueldos (totalBruto)
        if (nomina.getTotalBruto().compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento drSueldos = new DetalleAsiento();
            drSueldos.setCuenta(cuentaGastoSueldos);
            drSueldos.setDebe(nomina.getTotalBruto());
            drSueldos.setDescripcion("Sueldos brutos — " + nomina.getNumero());
            drSueldos.setAsiento(asiento);
            detalles.add(drSueldos);
        }

        // DR 2: Patronal CCSS cost — expense on the same gasto account, separate line for ledger clarity
        if (nomina.getTotalCcssPatronal().compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento drCcssPatronal = new DetalleAsiento();
            drCcssPatronal.setCuenta(cuentaGastoSueldos);
            drCcssPatronal.setDebe(nomina.getTotalCcssPatronal());
            drCcssPatronal.setDescripcion("Carga patronal CCSS — " + nomina.getNumero());
            drCcssPatronal.setAsiento(asiento);
            detalles.add(drCcssPatronal);
        }

        // CR 1: CCSS Obrero por pagar — sum across detalles
        BigDecimal totalCcssObrero = nomina.getDetalles().stream()
                .map(DetalleNomina::getCcssObrero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalCcssObrero.compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento crCcss = new DetalleAsiento();
            crCcss.setCuenta(cuentaCcssObrero);
            crCcss.setHaber(totalCcssObrero);
            crCcss.setDescripcion("CCSS obrero por pagar — " + nomina.getNumero());
            crCcss.setAsiento(asiento);
            detalles.add(crCcss);
        }

        // CR 2: INS por pagar — sum across detalles
        BigDecimal totalIns = nomina.getDetalles().stream()
                .map(DetalleNomina::getIns)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalIns.compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento crIns = new DetalleAsiento();
            crIns.setCuenta(cuentaIns);
            crIns.setHaber(totalIns);
            crIns.setDescripcion("INS por pagar — " + nomina.getNumero());
            crIns.setAsiento(asiento);
            detalles.add(crIns);
        }

        // CR 3: Renta por pagar — sum across detalles (only when > 0)
        BigDecimal totalRenta = nomina.getDetalles().stream()
                .map(DetalleNomina::getImpuestoRenta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRenta.compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento crRenta = new DetalleAsiento();
            crRenta.setCuenta(cuentaRenta);
            crRenta.setHaber(totalRenta);
            crRenta.setDescripcion("Impuesto renta por pagar — " + nomina.getNumero());
            crRenta.setAsiento(asiento);
            detalles.add(crRenta);
        }

        // CR 4: Salarios netos por pagar (totalNeto)
        if (nomina.getTotalNeto().compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento crSalarios = new DetalleAsiento();
            crSalarios.setCuenta(cuentaSalarios);
            crSalarios.setHaber(nomina.getTotalNeto());
            crSalarios.setDescripcion("Salarios netos por pagar — " + nomina.getNumero());
            crSalarios.setAsiento(asiento);
            detalles.add(crSalarios);
        }

        // CR 5: CCSS Patronal liability (balances DR 2)
        if (nomina.getTotalCcssPatronal().compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento crCcssPatronal = new DetalleAsiento();
            crCcssPatronal.setCuenta(cuentaCcssPatronal);
            crCcssPatronal.setHaber(nomina.getTotalCcssPatronal());
            crCcssPatronal.setDescripcion("CCSS patronal por pagar — " + nomina.getNumero());
            crCcssPatronal.setAsiento(asiento);
            detalles.add(crCcssPatronal);
        }

        asiento.setDetalles(detalles);

        if (!asiento.estaCuadrado()) {
            throw new IllegalStateException(
                    String.format("Asiento de nómina no cuadra — debe: %s, haber: %s",
                            asiento.getTotalDebe(), asiento.getTotalHaber()));
        }

        AsientoContable guardado = asientoRepository.save(asiento);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Asiento de nómina {} generado y contabilizado: {}",
                nomina.getNumero(), guardado.getNumero());
        return guardado;
    }

    /**
     * Resolves the CuentaContable mapped to the given accounting-event key.
     *
     * @param clave accounting event key (e.g. "CPP_CUENTA_COMPRAS")
     * @return the associated CuentaContable
     * @throws IllegalStateException if the key is not configured in parametros_contables
     */
    private CuentaContable resolverCuenta(String clave) {
        return parametroContableRepository.findByClave(clave)
            .map(ParametroContable::getCuentaContable)
            .orElseThrow(() -> new IllegalStateException("Parámetro contable no configurado: " + clave));
    }

    // ==================== UTILITIES ====================

    /**
     * Genera el número de asiento automático.
     * Formato: ASI-YYYY-####
     * @return Número generado
     */
    private String generarNumeroAsiento() {
        String anio = String.valueOf(LocalDate.now().getYear());
        Optional<String> ultimoNumero = asientoRepository.findUltimoNumeroDelAnio(anio);
        
        int siguiente = 1;
        if (ultimoNumero.isPresent()) {
            String[] partes = ultimoNumero.get().split("-");
            if (partes.length == 3) {
                siguiente = Integer.parseInt(partes[2]) + 1;
            }
        }
        
        return String.format("ASI-%s-%04d", anio, siguiente);
    }
    
    /**
     * Valida los detalles de un asiento.
     * @param detalles Lista de detalles
     * @throws IllegalArgumentException si hay errores de validación
     */
    private void validarDetalles(List<DetalleAsiento> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("El asiento debe tener al menos un detalle");
        }
        
        for (DetalleAsiento detalle : detalles) {
            if (detalle.getCuenta() == null) {
                throw new IllegalArgumentException("Todos los detalles deben tener una cuenta");
            }
            
            CuentaContable cuenta = cuentaRepository.findById(detalle.getCuenta().getIdCuenta())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + detalle.getCuenta().getIdCuenta()));
            
            if (!cuenta.puedeRecibirMovimientos()) {
                throw new IllegalArgumentException(
                    "La cuenta " + cuenta.getCodigo() + " - " + cuenta.getNombre() + " no acepta movimientos directos"
                );
            }
        }
    }
}
