package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.util.ConversionMoneda;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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
    
    // ==================== VENTAS Y COBROS (ADR-008) ====================

    /** Clave de la cuenta de cuentas por cobrar a clientes (DR en venta, CR en cobro). */
    public static final String CLAVE_VENTA_CXC_CLIENTES = "VENTA_CXC_CLIENTES";
    /** Clave de la cuenta de ingresos por ventas (CR en venta). */
    public static final String CLAVE_VENTA_INGRESOS = "VENTA_INGRESOS";
    /** Clave de la cuenta de IVA por pagar (CR en venta). */
    public static final String CLAVE_VENTA_IVA_POR_PAGAR = "VENTA_IVA_POR_PAGAR";
    /** Clave de la cuenta de anticipos de clientes (CR en cobro sin factura). */
    public static final String CLAVE_COBRO_ANTICIPOS_CLIENTES = "COBRO_ANTICIPOS_CLIENTES";
    /** Prefijo de las claves de caja/banco por método de pago: COBRO_CAJA_&lt;MetodoPago&gt;. */
    public static final String PREFIJO_COBRO_CAJA = "COBRO_CAJA_";

    /** Clave de la cuenta de ingreso por diferencial cambiario realizado (ADR-011). */
    public static final String CLAVE_DIFERENCIAL_CAMBIARIO_INGRESO = "DIFERENCIAL_CAMBIARIO_INGRESO";
    /** Clave de la cuenta de gasto por diferencial cambiario realizado (ADR-011). */
    public static final String CLAVE_DIFERENCIAL_CAMBIARIO_GASTO = "DIFERENCIAL_CAMBIARIO_GASTO";

    /** Tolerancia de redondeo (en CRC) que se ajusta en la línea de ingresos. */
    private static final BigDecimal TOLERANCIA_REDONDEO = new BigDecimal("0.02");

    /**
     * Genera y contabiliza el asiento de venta de una factura (ADR-008, regla 2).
     *
     * Estructura (montos en CRC, ADR-004):
     *   DR VENTA_CXC_CLIENTES  — total
     *   CR VENTA_INGRESOS      — subtotal (absorbe el ajuste de redondeo)
     *   CR VENTA_IVA_POR_PAGAR — impuesto (solo si &gt; 0)
     *
     * Si la factura no está en CRC, cada línea se multiplica por factura.tipoCambio
     * (HALF_UP, 2 decimales).
     *
     * Fecha (ADR-014): la de emisión de la factura, nunca la del sistema.
     *
     * @param factura Factura origen (ya persistida)
     * @return Asiento contabilizado, o null si la factura tiene total cero
     * @throws IllegalStateException si la factura no tiene fecha de emisión, falta una clave contable,
     *                               el tipo de cambio es inválido o el asiento no cuadra
     */
    @Transactional
    public AsientoContable generarAsientoVenta(Factura factura) {
        if (factura == null || factura.getIdFactura() == null) {
            throw new IllegalArgumentException("La factura debe estar persistida para generar su asiento de venta");
        }
        log.info("Generando asiento automático para factura: {}", factura.getNumeroFactura());

        // Verificar que no exista ya un asiento vigente para esta factura
        if (asientoRepository.existeAsientoVentaParaFactura(factura.getIdFactura().longValue())) {
            throw new IllegalArgumentException("Ya existe un asiento contable para esta factura");
        }

        AsientoContable asiento = construirAsientoVenta(factura);
        if (asiento == null) {
            log.warn("Factura {} con total cero — no se genera asiento de venta", factura.getNumeroFactura());
            return null;
        }

        validarCuadre(asiento, "venta");

        AsientoContable guardado = asientoRepository.save(asiento);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Asiento de venta generado y contabilizado: {}", guardado.getNumero());
        return guardado;
    }

    /**
     * Genera y contabiliza el asiento de cobro de un pago (ADR-008 regla 3, ADR-011).
     *
     * Estructura (montos en CRC):
     *   DR COBRO_CAJA_&lt;MetodoPago&gt;          — monto del pago en CRC al tipo de cambio del pago
     *   CR VENTA_CXC_CLIENTES (con factura)   — montoAplicado × factura.tipoCambio
     *   CR COBRO_ANTICIPOS_CLIENTES (adelanto) — monto del pago en CRC (sin diferencial)
     *   CR DIFERENCIAL_CAMBIARIO_INGRESO      — si caja &gt; CxC
     *   DR DIFERENCIAL_CAMBIARIO_GASTO        — si caja &lt; CxC
     *
     * Pagos anteriores a ADR-011 (sin moneda/tipoCambio/montoAplicado) se tratan como pagos en la
     * moneda de la factura al tipo de cambio de la factura (CRC para adelantos): sin diferencial.
     *
     * Fecha (ADR-014): la del pago (pago.fechaPago), nunca la del sistema.
     *
     * @param pago Pago origen en estado CONFIRMADO o CONCILIADO
     * @return Asiento contabilizado
     * @throws IllegalStateException si el pago no es válido o no tiene fecha, falta una clave contable
     *                               (incluidas DIFERENCIAL_CAMBIARIO_* cuando hay diferencia) o no cuadra
     */
    @Transactional
    public AsientoContable generarAsientoPago(Pago pago) {
        if (pago == null || pago.getIdPago() == null) {
            throw new IllegalArgumentException("El pago debe estar persistido para generar su asiento de cobro");
        }
        log.info("Generando asiento automático para pago ID: {}", pago.getIdPago());

        if (!pago.esValido()) {
            throw new IllegalStateException(
                "Solo se contabilizan pagos CONFIRMADOS o CONCILIADOS. Estado actual: " + pago.getEstado());
        }

        // Verificar que no exista ya un asiento vigente para este pago
        if (asientoRepository.existeAsientoPagoParaPago(pago.getIdPago())) {
            throw new IllegalArgumentException("Ya existe un asiento contable para este pago");
        }

        if (pago.getMetodoPago() == null) {
            throw new IllegalStateException("El pago " + pago.getNumeroPago() + " no tiene método de pago");
        }
        LocalDate fecha = fechaEconomica(pago.getFechaPago() != null ? pago.getFechaPago().toLocalDate() : null,
            "El pago " + pago.getNumeroPago(), "fechaPago");

        Factura factura = pago.getFactura();
        CuentaContable cuentaCaja = resolverCuenta(PREFIJO_COBRO_CAJA + pago.getMetodoPago().name());
        CuentaContable cuentaCredito = factura != null
            ? resolverCuenta(CLAVE_VENTA_CXC_CLIENTES)
            : resolverCuenta(CLAVE_COBRO_ANTICIPOS_CLIENTES);

        // ADR-011: moneda y tipo de cambio propios del pago
        MonedaFE monedaDocumento = factura != null ? ConversionMoneda.oCrc(factura.getMonedaFE()) : null;
        MonedaFE monedaPago = pago.getMoneda() != null ? pago.getMoneda()
            : (monedaDocumento != null ? monedaDocumento : MonedaFE.CRC);
        BigDecimal tipoCambioPago = tipoCambioDelPago(pago, monedaPago, monedaDocumento, factura);
        BigDecimal montoCajaCrc = ConversionMoneda.montoEnCrc(pago.getMonto(), monedaPago, tipoCambioPago);

        BigDecimal creditoCrc;
        if (factura != null) {
            BigDecimal montoAplicado = pago.getMontoAplicado() != null
                ? pago.getMontoAplicado()
                : ConversionMoneda.montoAplicado(pago.getMonto(), monedaPago, monedaDocumento, tipoCambioPago);
            creditoCrc = convertirACrc(montoAplicado, resolverTipoCambio(factura));
        } else {
            // Adelanto sin factura: se acredita al tipo de cambio del pago, sin diferencial
            creditoCrc = montoCajaCrc;
        }

        String nombreCliente = nombreCliente(pago.getCliente() != null ? pago.getCliente()
            : (factura != null ? factura.getCliente() : null));
        String origen = factura != null ? "Factura " + factura.getNumeroFactura() : "Adelanto sin factura";

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(fecha);
        asiento.setConcepto(truncar("Cobro " + pago.getNumeroPago() + " - " + pago.getMetodoPago().getDescripcion()
            + " - " + origen + nombreCliente, 500));
        asiento.setTipo(TipoAsiento.AUTOMATICO_PAGO);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setPago(pago);

        List<DetalleAsiento> detalles = new ArrayList<>();

        // Débito: Caja/Banco según método de pago
        detalles.add(linea(asiento, cuentaCaja, montoCajaCrc, null,
            "Recibo: " + pago.getMetodoPago().getDescripcion()));

        // Crédito: CxC clientes (con factura) o anticipos de clientes (adelanto)
        detalles.add(linea(asiento, cuentaCredito, null, creditoCrc,
            factura != null ? "Aplicado a " + origen : "Anticipo de cliente"));

        // Diferencial cambiario realizado: lo recibido en CRC vs lo que se rebaja de la CxC
        agregarDiferencialCambiario(asiento, detalles, montoCajaCrc.subtract(creditoCrc),
            "Cobro " + pago.getNumeroPago());

        asiento.setDetalles(detalles);

        validarCuadre(asiento, "cobro");

        AsientoContable guardado = asientoRepository.save(asiento);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Asiento de pago generado y contabilizado: {}", guardado.getNumero());
        return guardado;
    }

    /**
     * Tipo de cambio del pago (ADR-011). Si el pago no lo trae (pagos anteriores a ADR-011) se usa
     * el de la factura, o 1 para adelantos en CRC.
     * @throws IllegalStateException si hay moneda extranjera involucrada y no hay tipo de cambio válido
     */
    private BigDecimal tipoCambioDelPago(Pago pago, MonedaFE monedaPago, MonedaFE monedaDocumento, Factura factura) {
        MonedaFE extranjera;
        try {
            extranjera = ConversionMoneda.monedaExtranjera(monedaPago,
                monedaDocumento != null ? monedaDocumento : monedaPago);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        if (extranjera == MonedaFE.CRC) {
            return BigDecimal.ONE;
        }
        BigDecimal tipoCambio = pago.getTipoCambio();
        if (tipoCambio == null && factura != null) {
            tipoCambio = resolverTipoCambio(factura);
        }
        if (tipoCambio == null || tipoCambio.signum() <= 0) {
            throw new IllegalStateException(String.format(
                "El pago %s involucra %s y requiere un tipo de cambio mayor a 0 para contabilizarse en CRC",
                pago.getNumeroPago(), extranjera.getCodigo()));
        }
        return tipoCambio;
    }

    /**
     * Agrega la línea de diferencial cambiario realizado (ADR-011).
     * @param ganancia &gt; 0 → CR DIFERENCIAL_CAMBIARIO_INGRESO; &lt; 0 → DR DIFERENCIAL_CAMBIARIO_GASTO; 0 → nada
     * @throws IllegalStateException si la clave contable requerida no está configurada
     */
    private void agregarDiferencialCambiario(AsientoContable asiento, List<DetalleAsiento> detalles,
                                             BigDecimal ganancia, String referencia) {
        if (ganancia.signum() > 0) {
            detalles.add(linea(asiento, resolverCuenta(CLAVE_DIFERENCIAL_CAMBIARIO_INGRESO), null, ganancia,
                "Diferencial cambiario (ingreso) — " + referencia));
        } else if (ganancia.signum() < 0) {
            detalles.add(linea(asiento, resolverCuenta(CLAVE_DIFERENCIAL_CAMBIARIO_GASTO), ganancia.negate(), null,
                "Diferencial cambiario (gasto) — " + referencia));
        }
    }

    private static DetalleAsiento linea(AsientoContable asiento, CuentaContable cuenta,
                                        BigDecimal debe, BigDecimal haber, String descripcion) {
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setCuenta(cuenta);
        if (debe != null) {
            detalle.setDebe(debe);
        }
        if (haber != null) {
            detalle.setHaber(haber);
        }
        detalle.setDescripcion(truncar(descripcion, 500));
        detalle.setAsiento(asiento);
        return detalle;
    }

    /**
     * Genera y contabiliza el asiento de reversión de un asiento contabilizado (ADR-008, regla 4).
     * Crea líneas espejo (debe ↔ haber) con el mismo tipo y los mismos vínculos de origen,
     * y referencia el asiento original en {@code asientoRevertido} y en el concepto.
     * El asiento original no se edita ni se borra.
     *
     * @param original Asiento contabilizado a revertir
     * @param motivo   Motivo de la reversión
     * @return Asiento de reversión contabilizado
     * @throws IllegalStateException si el original no es reversible o la reversión no cuadra
     */
    @Transactional
    public AsientoContable generarAsientoReversion(AsientoContable original, String motivo) {
        if (original == null) {
            throw new IllegalArgumentException("El asiento a revertir es obligatorio");
        }
        if (original.getEstado() != EstadoAsiento.CONTABILIZADO) {
            throw new IllegalStateException(
                "Solo se revierten asientos contabilizados. Estado actual: " + original.getEstado());
        }
        if (original.getAsientoRevertido() != null) {
            throw new IllegalStateException("El asiento " + original.getNumero() + " es una reversión y no puede revertirse");
        }
        if (original.getIdAsiento() != null
                && asientoRepository.existsByAsientoRevertidoIdAsiento(original.getIdAsiento())) {
            throw new IllegalStateException("El asiento " + original.getNumero() + " ya fue revertido");
        }

        log.info("Generando reversión del asiento {}", original.getNumero());

        AsientoContable reversion = new AsientoContable();
        reversion.setNumero(generarNumeroAsiento());
        reversion.setFecha(LocalDate.now());
        String sufijoMotivo = (motivo != null && !motivo.isBlank()) ? " - " + motivo.trim() : "";
        reversion.setConcepto(truncar("Reversión del asiento " + original.getNumero() + sufijoMotivo, 500));
        reversion.setTipo(original.getTipo());
        reversion.setEstado(EstadoAsiento.BORRADOR);
        reversion.setFactura(original.getFactura());
        reversion.setPago(original.getPago());
        reversion.setCuentaPorPagar(original.getCuentaPorPagar());
        reversion.setPagoProveedor(original.getPagoProveedor());
        reversion.setNomina(original.getNomina());
        reversion.setAsientoRevertido(original);

        List<DetalleAsiento> detalles = new ArrayList<>();
        for (DetalleAsiento linea : original.getDetalles()) {
            DetalleAsiento espejo = new DetalleAsiento();
            espejo.setCuenta(linea.getCuenta());
            // Líneas espejo: el debe original pasa al haber y viceversa
            espejo.setDebe(linea.getHaber());
            espejo.setHaber(linea.getDebe());
            espejo.setDescripcion(truncar("Reversión: " + (linea.getDescripcion() != null ? linea.getDescripcion() : ""), 500));
            espejo.setAsiento(reversion);
            detalles.add(espejo);
        }
        reversion.setDetalles(detalles);

        validarCuadre(reversion, "reversión");

        AsientoContable guardado = asientoRepository.save(reversion);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Asiento de reversión {} generado para {}", guardado.getNumero(), original.getNumero());
        return guardado;
    }

    /**
     * Revierte los asientos de venta vigentes de una factura.
     * @param factura Factura origen
     * @param motivo  Motivo de la reversión
     * @return Asientos de reversión generados (vacío si no había asiento vigente)
     */
    @Transactional
    public List<AsientoContable> revertirAsientoVenta(Factura factura, String motivo) {
        List<AsientoContable> reversiones = new ArrayList<>();
        for (AsientoContable vigente : asientoRepository.findAsientosVentaVigentes(factura.getIdFactura().longValue())) {
            reversiones.add(generarAsientoReversion(vigente, motivo));
        }
        return reversiones;
    }

    /**
     * Revierte los asientos de cobro vigentes de un pago.
     * @param pago   Pago origen
     * @param motivo Motivo de la reversión
     * @return Asientos de reversión generados (vacío si el pago no estaba contabilizado)
     */
    @Transactional
    public List<AsientoContable> revertirAsientoPago(Pago pago, String motivo) {
        List<AsientoContable> reversiones = new ArrayList<>();
        for (AsientoContable vigente : asientoRepository.findAsientosPagoVigentes(pago.getIdPago())) {
            reversiones.add(generarAsientoReversion(vigente, motivo));
        }
        return reversiones;
    }

    /**
     * Indica si el pago ya tiene un asiento de cobro vigente.
     * @param pago Pago a verificar
     * @return true si existe un asiento de cobro vigente
     */
    public boolean tieneAsientoPagoVigente(Pago pago) {
        return pago.getIdPago() != null && asientoRepository.existeAsientoPagoParaPago(pago.getIdPago());
    }

    /**
     * Indica si la factura tiene algún asiento contabilizado vinculado.
     * @param facturaId ID de la factura
     * @return true si existe al menos un asiento contabilizado
     */
    public boolean tieneAsientoContabilizadoFactura(Integer facturaId) {
        return facturaId != null && asientoRepository.existeAsientoContabilizadoParaFactura(facturaId.longValue());
    }

    /**
     * Indica si el pago tiene algún asiento contabilizado vinculado.
     * @param pagoId ID del pago
     * @return true si existe al menos un asiento contabilizado
     */
    public boolean tieneAsientoContabilizadoPago(Long pagoId) {
        return pagoId != null && asientoRepository.existeAsientoContabilizadoParaPago(pagoId);
    }

    /**
     * Sincroniza el asiento de venta tras editar una factura ya contabilizada.
     * Si los montos en CRC (subtotal, impuesto, total, moneda o tipo de cambio) cambiaron
     * respecto del asiento vigente, revierte el asiento vigente y genera uno nuevo.
     * Las facturas sin asiento vigente (históricas) no se contabilizan aquí.
     *
     * @param factura Factura editada
     * @return El nuevo asiento de venta, o null si no hubo cambios o no había asiento
     */
    @Transactional
    public AsientoContable sincronizarAsientoVenta(Factura factura) {
        if (factura == null || factura.getIdFactura() == null) {
            return null;
        }
        List<AsientoContable> vigentes = asientoRepository.findAsientosVentaVigentes(factura.getIdFactura().longValue());
        if (vigentes.isEmpty()) {
            return null;
        }

        AsientoContable esperado = construirAsientoVenta(factura);
        boolean sinCambios = vigentes.size() == 1 && esperado != null
            && resumenPorCuenta(vigentes.get(0)).equals(resumenPorCuenta(esperado));
        if (sinCambios) {
            return null;
        }

        log.info("Montos de la factura {} cambiaron — se revierte y regenera el asiento de venta",
            factura.getNumeroFactura());
        for (AsientoContable vigente : vigentes) {
            generarAsientoReversion(vigente, "Modificación de montos de la factura " + factura.getNumeroFactura());
        }
        return generarAsientoVenta(factura);
    }

    /**
     * Contabiliza el asiento de venta tras guardar las líneas de una factura.
     *
     * El flujo de creación guarda primero la factura sin líneas (total cero, sin asiento)
     * y luego sus líneas. Si la factura no tenía líneas antes de este guardado
     * ({@code primeraCargaDeLineas}) y no tiene asiento de venta vigente, se genera aquí.
     * En cualquier otro caso se delega en {@link #sincronizarAsientoVenta(Factura)}, de modo
     * que las facturas históricas (con líneas y sin asiento) siguen sin contabilizarse.
     *
     * @param factura              Factura con totales ya actualizados
     * @param primeraCargaDeLineas true si la factura no tenía líneas antes del guardado
     * @return El asiento generado, o null si no hubo nada que contabilizar
     */
    @Transactional
    public AsientoContable contabilizarVentaTrasGuardarLineas(Factura factura, boolean primeraCargaDeLineas) {
        if (factura == null || factura.getIdFactura() == null) {
            return null;
        }
        if (primeraCargaDeLineas
                && !asientoRepository.existeAsientoVentaParaFactura(factura.getIdFactura().longValue())) {
            return generarAsientoVenta(factura);
        }
        return sincronizarAsientoVenta(factura);
    }

    /**
     * Construye (sin persistir ni numerar) el asiento de venta de una factura, con montos en CRC.
     * @return Asiento en borrador, o null si el total es cero
     */
    private AsientoContable construirAsientoVenta(Factura factura) {
        BigDecimal tipoCambio = resolverTipoCambio(factura);

        BigDecimal total = valorONulo(factura.getTotal());
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        LocalDate fecha = fechaEconomica(factura.getFechaEmision(),
            "La factura " + factura.getNumeroFactura(), "fechaEmision");
        BigDecimal subtotal = valorONulo(factura.getSubtotal());
        BigDecimal impuesto = valorONulo(factura.getImpuesto());

        BigDecimal totalCrc = convertirACrc(total, tipoCambio);
        BigDecimal impuestoCrc = convertirACrc(impuesto, tipoCambio);
        BigDecimal ingresosCrc = convertirACrc(subtotal, tipoCambio);

        // Ajuste del centavo: la diferencia de redondeo se lleva a la línea de ingresos
        BigDecimal diferencia = totalCrc.subtract(ingresosCrc).subtract(impuestoCrc);
        if (diferencia.signum() != 0 && diferencia.abs().compareTo(TOLERANCIA_REDONDEO) <= 0) {
            ingresosCrc = ingresosCrc.add(diferencia);
        }

        CuentaContable cuentaCxC = resolverCuenta(CLAVE_VENTA_CXC_CLIENTES);
        CuentaContable cuentaIngresos = resolverCuenta(CLAVE_VENTA_INGRESOS);

        String nombreCliente = nombreCliente(factura.getCliente());

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(fecha);
        asiento.setConcepto(truncar("Venta según factura " + factura.getNumeroFactura() + nombreCliente, 500));
        asiento.setTipo(TipoAsiento.AUTOMATICO_VENTA);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setFactura(factura);

        List<DetalleAsiento> detalles = new ArrayList<>();

        // Débito: Cuentas por Cobrar (Total factura)
        DetalleAsiento debitoCxC = new DetalleAsiento();
        debitoCxC.setCuenta(cuentaCxC);
        debitoCxC.setDebe(totalCrc);
        debitoCxC.setDescripcion("Cuentas por cobrar" + nombreCliente);
        debitoCxC.setAsiento(asiento);
        detalles.add(debitoCxC);

        // Crédito: Ingresos por ventas (Subtotal + ajuste de redondeo)
        if (ingresosCrc.compareTo(BigDecimal.ZERO) > 0) {
            DetalleAsiento creditoVentas = new DetalleAsiento();
            creditoVentas.setCuenta(cuentaIngresos);
            creditoVentas.setHaber(ingresosCrc);
            creditoVentas.setDescripcion("Venta de mercadería");
            creditoVentas.setAsiento(asiento);
            detalles.add(creditoVentas);
        }

        // Crédito: IVA por Pagar (Impuesto)
        if (impuestoCrc.compareTo(BigDecimal.ZERO) > 0) {
            CuentaContable cuentaIva = resolverCuenta(CLAVE_VENTA_IVA_POR_PAGAR);
            DetalleAsiento creditoIva = new DetalleAsiento();
            creditoIva.setCuenta(cuentaIva);
            creditoIva.setHaber(impuestoCrc);
            creditoIva.setDescripcion("IVA por pagar");
            creditoIva.setAsiento(asiento);
            detalles.add(creditoIva);
        }

        asiento.setDetalles(detalles);
        return asiento;
    }

    /**
     * Determina el tipo de cambio a CRC de una factura (ADR-004).
     * @return 1 para CRC; factura.tipoCambio en otro caso
     * @throws IllegalStateException si la moneda no es CRC y el tipo de cambio es nulo o &lt;= 0
     */
    private BigDecimal resolverTipoCambio(Factura factura) {
        if (factura.getMonedaFE() == null || factura.getMonedaFE() == MonedaFE.CRC) {
            return BigDecimal.ONE;
        }
        BigDecimal tipoCambio = factura.getTipoCambio();
        if (tipoCambio == null || tipoCambio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(String.format(
                "La factura %s está en %s y requiere un tipo de cambio mayor a 0 para contabilizarse en CRC",
                factura.getNumeroFactura(), factura.getMonedaFE().getCodigo()));
        }
        return tipoCambio;
    }

    private static BigDecimal convertirACrc(BigDecimal monto, BigDecimal tipoCambio) {
        return valorONulo(monto).multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal valorONulo(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static String nombreCliente(Cliente cliente) {
        return cliente != null && cliente.getNombre() != null ? " - " + cliente.getNombre() : "";
    }

    private static String truncar(String texto, int max) {
        return texto.length() > max ? texto.substring(0, max) : texto;
    }

    /**
     * Valida que el asiento cuadre antes de guardarlo (ADR-008, regla 6).
     * @throws IllegalStateException si total debe != total haber
     */
    private static void validarCuadre(AsientoContable asiento, String origen) {
        if (!asiento.estaCuadrado()) {
            throw new IllegalStateException(String.format(
                "El asiento de %s no cuadra — debe: %s, haber: %s",
                origen, asiento.getTotalDebe(), asiento.getTotalHaber()));
        }
    }

    /**
     * Resume un asiento como mapa (cuenta|lado → monto) para comparar asientos sin importar el orden.
     */
    private static Map<String, BigDecimal> resumenPorCuenta(AsientoContable asiento) {
        Map<String, BigDecimal> resumen = new HashMap<>();
        for (DetalleAsiento d : asiento.getDetalles()) {
            CuentaContable c = d.getCuenta();
            String cuenta = c == null ? "?" : (c.getIdCuenta() != null ? "id:" + c.getIdCuenta() : "cod:" + c.getCodigo());
            if (d.getDebe() != null && d.getDebe().signum() > 0) {
                resumen.merge(cuenta + "|D", d.getDebe().setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
            }
            if (d.getHaber() != null && d.getHaber().signum() > 0) {
                resumen.merge(cuenta + "|H", d.getHaber().setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
            }
        }
        return resumen;
    }

    // ==================== CPP / SUPPLIER PAYMENT GENERATORS ====================

    /** Clave de la cuenta de compras: contrapartida de la CxP con OC y preseleccionada en la CxP manual. */
    public static final String CLAVE_CPP_CUENTA_COMPRAS = "CPP_CUENTA_COMPRAS";
    /** Clave de la cuenta de proveedores (CR en compra, DR en pago a proveedor). */
    public static final String CLAVE_CPP_CUENTA_POR_PAGAR = "CPP_CUENTA_POR_PAGAR";
    /** Clave de la cuenta de IVA crédito fiscal (DR en compra). */
    public static final String CLAVE_CPP_IVA_CREDITO_FISCAL = "CPP_IVA_CREDITO_FISCAL";
    /** Prefijo de las claves de banco/caja de pagos a proveedor: CPP_BANCO_&lt;MetodoPagoProveedor&gt;. */
    public static final String PREFIJO_CPP_BANCO = "CPP_BANCO_";

    /**
     * Genera y contabiliza el asiento de compra de una CxP (ADR-011: montos en CRC; ADR-015: toda
     * CxP genera su asiento al crearse).
     *
     * CxP con orden de compra — el desglose sale de la OC:
     *   DR CPP_CUENTA_COMPRAS      — oc.subtotal × tc (absorbe el ajuste de redondeo)
     *   DR CPP_IVA_CREDITO_FISCAL  — oc.impuesto × tc (solo si &gt; 0)
     *   CR CPP_CUENTA_POR_PAGAR    — oc.total × tc
     *
     * CxP manual (ADR-016) — desglose y contrapartida de la propia CxP:
     *   DR cpp.cuentaGasto         — cpp.subtotal × tc (absorbe el ajuste de redondeo)
     *   DR CPP_IVA_CREDITO_FISCAL  — cpp.impuesto × tc (solo si &gt; 0)
     *   CR CPP_CUENTA_POR_PAGAR    — cpp.monto × tc
     *
     * tc = cpp.tipoCambio (en la CxP con OC, heredado de la OC); 1 si la CxP está en CRC. Cada línea
     * se convierte con HALF_UP a 2 decimales y la diferencia de redondeo (hasta 0,02) se lleva a la
     * línea de compras/gasto, con el mismo criterio que el asiento de venta.
     *
     * Fecha (ADR-014): cpp.fechaEmision y, si falta, oc.fechaRecepcion; nunca la del sistema.
     *
     * @param cpp la CuentaPorPagar recién guardada
     * @return el asiento contabilizado
     * @throws IllegalStateException si a una CxP manual le falta subtotal, impuesto o cuentaGasto, no hay
     *                               fecha de emisión ni de recepción, falta una clave contable, el tipo de
     *                               cambio o el asiento no cuadra
     */
    @Transactional
    public AsientoContable generarAsientoCompra(CuentaPorPagar cpp) {
        var oc = cpp.getOrdenCompra();
        BigDecimal subtotal;
        BigDecimal impuesto;
        BigDecimal total;
        CuentaContable cuentaDebito;
        String concepto;
        String referencia;
        if (oc != null) {
            subtotal = oc.getSubtotal();
            impuesto = oc.getImpuesto();
            total = oc.getTotal();
            cuentaDebito = null; // CPP_CUENTA_COMPRAS, se resuelve junto con las demás claves
            concepto = "Compra según OC " + oc.getNumero() + " - " + cpp.getProveedor().getNombre();
            referencia = "OC " + oc.getNumero();
        } else {
            if (cpp.getSubtotal() == null || cpp.getImpuesto() == null || cpp.getCuentaGasto() == null) {
                throw new IllegalStateException("La cuenta por pagar manual " + cpp.getNumero()
                    + " requiere subtotal, impuesto y cuenta de gasto para generar su asiento de compra (ADR-016)");
            }
            subtotal = cpp.getSubtotal();
            impuesto = cpp.getImpuesto();
            total = cpp.getMonto();
            cuentaDebito = cpp.getCuentaGasto();
            concepto = "CxP manual " + cpp.getNumero() + " - " + cpp.getProveedor().getNombre()
                + " - " + cpp.getDescripcion();
            referencia = "CxP " + cpp.getNumero();
        }
        LocalDate fecha = fechaAsientoCompra(cpp);

        MonedaFE moneda = ConversionMoneda.oCrc(cpp.getMoneda() != null ? cpp.getMoneda()
            : (oc != null ? oc.getMoneda() : null));
        BigDecimal tipoCambio = tipoCambioDocumento(moneda,
            cpp.getTipoCambio() != null ? cpp.getTipoCambio() : (oc != null ? oc.getTipoCambio() : null),
            "La cuenta por pagar " + cpp.getNumero());

        BigDecimal subtotalCrc = convertirACrc(subtotal, tipoCambio);
        BigDecimal impuestoCrc = convertirACrc(impuesto, tipoCambio);
        BigDecimal totalCrc = convertirACrc(total, tipoCambio);

        // Ajuste del centavo: la diferencia de redondeo se lleva a la línea de compras/gasto
        BigDecimal diferencia = totalCrc.subtract(subtotalCrc).subtract(impuestoCrc);
        if (diferencia.signum() != 0 && diferencia.abs().compareTo(TOLERANCIA_REDONDEO) <= 0) {
            subtotalCrc = subtotalCrc.add(diferencia);
        }

        if (cuentaDebito == null) {
            cuentaDebito = resolverCuenta(CLAVE_CPP_CUENTA_COMPRAS);
        }
        CuentaContable cuentaPorPagar  = resolverCuenta(CLAVE_CPP_CUENTA_POR_PAGAR);

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        // La fecha del asiento es la del documento, coherente con el tipo de cambio con que se convierte
        asiento.setFecha(fecha);
        asiento.setConcepto(truncar(concepto, 500));
        asiento.setTipo(TipoAsiento.AUTOMATICO_COMPRA);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setCuentaPorPagar(cpp);

        List<DetalleAsiento> detalles = new ArrayList<>();

        // DR Compras / contrapartida elegida (subtotal + ajuste de redondeo)
        if (subtotalCrc.signum() > 0) {
            detalles.add(linea(asiento, cuentaDebito, subtotalCrc, null, "Compras — " + referencia));
        }

        // DR IVA Crédito Fiscal (cuando aplica)
        if (impuestoCrc.signum() > 0) {
            CuentaContable cuentaIva = resolverCuenta(CLAVE_CPP_IVA_CREDITO_FISCAL);
            detalles.add(linea(asiento, cuentaIva, impuestoCrc, null, "IVA Crédito Fiscal — " + referencia));
        }

        // CR Cuentas por Pagar (total)
        detalles.add(linea(asiento, cuentaPorPagar, null, totalCrc,
            "Cuentas por Pagar — " + cpp.getProveedor().getNombre()));

        asiento.setDetalles(detalles);

        validarCuadre(asiento, "compra");

        AsientoContable guardado = asientoRepository.save(asiento);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Purchase journal entry {} posted for CPP {}", guardado.getNumero(), cpp.getNumero());
        return guardado;
    }

    /**
     * Cuentas elegibles como contrapartida de una CxP manual (ADR-016), ordenadas por código: activas,
     * que aceptan movimientos, de tipo EGRESO o ACTIVO y que no estén configuradas en
     * parametros_contables como banco/caja, cuentas por cobrar, cuentas por pagar ni IVA crédito
     * fiscal (ver {@link #esClaveExcluidaDeContrapartida(String)}).
     */
    public List<CuentaContable> obtenerCuentasContrapartidaCxpManual() {
        Map<Long, String> excluidas = cuentasExcluidasDeContrapartida();
        return cuentaRepository.findByAceptaMovimientosAndActiva(true, true).stream()
            .filter(c -> c.getTipo() == TipoCuenta.EGRESO || c.getTipo() == TipoCuenta.ACTIVO)
            .filter(c -> !excluidas.containsKey(c.getIdCuenta()))
            .sorted(Comparator.comparing(CuentaContable::getCodigo))
            .toList();
    }

    /** Id de la cuenta configurada en CPP_CUENTA_COMPRAS (preselección de la contrapartida), si existe. */
    public Optional<Long> obtenerIdCuentaComprasCpp() {
        return parametroContableRepository.findByClave(CLAVE_CPP_CUENTA_COMPRAS)
            .map(p -> p.getCuentaContable().getIdCuenta());
    }

    /**
     * Valida la contrapartida de una CxP manual (ADR-016) con las mismas reglas de
     * {@link #obtenerCuentasContrapartidaCxpManual()}.
     * @param cuentaId id de la cuenta elegida
     * @return la cuenta validada
     * @throws IllegalArgumentException si falta, no existe, está inactiva, no acepta movimientos, no es de
     *                                  tipo EGRESO o ACTIVO, o está configurada como banco/caja, CxC, CxP o IVA
     */
    public CuentaContable validarContrapartidaCxpManual(Long cuentaId) {
        if (cuentaId == null) {
            throw new IllegalArgumentException("La cuenta por pagar manual requiere la cuenta de contrapartida (cuentaGastoId)");
        }
        CuentaContable cuenta = cuentaRepository.findById(cuentaId)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta contable no encontrada: " + cuentaId));
        String etiqueta = cuenta.getCodigo() + " - " + cuenta.getNombre();
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new IllegalArgumentException("La cuenta " + etiqueta + " está inactiva");
        }
        if (!Boolean.TRUE.equals(cuenta.getAceptaMovimientos())) {
            throw new IllegalArgumentException("La cuenta " + etiqueta + " no acepta movimientos");
        }
        if (cuenta.getTipo() != TipoCuenta.EGRESO && cuenta.getTipo() != TipoCuenta.ACTIVO) {
            throw new IllegalArgumentException("La cuenta " + etiqueta + " es de tipo "
                + (cuenta.getTipo() != null ? cuenta.getTipo().name() : "sin tipo")
                + "; la contrapartida de una CxP manual debe ser de tipo EGRESO o ACTIVO");
        }
        String clave = cuentasExcluidasDeContrapartida().get(cuenta.getIdCuenta());
        if (clave != null) {
            throw new IllegalArgumentException("La cuenta " + etiqueta + " está configurada como " + clave
                + " y no puede usarse como contrapartida de una CxP manual");
        }
        return cuenta;
    }

    /**
     * Claves de parametros_contables cuyas cuentas no pueden ser contrapartida de una CxP manual:
     * banco/caja de pagos a proveedor (CPP_BANCO_*) y de cobros (COBRO_CAJA_*), cuentas por cobrar
     * (VENTA_CXC_CLIENTES), cuentas por pagar (CPP_CUENTA_POR_PAGAR) e IVA crédito fiscal
     * (CPP_IVA_CREDITO_FISCAL).
     */
    static boolean esClaveExcluidaDeContrapartida(String clave) {
        return clave.startsWith(PREFIJO_CPP_BANCO)
            || clave.startsWith(PREFIJO_COBRO_CAJA)
            || clave.equals(CLAVE_VENTA_CXC_CLIENTES)
            || clave.equals(CLAVE_CPP_CUENTA_POR_PAGAR)
            || clave.equals(CLAVE_CPP_IVA_CREDITO_FISCAL);
    }

    /** Id de cuenta → clave de parametros_contables que la excluye como contrapartida. */
    private Map<Long, String> cuentasExcluidasDeContrapartida() {
        Map<Long, String> excluidas = new HashMap<>();
        for (ParametroContable p : parametroContableRepository.findAllWithCuentas()) {
            if (esClaveExcluidaDeContrapartida(p.getClave())) {
                excluidas.putIfAbsent(p.getCuentaContable().getIdCuenta(), p.getClave());
            }
        }
        return excluidas;
    }

    /**
     * Fecha del asiento de compra (ADR-014): la de emisión de la CxP; si falta, la de recepción de
     * la OC. Nunca la fecha del sistema.
     * @throws IllegalStateException si no hay ninguna de las dos
     */
    private LocalDate fechaAsientoCompra(CuentaPorPagar cpp) {
        if (cpp.getFechaEmision() != null) {
            return cpp.getFechaEmision();
        }
        LocalDate fechaRecepcion = cpp.getOrdenCompra() != null ? cpp.getOrdenCompra().getFechaRecepcion() : null;
        return fechaEconomica(fechaRecepcion, "La cuenta por pagar " + cpp.getNumero(),
            "fechaEmision ni fechaRecepcion de la orden de compra");
    }

    /**
     * Genera y contabiliza el asiento de un pago a proveedor (ADR-011: montos en CRC).
     *
     * Estructura:
     *   DR CPP_CUENTA_POR_PAGAR          — montoAplicado × cpp.tipoCambio
     *   CR CPP_BANCO_&lt;método&gt;           — monto del pago en CRC al tipo de cambio del pago
     *   DR DIFERENCIAL_CAMBIARIO_GASTO   — si se pagaron más colones de los registrados en la CxP
     *   CR DIFERENCIAL_CAMBIARIO_INGRESO — si se pagaron menos colones
     *
     * Pagos anteriores a ADR-011 (sin moneda/tipoCambio/montoAplicado) se tratan como pagos en la
     * moneda de la CxP a su tipo de cambio: sin diferencial.
     *
     * Fecha (ADR-014): la del pago (pagoProveedor.fecha), nunca la del sistema.
     *
     * @param pago el PagoProveedor recién guardado
     * @return el asiento contabilizado
     * @throws IllegalStateException si el pago no tiene fecha, falta una clave contable o el asiento no cuadra
     */
    @Transactional
    public AsientoContable generarAsientoPagoProveedor(PagoProveedor pago) {
        LocalDate fecha = fechaEconomica(pago.getFecha(), "El pago a proveedor " + pago.getNumero(), "fecha");

        String bancoKey = switch (pago.getMetodoPago()) {
            case TRANSFERENCIA -> "CPP_BANCO_TRANSFERENCIA";
            case CHEQUE        -> "CPP_BANCO_CHEQUE";
            case EFECTIVO      -> "CPP_BANCO_EFECTIVO";
            case SINPE         -> "CPP_BANCO_SINPE";
            case DEPOSITO      -> "CPP_BANCO_DEPOSITO";
        };

        CuentaContable cuentaPorPagar = resolverCuenta(CLAVE_CPP_CUENTA_POR_PAGAR);
        CuentaContable cuentaBanco    = resolverCuenta(bancoKey);

        CuentaPorPagar cpp = pago.getCuentaPorPagar();
        MonedaFE monedaCuenta = ConversionMoneda.oCrc(cpp != null ? cpp.getMoneda() : null);
        BigDecimal tipoCambioCuenta = tipoCambioDocumento(monedaCuenta, cpp != null ? cpp.getTipoCambio() : null,
            "La cuenta por pagar " + (cpp != null ? cpp.getNumero() : ""));
        MonedaFE monedaPago = pago.getMoneda() != null ? pago.getMoneda() : monedaCuenta;

        MonedaFE extranjera;
        try {
            extranjera = ConversionMoneda.monedaExtranjera(monedaPago, monedaCuenta);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        BigDecimal tipoCambioPago = extranjera == MonedaFE.CRC ? BigDecimal.ONE
            : (pago.getTipoCambio() != null ? pago.getTipoCambio() : tipoCambioCuenta);
        if (tipoCambioPago == null || tipoCambioPago.signum() <= 0) {
            throw new IllegalStateException("El pago " + pago.getNumero()
                + " requiere un tipo de cambio mayor a 0 para contabilizarse en CRC");
        }

        BigDecimal montoAplicado = pago.getMontoAplicado() != null
            ? pago.getMontoAplicado()
            : ConversionMoneda.montoAplicado(pago.getMonto(), monedaPago, monedaCuenta, tipoCambioPago);
        BigDecimal cxpCrc = convertirACrc(montoAplicado, tipoCambioCuenta);
        BigDecimal bancoCrc = ConversionMoneda.montoEnCrc(pago.getMonto(), monedaPago, tipoCambioPago);

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(fecha);
        asiento.setConcepto("Pago proveedor " + pago.getNumero() + " — " + pago.getMetodoPago());
        asiento.setTipo(TipoAsiento.AUTOMATICO_GASTO);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setPagoProveedor(pago);

        List<DetalleAsiento> detalles = new ArrayList<>();

        // DR Cuentas por Pagar (lo que se rebaja de la CxP, a su tipo de cambio)
        detalles.add(linea(asiento, cuentaPorPagar, cxpCrc, null, "Pago a proveedor — " + pago.getNumero()));

        // CR Banco/Caja (lo que salió en CRC, al tipo de cambio del pago)
        detalles.add(linea(asiento, cuentaBanco, null, bancoCrc, pago.getMetodoPago() + " — " + pago.getNumero()));

        // Diferencial cambiario: ganancia si se pagaron menos colones de los registrados
        agregarDiferencialCambiario(asiento, detalles, cxpCrc.subtract(bancoCrc), "Pago proveedor " + pago.getNumero());

        asiento.setDetalles(detalles);

        validarCuadre(asiento, "pago a proveedor");

        AsientoContable guardado = asientoRepository.save(asiento);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Supplier payment journal entry {} posted for pago {}", guardado.getNumero(), pago.getNumero());
        return guardado;
    }

    /**
     * Revierte los asientos vigentes de un pago a proveedor (ADR-008 regla 4, ADR-011).
     * @param pago   Pago a proveedor origen
     * @param motivo Motivo de la reversión
     * @return Asientos de reversión generados (vacío si el pago no estaba contabilizado)
     */
    @Transactional
    public List<AsientoContable> revertirAsientoPagoProveedor(PagoProveedor pago, String motivo) {
        List<AsientoContable> reversiones = new ArrayList<>();
        if (pago.getId() == null) {
            return reversiones;
        }
        for (AsientoContable vigente : asientoRepository.findAsientosPagoProveedorVigentes(pago.getId())) {
            reversiones.add(generarAsientoReversion(vigente, motivo));
        }
        return reversiones;
    }

    /**
     * Tipo de cambio de un documento (CxP/OC) a CRC: 1 si está en CRC; el indicado en otro caso.
     * @throws IllegalStateException si la moneda es extranjera y el tipo de cambio es nulo o &lt;= 0
     */
    private static BigDecimal tipoCambioDocumento(MonedaFE moneda, BigDecimal tipoCambio, String documento) {
        if (moneda == MonedaFE.CRC) {
            return BigDecimal.ONE;
        }
        if (tipoCambio == null || tipoCambio.signum() <= 0) {
            throw new IllegalStateException(String.format(
                "%s está en %s y requiere un tipo de cambio mayor a 0 para contabilizarse en CRC",
                documento, moneda.getCodigo()));
        }
        return tipoCambio;
    }

    /**
     * Generates a balanced payroll journal entry for a Nomina in APROBADA state.
     *
     * Entry structure (lines with amount 0 are omitted):
     *   DR NOMINA_GASTO_SUELDOS                  — nomina.totalBruto
     *   DR NOMINA_GASTO_SUELDOS                  — nomina.totalCcssPatronal (employer CCSS cost)
     *   DR NOMINA_APORTE_PATRONAL_ASE_GASTO      — sum(detalles.aportePatronalSolidarista) (ADR-013)
     *   CR NOMINA_CCSS_POR_PAGAR                 — sum(detalles.ccssObrero)
     *   CR NOMINA_INS_POR_PAGAR                  — sum(detalles.ins)
     *   CR NOMINA_RENTA_POR_PAGAR                — sum(detalles.impuestoRenta)
     *   CR NOMINA_SOLIDARISTA_POR_PAGAR          — sum(detalles.solidarista) (ADR-013)
     *   CR NOMINA_PENSION_ALIMENTARIA_POR_PAGAR  — sum(detalles.pensionAlimentaria) (ADR-013)
     *   CR NOMINA_SALARIOS_POR_PAGAR             — nomina.totalNeto
     *   CR NOMINA_CCSS_PATRONAL_POR_PAGAR        — nomina.totalCcssPatronal
     *   CR NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR  — sum(detalles.aportePatronalSolidarista) (ADR-013)
     *
     * Balance: totalBruto = ccssObrero + ins + renta + solidarista + pension + totalNeto; the
     * employer CCSS and ASE contributions are posted as DR expense / CR liability pairs.
     *
     * The six base keys are always required. The solidarista, pensión and ASE keys are resolved
     * only when the corresponding total is greater than zero.
     *
     * Date (ADR-014): the entry is dated with nomina.periodoFin (accrual date, the same date used to
     * resolve the CCSS, income tax and ASE rates), never with the system date. nomina.fechaPago is
     * reserved for the future payroll payment entry.
     *
     * @param nomina the approved Nomina to post
     * @return the posted AsientoContable
     * @throws IllegalStateException if the nomina has no periodoFin, a required NOMINA_* key is not
     *         configured, or the entry does not balance (the message includes Debe, Haber and the difference)
     */
    @Transactional
    public AsientoContable generarAsientoNomina(Nomina nomina) {
        LocalDate fecha = fechaEconomica(nomina.getPeriodoFin(), "La nómina " + nomina.getNumero(), "periodoFin");

        // Resolve the 6 base accounts — fail fast if any missing
        CuentaContable cuentaGastoSueldos  = resolverCuenta("NOMINA_GASTO_SUELDOS");
        CuentaContable cuentaCcssPatronal  = resolverCuenta("NOMINA_CCSS_PATRONAL_POR_PAGAR");
        CuentaContable cuentaCcssObrero    = resolverCuenta("NOMINA_CCSS_POR_PAGAR");
        CuentaContable cuentaIns           = resolverCuenta("NOMINA_INS_POR_PAGAR");
        CuentaContable cuentaRenta         = resolverCuenta("NOMINA_RENTA_POR_PAGAR");
        CuentaContable cuentaSalarios      = resolverCuenta("NOMINA_SALARIOS_POR_PAGAR");

        List<DetalleNomina> lineasNomina = nomina.getDetalles() != null ? nomina.getDetalles() : List.of();
        BigDecimal totalCcssObrero  = sumarDetallesNomina(lineasNomina, DetalleNomina::getCcssObrero);
        BigDecimal totalIns         = sumarDetallesNomina(lineasNomina, DetalleNomina::getIns);
        BigDecimal totalRenta       = sumarDetallesNomina(lineasNomina, DetalleNomina::getImpuestoRenta);
        BigDecimal totalSolidarista = sumarDetallesNomina(lineasNomina, DetalleNomina::getSolidarista);
        BigDecimal totalPension     = sumarDetallesNomina(lineasNomina, DetalleNomina::getPensionAlimentaria);
        BigDecimal totalAporteAse   = sumarDetallesNomina(lineasNomina, DetalleNomina::getAportePatronalSolidarista);

        // Optional accounts (ADR-013) — resolved only when they will be used
        CuentaContable cuentaSolidarista = totalSolidarista.signum() > 0
                ? resolverCuenta("NOMINA_SOLIDARISTA_POR_PAGAR") : null;
        CuentaContable cuentaPension = totalPension.signum() > 0
                ? resolverCuenta("NOMINA_PENSION_ALIMENTARIA_POR_PAGAR") : null;
        CuentaContable cuentaAseGasto = totalAporteAse.signum() > 0
                ? resolverCuenta("NOMINA_APORTE_PATRONAL_ASE_GASTO") : null;
        CuentaContable cuentaAsePorPagar = totalAporteAse.signum() > 0
                ? resolverCuenta("NOMINA_APORTE_PATRONAL_ASE_POR_PAGAR") : null;

        AsientoContable asiento = new AsientoContable();
        asiento.setNumero(generarNumeroAsiento());
        asiento.setFecha(fecha);
        asiento.setConcepto("Nómina " + nomina.getNumero() + " — período "
                + nomina.getPeriodoInicio() + " / " + nomina.getPeriodoFin());
        asiento.setTipo(TipoAsiento.AUTOMATICO_NOMINA);
        asiento.setEstado(EstadoAsiento.BORRADOR);
        asiento.setNomina(nomina);

        List<DetalleAsiento> detalles = new ArrayList<>();
        String numero = nomina.getNumero();

        // Debits
        agregarLineaNomina(detalles, asiento, cuentaGastoSueldos, nomina.getTotalBruto(), true,
                "Sueldos brutos — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaGastoSueldos, nomina.getTotalCcssPatronal(), true,
                "Carga patronal CCSS — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaAseGasto, totalAporteAse, true,
                "Aporte patronal asociación solidarista — " + numero);

        // Credits
        agregarLineaNomina(detalles, asiento, cuentaCcssObrero, totalCcssObrero, false,
                "CCSS obrero por pagar — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaIns, totalIns, false,
                "INS por pagar — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaRenta, totalRenta, false,
                "Impuesto renta por pagar — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaSolidarista, totalSolidarista, false,
                "Ahorro obrero solidarista por pagar — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaPension, totalPension, false,
                "Pensiones alimentarias por pagar — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaSalarios, nomina.getTotalNeto(), false,
                "Salarios netos por pagar — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaCcssPatronal, nomina.getTotalCcssPatronal(), false,
                "CCSS patronal por pagar — " + numero);
        agregarLineaNomina(detalles, asiento, cuentaAsePorPagar, totalAporteAse, false,
                "Aporte patronal asociación solidarista por pagar — " + numero);

        asiento.setDetalles(detalles);

        if (!asiento.estaCuadrado()) {
            BigDecimal debe = asiento.getTotalDebe();
            BigDecimal haber = asiento.getTotalHaber();
            throw new IllegalStateException(String.format(
                    "Asiento de nómina %s no cuadra — Debe: %s, Haber: %s, diferencia: %s",
                    numero, debe, haber, debe.subtract(haber)));
        }

        AsientoContable guardado = asientoRepository.save(asiento);
        guardado.contabilizar();
        guardado = asientoRepository.save(guardado);

        log.info("Asiento de nómina {} generado y contabilizado: {}",
                nomina.getNumero(), guardado.getNumero());
        return guardado;
    }

    /** Sums one amount across the payroll lines, treating null as zero. */
    private static BigDecimal sumarDetallesNomina(List<DetalleNomina> lineas,
                                                  Function<DetalleNomina, BigDecimal> campo) {
        return lineas.stream()
                .map(campo)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Adds a debit or credit line to the payroll entry; amounts null or &lt;= 0 are skipped. */
    private static void agregarLineaNomina(List<DetalleAsiento> detalles, AsientoContable asiento,
                                           CuentaContable cuenta, BigDecimal monto, boolean debe,
                                           String descripcion) {
        if (monto == null || monto.signum() <= 0) {
            return;
        }
        DetalleAsiento linea = new DetalleAsiento();
        linea.setCuenta(cuenta);
        if (debe) {
            linea.setDebe(monto);
        } else {
            linea.setHaber(monto);
        }
        linea.setDescripcion(descripcion);
        linea.setAsiento(asiento);
        detalles.add(linea);
    }

    /**
     * Economic date of the source document for an automatic entry (ADR-014). There is no fallback
     * to the system date: a document without its date cannot be posted.
     *
     * @param fecha     the document's economic date
     * @param documento description of the document for the message (e.g. "La factura FAC-0001")
     * @param campo     name of the missing field
     * @return the date itself
     * @throws IllegalStateException if the date is null
     */
    private static LocalDate fechaEconomica(LocalDate fecha, String documento, String campo) {
        if (fecha == null) {
            throw new IllegalStateException(String.format(
                "%s no tiene %s: el asiento automático se fecha con la fecha económica del documento (ADR-014)",
                documento, campo));
        }
        return fecha;
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
