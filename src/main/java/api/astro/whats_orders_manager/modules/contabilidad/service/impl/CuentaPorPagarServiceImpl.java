package api.astro.whats_orders_manager.modules.contabilidad.service.impl;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.AceptacionComprobanteDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarVistaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagarLinea;
import api.astro.whats_orders_manager.modules.contabilidad.model.ProrrataIva;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ProrrataIvaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorDuplicadoException;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorValidator;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.contabilidad.service.IvaCompraCalculo;
import api.astro.whats_orders_manager.modules.contabilidad.service.ProrrataNoConfiguradaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.HoraFiscal;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.facturacion.util.ConversionMoneda;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.PagoProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaPorPagarServiceImpl implements CuentaPorPagarService {

    private final CuentaPorPagarRepository cuentaRepository;
    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final AsientoContableService asientoService;
    private final TipoCambioService tipoCambioService;
    private final ComprobanteProveedorValidator comprobanteValidator;
    private final ProrrataIvaRepository prorrataRepository;
    private final PagoProveedorRepository pagoProveedorRepository;
    private final UsuarioActividadService actividadService;
    private final Clock clock;

    private static final String NOMBRE_INDICE_COMPROBANTE = "uk_cpp_proveedor_comprobante";

    /**
     * Crea una CxP y genera su asiento de compra en la misma transacción (ADR-015).
     *
     * Con ordenCompraId: mismas reglas que {@link #crearDesdeOrdenCompra(Long)}. El desglose, la moneda y
     * el tipo de cambio salen de la OC (se ignoran subtotal, impuesto y tipoCambio); se
     * rechaza si la OC no está recibida, si ya tiene CxP, si el monto indicado difiere de oc.total o si
     * la moneda indicada difiere de la de la OC.
     * Sin ordenCompraId (CxP manual, ADR-016, T-054): exige el comprobante del proveedor y al menos una línea; cada línea
     * lleva su cuenta (hoja válida, {@link AsientoContableService#validarContrapartidaCxpManual(Long)}), tarifa, base, IVA y
     * tratamiento. El subtotal, el impuesto y el monto se calculan a partir de las líneas (lo que envíe el cliente se
     * ignora) y el IVA acreditable y no acreditable de cada línea se calcula y se guarda. Una línea de uso común exige el
     * porcentaje de prorrata del año del comprobante ({@link ProrrataNoConfiguradaException}, 409).
     *
     * Si la validación o el asiento fallan no queda guardada la CxP ni el cambio de saldo del proveedor.
     */
    @Override
    @Transactional
    public CuentaPorPagar crear(CuentaPorPagarDTO dto) {
        if (dto.getProveedorId() == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio");
        }
        // Bloqueo por proveedor: serializa altas y anulaciones del mismo proveedor, de modo que el chequeo de comprobante
        // duplicado y el recálculo del saldo ven siempre lo confirmado (el índice único queda como respaldo)
        Proveedor proveedor = proveedorRepository.findByIdForUpdate(dto.getProveedorId())
            .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado: " + dto.getProveedorId()));

        OrdenCompra oc = null;
        MonedaOc monedaOc = null;
        BigDecimal monto;
        BigDecimal subtotal = null;
        BigDecimal impuesto = null;
        List<CuentaPorPagarLinea> lineas = new ArrayList<>();
        String numeroComprobante = null;
        String comprobanteNormalizado = null;
        String descripcion = dto.getDescripcion();
        if (dto.getOrdenCompraId() != null) {
            oc = ordenCompraRepository.findById(dto.getOrdenCompraId())
                .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + dto.getOrdenCompraId()));
            validarOcParaCxp(oc);
            monedaOc = monedaYTipoCambioDeOc(oc);
            if (dto.getMoneda() != null && dto.getMoneda() != monedaOc.moneda()) {
                throw new IllegalArgumentException(String.format(
                    "La moneda indicada (%s) no coincide con la moneda de la OC %s (%s)",
                    dto.getMoneda().getCodigo(), oc.getNumero(), monedaOc.moneda().getCodigo()));
            }
            if (dto.getMonto() != null && dto.getMonto().compareTo(oc.getTotal()) != 0) {
                throw new IllegalArgumentException(String.format(
                    "El monto %s no coincide con el total de la OC %s (%s)", dto.getMonto(), oc.getNumero(), oc.getTotal()));
            }
            monto = oc.getTotal();
            if (descripcion == null || descripcion.isBlank()) {
                descripcion = "Compra según OC " + oc.getNumero();
            }
        } else {
            // T-052/T-054: comprobante del proveedor y líneas con cuenta y tratamiento de IVA (ADR-066, ADR-067)
            comprobanteValidator.validar(dto);
            if (descripcion == null || descripcion.isBlank()) {
                throw new IllegalArgumentException("La descripción es obligatoria");
            }
            // la cuenta de cada línea: hoja activa que acepta movimientos (se valida una vez por cuenta)
            Map<Long, CuentaContable> cuentas = new LinkedHashMap<>();
            for (CuentaPorPagarLineaDTO l : dto.getLineas()) {
                cuentas.computeIfAbsent(l.getCuentaContableId(), asientoService::validarContrapartidaCxpManual);
            }
            BigDecimal prorrata = null;
            if (dto.getLineas().stream().anyMatch(l -> l.getTratamiento() == TratamientoIvaCompra.USO_COMUN)) {
                int anio = dto.getFechaComprobante().getYear();
                prorrata = prorrataRepository.findByAnio(anio).map(ProrrataIva::getPorcentaje)
                    .orElseThrow(() -> new ProrrataNoConfiguradaException(anio));
            }
            subtotal = BigDecimal.ZERO;
            impuesto = BigDecimal.ZERO;
            int numeroLinea = 1;
            for (CuentaPorPagarLineaDTO l : dto.getLineas()) {
                BigDecimal base = l.getBase().setScale(2, RoundingMode.HALF_UP);
                BigDecimal iva = l.getIva().setScale(2, RoundingMode.HALF_UP);
                boolean usoComun = l.getTratamiento() == TratamientoIvaCompra.USO_COMUN;
                boolean limitado = l.getTratamiento() == TratamientoIvaCompra.LIMITADO;
                BigDecimal tarifaLimite = limitado ? l.getTarifaLimite().setScale(2, RoundingMode.HALF_UP) : null;
                IvaCompraCalculo.Resultado r = IvaCompraCalculo.calcular(base, iva, l.getTratamiento(), tarifaLimite,
                    usoComun ? prorrata : null);
                String descripcionLinea = l.getDescripcion() != null && !l.getDescripcion().isBlank()
                    ? l.getDescripcion().trim() : descripcion.trim();
                lineas.add(CuentaPorPagarLinea.builder().numeroLinea(numeroLinea++)
                    .descripcion(descripcionLinea.length() > 300 ? descripcionLinea.substring(0, 300) : descripcionLinea)
                    .cuentaContable(cuentas.get(l.getCuentaContableId())).codigoTarifa(l.getCodigoTarifa())
                    .base(base).iva(iva).tratamiento(l.getTratamiento()).tarifaLimite(tarifaLimite)
                    .porcentajeProrrata(usoComun ? prorrata : null)
                    .ivaAcreditable(r.acreditable()).ivaNoAcreditable(r.noAcreditable()).build());
                subtotal = subtotal.add(base);
                impuesto = impuesto.add(iva);
            }
            monto = subtotal.add(impuesto);
            numeroComprobante = dto.getNumeroComprobanteProveedor().trim();
            comprobanteNormalizado = normalizarComprobante(numeroComprobante);
            if (cuentaRepository.existsByProveedorAndComprobanteNormalizado(proveedor, comprobanteNormalizado)) {
                throw new ComprobanteProveedorDuplicadoException(numeroComprobante);
            }
        }

        // ADR-011: tipo de cambio propio; con OC, moneda y tipo de cambio salen de la orden (ADR-015)
        // ADR-014: en la CxP manual la fecha de emisión (y la del asiento) es la del comprobante del proveedor
        LocalDate fechaEmision = dto.getOrdenCompraId() == null ? dto.getFechaComprobante()
            : (dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDate.now());
        MonedaFE moneda;
        BigDecimal tipoCambio;
        if (monedaOc != null) {
            moneda = monedaOc.moneda();
            tipoCambio = monedaOc.tipoCambio();
        } else {
            moneda = dto.getMoneda() != null ? dto.getMoneda() : MonedaFE.CRC;
            tipoCambio = tipoCambioService.resolverTipoCambioOperacion(
                moneda, moneda, dto.getTipoCambio(), fechaEmision);
        }

        CuentaPorPagar cuenta = CuentaPorPagar.builder()
            .numero(generarNumero())
            .proveedor(proveedor)
            .ordenCompra(oc)
            .descripcion(descripcion)
            .monto(monto)
            .saldoPendiente(monto)
            .subtotal(subtotal)
            .impuesto(impuesto)
            .moneda(moneda)
            .tipoCambio(tipoCambio)
            .fechaEmision(fechaEmision)
            .fechaVencimiento(dto.getFechaVencimiento())
            .notas(dto.getNotas())
            .numeroComprobanteProveedor(numeroComprobante)
            .claveComprobante(dto.getClaveComprobante() != null && !dto.getClaveComprobante().isBlank()
                ? dto.getClaveComprobante().trim() : null)
            .fechaComprobante(oc == null ? dto.getFechaComprobante() : null)
            .comprobanteNormalizado(comprobanteNormalizado)
            .build();
        for (CuentaPorPagarLinea linea : lineas) {
            linea.setCuentaPorPagar(cuenta);
            cuenta.getLineas().add(linea);
        }

        CuentaPorPagar guardada = guardarComprobando(cuenta, numeroComprobante);
        // ADR-015: el asiento va en la misma transacción; si falla, se revierte también la CxP.
        // El saldo del proveedor se recalcula después, para no dejarlo modificado en memoria si el asiento falla.
        asientoService.generarAsientoCompra(guardada);
        recalcularSaldoProveedor(proveedor);
        log.info("Cuenta por pagar {} creada para proveedor {}", guardada.getNumero(), proveedor.getNombre());
        return guardada;
    }

    @Override
    @Transactional
    public CuentaPorPagar crearDesdeOrdenCompra(Long ordenCompraId) {
        OrdenCompra oc = ordenCompraRepository.findById(ordenCompraId)
            .orElseThrow(() -> new NoSuchElementException("Orden de compra no encontrada: " + ordenCompraId));

        validarOcParaCxp(oc);
        MonedaOc monedaOc = monedaYTipoCambioDeOc(oc);

        CuentaPorPagar cuenta = CuentaPorPagar.builder()
            .numero(generarNumero())
            .proveedor(oc.getProveedor())
            .ordenCompra(oc)
            .descripcion("Compra según OC " + oc.getNumero())
            .monto(oc.getTotal())
            .saldoPendiente(oc.getTotal())
            .moneda(monedaOc.moneda())
            .tipoCambio(monedaOc.tipoCambio())
            .fechaEmision(LocalDate.now())
            .fechaVencimiento(oc.getProveedor().getDiasCredito() != null && oc.getProveedor().getDiasCredito() > 0
                ? LocalDate.now().plusDays(oc.getProveedor().getDiasCredito())
                : null)
            .build();

        CuentaPorPagar guardada = cuentaRepository.save(cuenta);
        asientoService.generarAsientoCompra(guardada);
        recalcularSaldoProveedor(oc.getProveedor());
        log.info("CPP {} generada desde OC {}", guardada.getNumero(), oc.getNumero());
        return guardada;
    }

    /**
     * Reglas comunes para crear una CxP desde una OC, por cualquiera de las dos vías (ADR-015):
     * la OC debe estar recibida (total o parcialmente) y no tener ya una CxP.
     */
    private void validarOcParaCxp(OrdenCompra oc) {
        if (oc.getEstado() != EstadoOrdenCompra.RECIBIDA && oc.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
            throw new IllegalStateException("Solo se puede generar CPP de una OC recibida");
        }
        if (cuentaRepository.existsByOrdenCompra(oc)) {
            throw new IllegalStateException("Ya existe una cuenta por pagar para la OC " + oc.getNumero());
        }
    }

    /** Moneda y tipo de cambio que hereda de la OC una CxP creada desde ella (ADR-011). */
    private MonedaOc monedaYTipoCambioDeOc(OrdenCompra oc) {
        MonedaFE moneda = oc.getMoneda() != null ? oc.getMoneda() : MonedaFE.CRC;
        BigDecimal tipoCambio = tipoCambioService.resolverTipoCambioOperacion(
            moneda, moneda, oc.getTipoCambio(), oc.getFecha());
        return new MonedaOc(moneda, tipoCambio);
    }

    private record MonedaOc(MonedaFE moneda, BigDecimal tipoCambio) {}

    @Override
    @Transactional(readOnly = true)
    public CuentaPorPagar findById(Long id) {
        return cuentaRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Cuenta por pagar no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagar> findAll() {
        return cuentaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagar> findByEstado(EstadoCuentaPorPagar estado) {
        return cuentaRepository.findByEstadoOrderByFechaVencimientoAsc(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagar> findVencidas() {
        return cuentaRepository.findVencidas(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findOrdenCompraIdsConCpp() {
        return new HashSet<>(cuentaRepository.findOrdenCompraIds());
    }

    @Override
    @Transactional
    public int actualizarVencidas() {
        List<CuentaPorPagar> vencidas = cuentaRepository.findVencidas(LocalDate.now());
        vencidas.forEach(c -> c.setEstado(EstadoCuentaPorPagar.VENCIDA));
        cuentaRepository.saveAll(vencidas);
        if (!vencidas.isEmpty()) {
            log.info("Marcadas {} cuentas por pagar como VENCIDA", vencidas.size());
        }
        return vencidas.size();
    }

    @Override
    @Transactional
    public void aplicarPago(CuentaPorPagar cuenta, BigDecimal monto) {
        cuenta.setSaldoPendiente(cuenta.getSaldoPendiente().subtract(monto));
        cuenta.setEstado(cuenta.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0
            ? EstadoCuentaPorPagar.PAGADA
            : EstadoCuentaPorPagar.PARCIAL);
        cuentaRepository.save(cuenta);
        recalcularSaldoProveedor(cuenta.getProveedor());
    }

    @Override
    @Transactional
    public void revertirPago(CuentaPorPagar cuenta, BigDecimal monto) {
        cuenta.setSaldoPendiente(cuenta.getSaldoPendiente().add(monto));
        cuenta.setEstado(cuenta.getSaldoPendiente().compareTo(cuenta.getMonto()) == 0
            ? EstadoCuentaPorPagar.PENDIENTE
            : EstadoCuentaPorPagar.PARCIAL);
        cuentaRepository.save(cuenta);
        recalcularSaldoProveedor(cuenta.getProveedor());
    }

    @Override
    @Transactional
    public CuentaPorPagar bloquear(Long id) {
        Long proveedorId = cuentaRepository.findProveedorId(id)
            .orElseThrow(() -> new NoSuchElementException("Cuenta por pagar no encontrada: " + id));
        proveedorRepository.findByIdForUpdate(proveedorId);
        return cuentaRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new NoSuchElementException("Cuenta por pagar no encontrada: " + id));
    }

    @Override
    @Transactional
    public CuentaPorPagar anular(Long id, String motivo, Usuario usuario) {
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.length() < 3 || motivoLimpio.length() > 180) {
            throw new IllegalArgumentException(MSG_ANULAR_MOTIVO);
        }
        // Bloqueo proveedor → CxP; estado y pagos se revisan DESPUÉS, sobre el estado vigente
        CuentaPorPagar cuenta = bloquear(id);
        if (cuenta.getEstado() == EstadoCuentaPorPagar.ANULADA) {
            throw new IllegalStateException(MSG_ANULAR_YA_ANULADA);
        }
        if (!pagoProveedorRepository.findByCuentaPorPagarAndEstadoForUpdate(cuenta, EstadoPagoProveedor.CONFIRMADO).isEmpty()) {
            throw new IllegalStateException(MSG_ANULAR_CON_PAGOS);
        }

        // ADR-023: el asiento de compra no se edita ni se borra; se revierte desde su documento origen
        var reversiones = asientoService.revertirAsientoCompra(cuenta,
            "Anulación de cuenta por pagar " + cuenta.getNumero() + ": " + motivoLimpio);

        cuenta.setEstado(EstadoCuentaPorPagar.ANULADA);
        cuenta.setAnuladaPorId(usuario != null ? usuario.getIdUsuario() : null);
        cuenta.setFechaAnulacion(HoraFiscal.ahora(clock));
        cuenta.setMotivoAnulacion(motivoLimpio);
        cuenta.setComprobanteNormalizado(null); // una CxP anulada no bloquea volver a registrar el comprobante
        CuentaPorPagar anulada = cuentaRepository.save(cuenta);
        recalcularSaldoProveedor(cuenta.getProveedor());

        String descripcion = "Cuenta por pagar " + cuenta.getNumero() + " anulada: " + motivoLimpio
            + (reversiones.isEmpty() ? " (sin asiento de compra que revertir)" : "");
        if (usuario != null && usuario.getIdUsuario() != null) {
            actividadService.registrarActividad(usuario.getIdUsuario(), "CXP_ANULADA", descripcion,
                "CuentaPorPagar", id.intValue());
        }
        log.info(descripcion);
        return anulada;
    }

    @Override
    @Transactional
    public CuentaPorPagar registrarAceptacion(Long id, AceptacionComprobanteDTO dto, Usuario usuario) {
        if (dto == null || dto.getEstado() == null || dto.getEstado() == EstadoAceptacionComprobante.PENDIENTE) {
            throw new IllegalArgumentException(MSG_ACEPTACION_ESTADO);
        }
        if (dto.getFecha() == null) {
            throw new IllegalArgumentException(MSG_ACEPTACION_FECHA);
        }
        String numero = dto.getNumero() == null ? "" : dto.getNumero().trim();
        if (numero.isEmpty()) {
            throw new IllegalArgumentException(MSG_ACEPTACION_NUMERO);
        }
        if (numero.length() > 50) {
            throw new IllegalArgumentException(MSG_ACEPTACION_NUMERO_LARGO);
        }
        if (dto.getFecha().isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException(MSG_ACEPTACION_FECHA_FUTURA);
        }
        // mismo orden de bloqueo que la anulación y los pagos: proveedor y luego la CxP
        CuentaPorPagar cuenta = bloquear(id);
        if (cuenta.getEstado() == EstadoCuentaPorPagar.ANULADA) {
            throw new IllegalStateException(MSG_ACEPTACION_ANULADA);
        }
        LocalDate fechaComprobante = cuenta.getFechaComprobante() != null ? cuenta.getFechaComprobante() : cuenta.getFechaEmision();
        if (fechaComprobante != null && dto.getFecha().isBefore(fechaComprobante)) {
            throw new IllegalArgumentException(MSG_ACEPTACION_FECHA_ANTERIOR);
        }
        EstadoAceptacionComprobante anterior = cuenta.getEstadoAceptacion();
        cuenta.setEstadoAceptacion(dto.getEstado());
        cuenta.setFechaAceptacion(dto.getFecha());
        cuenta.setNumeroMensajeReceptor(numero);
        CuentaPorPagar guardada = cuentaRepository.save(cuenta);

        String descripcion = "Aceptación del comprobante de la cuenta por pagar " + cuenta.getNumero() + ": "
            + anterior + " -> " + dto.getEstado() + " (fecha " + dto.getFecha() + ", mensaje receptor " + numero + ")";
        if (usuario != null && usuario.getIdUsuario() != null) {
            actividadService.registrarActividad(usuario.getIdUsuario(), "CXP_ACEPTACION_COMPROBANTE", descripcion,
                "CuentaPorPagar", id.intValue());
        }
        log.info(descripcion);
        return guardada;
    }

    /**
     * Saldo del proveedor en CRC = Σ (saldo de cada CxP no anulada × su tipo de cambio), cada una redondeada a 2
     * decimales (HALF_UP). Único punto de escritura; los asientos y los pagos no lo tocan.
     */
    @Override
    @Transactional
    public void recalcularSaldoProveedor(Proveedor proveedor) {
        BigDecimal total = cuentaRepository.findByProveedorAndEstadoNot(proveedor, EstadoCuentaPorPagar.ANULADA).stream()
            .map(c -> ConversionMoneda.montoEnCrcReporte(c.getSaldoPendiente(), c.getMoneda(), c.getTipoCambio()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        proveedor.setSaldoPendiente(total);
        proveedorRepository.save(proveedor);
    }

    @Override
    public List<String> advertencias(CuentaPorPagarDTO dto) {
        return comprobanteValidator.advertencias(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPorPagarVistaDTO detalleVista(Long id) {
        return CuentaPorPagarVistaDTO.de(findById(id), LocalDate.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagarVistaDTO> listarVista(EstadoCuentaPorPagar estado, EstadoAceptacionComprobante aceptacion) {
        var cuentas = estado != null ? cuentaRepository.findByEstadoConRelaciones(estado)
            : cuentaRepository.findAllConRelaciones();
        LocalDate hoy = LocalDate.now(clock);
        return cuentas.stream()
            .filter(c -> aceptacion == null || aceptacion == c.getEstadoAceptacion())
            .map(c -> CuentaPorPagarVistaDTO.de(c, hoy)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagarVistaDTO> vencidasVista() {
        LocalDate hoy = LocalDate.now(clock);
        return cuentaRepository.findVencidasConRelaciones(hoy).stream()
            .map(c -> CuentaPorPagarVistaDTO.de(c, hoy)).toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Número del comprobante sin espacios y en mayúsculas: la clave de comparación del bloqueo de duplicados. */
    static String normalizarComprobante(String numero) {
        return numero.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    /**
     * Guarda la CxP y fuerza el INSERT. Si el índice único (proveedor, comprobante) lo rechaza —otro alta idéntica ganó
     * la carrera— se responde el mismo duplicado que el chequeo previo; cualquier otra violación se propaga.
     */
    private CuentaPorPagar guardarComprobando(CuentaPorPagar cuenta, String numeroComprobante) {
        try {
            return cuentaRepository.saveAndFlush(cuenta);
        } catch (DataIntegrityViolationException e) {
            if (numeroComprobante != null && causaMenciona(e, NOMBRE_INDICE_COMPROBANTE)) {
                throw new ComprobanteProveedorDuplicadoException(numeroComprobante);
            }
            throw e;
        }
    }

    private static boolean causaMenciona(Throwable error, String texto) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().toLowerCase(Locale.ROOT).contains(texto)) {
                return true;
            }
        }
        return false;
    }

    private synchronized String generarNumero() {
        int anio = LocalDateTime.now().getYear();
        Integer ultimo = cuentaRepository.findUltimoConsecutivo(anio);
        int siguiente = (ultimo == null ? 0 : ultimo) + 1;
        return String.format("CPP-%d-%05d", anio, siguiente);
    }
}
