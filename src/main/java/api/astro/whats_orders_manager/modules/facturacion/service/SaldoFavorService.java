package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.dto.DevolucionSaldoFavorDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.SaldoFavorDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoAplicacionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.OrigenTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import api.astro.whats_orders_manager.modules.facturacion.exception.SaldoFavorConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.SaldoFavorPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.AplicacionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.model.SaldoFavorCliente;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.AplicacionSaldoFavorRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.PagoRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.SaldoFavorClienteRepository;
import api.astro.whats_orders_manager.modules.facturacion.util.CalculoAplicacionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.util.ConversionMoneda;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saldo a favor del cliente (T-048, fase B1 de ADR-053; cuentas de ADR-054, diferencial de ADR-055, TC de ADR-056 §3,
 * alcance de ADR-057).
 *
 * <ul>
 *   <li>{@link #crearDesdeNota}: el excedente de una nota aplicada sobre el saldo pendiente de su factura; la llama
 *       {@code NotaFacturaEfectos} en la misma transacción en que la nota pasa a APLICADA.</li>
 *   <li>{@link #aplicar}: aplicación manual de un monto del saldo a otra factura del mismo cliente y moneda, como un pago
 *       {@code NOTA_CREDITO}, con su asiento (débito a saldos a favor, crédito a CxC y el diferencial realizado). Una sola
 *       transacción, con el saldo y la factura bloqueados FOR UPDATE (en ese orden, siempre).</li>
 *   <li>{@link #reversar}: reversión con razón: el pago NOTA_CREDITO se anula con el mecanismo de anulación de pagos
 *       ({@code PagoService.anularPagoDeSaldoFavor}), que revierte el asiento una sola vez (ADR-023); el saldo vivo se
 *       restituye y nada se borra.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaldoFavorService {

    public static final String MSG_NO_VIGENTE = "El saldo a favor no está vigente";
    public static final String MSG_CLIENTE = "El saldo a favor solo se aplica a facturas del mismo cliente";
    public static final String MSG_MONEDA = "El saldo a favor solo se aplica a facturas en la misma moneda";
    public static final String MSG_MONTO = "El monto debe ser mayor a 0 y tener a lo sumo dos decimales";
    public static final String MSG_EXCEDE_SALDO = "El monto excede el saldo vivo del saldo a favor";
    /** T-049: hay devoluciones en trámite que reservan parte del saldo vivo (ADR-058 §2). */
    public static final String MSG_EXCEDE_DISPONIBLE = "El monto excede el saldo disponible del saldo a favor "
        + "(saldo vivo menos lo reservado por devoluciones en trámite)";
    public static final String MSG_EXCEDE_FACTURA = "El monto excede el saldo pendiente de la factura destino";
    public static final String MSG_ANULADA = "La factura destino está anulada";
    public static final String MSG_ACREDITADA = "La factura destino está acreditada totalmente";
    public static final String MSG_ORIGEN = "El saldo a favor no se aplica a la factura que lo originó";
    public static final String MSG_YA_REVERSADA = "La aplicación ya fue reversada";
    public static final String MSG_REVERSAR_DESTINO_ANULADA = "No se puede reversar: la factura destino está anulada";
    public static final String MSG_RAZON = "La razón de la reversión debe tener entre 3 y 180 caracteres";
    public static final String MSG_USUARIO = "No se pudo identificar al usuario";

    private final SaldoFavorClienteRepository saldoRepository;
    private final AplicacionSaldoFavorRepository aplicacionRepository;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final PagoService pagoService;
    private final NumeroPagoGeneratorService numeroPagoGenerator;
    private final AsientoContableService asientoService;
    private final AsientoContableRepository asientoRepository;
    private final TipoCambioService tipoCambioService;
    private final NotaFacturaPrecondiciones notaPrecondiciones;
    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final DevolucionSaldoFavorService devolucionService;
    private final Clock clock;

    // ── Generación al aceptarse la nota (criterio 6) ─────────────────────────

    /**
     * Crea el saldo a favor de una nota aplicada por su excedente: monto original = saldo vivo = {@code excedente}, TC de
     * origen el de la nota (el de la factura), fecha de origen la fecha contable de la nota.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SaldoFavorCliente crearDesdeNota(NotaFactura nota, BigDecimal excedente, String claveNota) {
        if (excedente == null || excedente.signum() <= 0) {
            throw new IllegalArgumentException("El excedente de la nota debe ser mayor a 0");
        }
        Factura factura = nota.getFactura();
        SaldoFavorCliente saldo = new SaldoFavorCliente();
        saldo.setCliente(factura.getCliente());
        saldo.setMoneda(nota.getMoneda());
        saldo.setMontoOriginal(excedente);
        saldo.setSaldoVivo(excedente);
        saldo.setTipoCambioOrigen("CRC".equals(nota.getMoneda()) ? BigDecimal.ONE : nota.getTipoCambio());
        saldo.setFechaOrigen(nota.getFechaContable());
        saldo.setNota(nota);
        saldo.setClaveNota(claveNota);
        saldo.setFacturaOrigen(factura);
        saldo.setEstado(EstadoSaldoFavor.VIGENTE);
        saldo = saldoRepository.save(saldo);
        log.info("Saldo a favor {} de {} {} creado por la nota {} (factura {})", saldo.getId(), excedente,
            saldo.getMoneda(), nota.getId(), factura.getNumeroFactura());
        return saldo;
    }

    // ── Aplicación (criterios 9 a 11) ────────────────────────────────────────

    /**
     * Aplica {@code monto} del saldo a la factura destino.
     *
     * @param usuarioId id del usuario que aplica
     * @throws NoSuchElementException          el saldo o la factura no existen (404)
     * @throws SaldoFavorConflictoException    una validación falla, falta el tipo de cambio del día o un parámetro contable
     *                                         (409, mensaje fijo): no se crea nada
     */
    @Transactional
    public AplicacionSaldoFavor aplicar(Long idSaldo, Integer idFactura, BigDecimal monto, Integer usuarioId) {
        if (usuarioId == null) {
            throw new SaldoFavorConflictoException(MSG_USUARIO);
        }
        if (idFactura == null || monto == null) {
            throw new SaldoFavorPedidoInvalidoException("La factura y el monto son obligatorios");
        }
        // Orden de bloqueo: saldo y luego factura (también al reversar), para no entrar en interbloqueo
        SaldoFavorCliente saldo = saldoRepository.findByIdParaActualizar(idSaldo)
            .orElseThrow(() -> new NoSuchElementException("El saldo a favor no existe"));
        Factura destino = facturaRepository.findByIdParaActualizar(idFactura)
            .orElseThrow(() -> new NoSuchElementException("La factura no existe"));

        validarAplicacion(saldo, destino, monto);

        MonedaFE moneda = MonedaFE.valueOf(saldo.getMoneda());
        LocalDate hoy = LocalDate.now(clock);
        BigDecimal tcFactura = BigDecimal.ONE;
        BigDecimal tcDia = null;
        TipoCambio filaDelDia = null;
        if (moneda != MonedaFE.CRC) {
            filaDelDia = tipoCambioService.obtenerTipoCambioDeLaFecha(moneda, hoy)
                .orElseThrow(() -> new SaldoFavorConflictoException("No hay tipo de cambio registrado para " + hoy
                    + "; regístrelo antes de aplicar el saldo"));
            tcDia = filaDelDia.getTasaVenta();
            tcFactura = destino.getTipoCambio();
            if (tcFactura == null || tcFactura.signum() <= 0) {
                throw new SaldoFavorConflictoException("La factura destino no tiene tipo de cambio");
            }
        }
        CalculoAplicacionSaldoFavor calculo = CalculoAplicacionSaldoFavor.calcular(monto, saldo.getTipoCambioOrigen(),
            tcFactura, tcDia);

        LocalDateTime ahora = LocalDateTime.now(clock);
        Pago pago = crearPago(saldo, destino, monto, moneda, tcFactura, ahora);
        // Cuenta como pagado para el saldo y el estado de pago de la factura (misma lógica de siempre)
        destino.agregarPago(pago);
        facturaRepository.save(destino);

        AsientoContable asiento;
        try {
            asiento = asientoService.generarAsientoAplicacionSaldoFavor(saldo, destino, pago, calculo, hoy);
        } catch (IllegalStateException e) {
            throw new SaldoFavorConflictoException(e.getMessage());
        }

        saldo.consumir(monto);
        saldoRepository.save(saldo);

        AplicacionSaldoFavor aplicacion = new AplicacionSaldoFavor();
        aplicacion.setSaldo(saldo);
        aplicacion.setFactura(destino);
        aplicacion.setPago(pago);
        aplicacion.setMonto(monto);
        aplicacion.setTipoCambioOrigen(saldo.getTipoCambioOrigen());
        aplicacion.setTipoCambioFactura(tcFactura);
        if (filaDelDia != null) {
            aplicacion.setTcDiaFecha(filaDelDia.getFecha());
            aplicacion.setTcDiaValor(filaDelDia.getTasaVenta());
            aplicacion.setTcDiaTipo("VENTA");
            aplicacion.setTcDiaFuente(filaDelDia.getFuente().name());
            aplicacion.setTcDiaOrigen(OrigenTipoCambio.de(filaDelDia.getFuente()));
        }
        aplicacion.setDiferencialSaldo(calculo.diferencialSaldo());
        aplicacion.setDiferencialCxc(calculo.diferencialCxc());
        aplicacion.setDiferencialNeto(calculo.diferencialNeto());
        aplicacion.setAsiento(asiento);
        aplicacion.setEstado(EstadoAplicacionSaldoFavor.VIGENTE);
        aplicacion.setUsuarioId(usuarioId);
        aplicacion.setFechaAplicacion(ahora);
        aplicacion = aplicacionRepository.save(aplicacion);
        log.info("Saldo a favor {}: aplicados {} {} a la factura {} (pago {}, asiento {})", saldo.getId(), monto,
            saldo.getMoneda(), destino.getNumeroFactura(), pago.getNumeroPago(), asiento.getNumero());
        return aplicacion;
    }

    /** Validaciones del criterio 9, con el saldo y la factura ya bloqueados. Mensajes fijos, 409. */
    private void validarAplicacion(SaldoFavorCliente saldo, Factura destino, BigDecimal monto) {
        if (saldo.getEstado() != EstadoSaldoFavor.VIGENTE) {
            throw new SaldoFavorConflictoException(MSG_NO_VIGENTE);
        }
        if (destino.estaAnulada()) {
            throw new SaldoFavorConflictoException(MSG_ANULADA);
        }
        if (destino.estaAcreditadaTotalmente()) {
            throw new SaldoFavorConflictoException(MSG_ACREDITADA);
        }
        if (destino.getIdFactura().equals(saldo.getFacturaOrigen().getIdFactura())) {
            throw new SaldoFavorConflictoException(MSG_ORIGEN);
        }
        // ADR-037 §3: con una nota EN_TRAMITE la factura no admite pagos nuevos, tampoco por saldo a favor
        notaPrecondiciones.exigirSinNotaEnTramite(destino.getIdFactura(), "aplicar un saldo a favor");
        if (!saldo.getCliente().getIdCliente().equals(destino.getCliente().getIdCliente())) {
            throw new SaldoFavorConflictoException(MSG_CLIENTE);
        }
        if (!saldo.getMoneda().equals(ConversionMoneda.oCrc(destino.getMonedaFE()).getCodigo())) {
            throw new SaldoFavorConflictoException(MSG_MONEDA);
        }
        if (monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 2) {
            throw new SaldoFavorConflictoException(MSG_MONTO);
        }
        if (monto.compareTo(saldo.getSaldoVivo()) > 0) {
            throw new SaldoFavorConflictoException(MSG_EXCEDE_SALDO);
        }
        // T-049: lo reservado por una devolución en trámite no se puede aplicar a una factura
        if (monto.compareTo(saldo.getDisponible()) > 0) {
            throw new SaldoFavorConflictoException(MSG_EXCEDE_DISPONIBLE);
        }
        if (monto.compareTo(destino.calcularSaldoPendiente()) > 0) {
            throw new SaldoFavorConflictoException(MSG_EXCEDE_FACTURA);
        }
    }

    /**
     * Pago NOTA_CREDITO sobre la factura destino. Se guarda directo (no por {@code registrarPago}): no es un cobro, no
     * genera el asiento de cobro en caja; el asiento de la aplicación lo genera este servicio. Método OTROS (99): el
     * catálogo de medios de pago no tiene un valor para notas.
     */
    private Pago crearPago(SaldoFavorCliente saldo, Factura destino, BigDecimal monto, MonedaFE moneda,
                           BigDecimal tcFactura, LocalDateTime ahora) {
        Pago pago = new Pago();
        pago.setCliente(destino.getCliente());
        pago.setFactura(destino);
        pago.setTipoPago(TipoPago.NOTA_CREDITO);
        pago.setMonto(monto);
        pago.setMoneda(moneda);
        pago.setTipoCambio(tcFactura);
        pago.setMontoAplicado(monto);
        pago.setMetodoPago(MetodoPago.OTROS);
        pago.setFechaPago(ahora);
        pago.setEstado(EstadoPago.CONFIRMADO);
        pago.setObservaciones("Aplicación de saldo a favor de la nota de crédito " + saldo.getNota().getId());
        pago.setNumeroPago(numeroPagoGenerator.generarNumeroPago(ahora.toLocalDate()));
        return pagoRepository.save(pago);
    }

    // ── Reversión (criterio 13) ──────────────────────────────────────────────

    /**
     * Reversa una aplicación VIGENTE: anula su pago NOTA_CREDITO con el mecanismo de anulación de pagos (que revierte el
     * asiento de la aplicación con un asiento espejo de hoy, ADR-023, y recalcula el estado de pago de la factura),
     * restituye el saldo vivo (vuelve a VIGENTE) y deja la razón, el usuario, la fecha y el asiento de reversión.
     *
     * @param razon de 3 a 180 caracteres
     * @throws SaldoFavorPedidoInvalidoException razón fuera de rango (400)
     * @throws SaldoFavorConflictoException      ya reversada o factura destino anulada (409)
     */
    @Transactional
    public AplicacionSaldoFavor reversar(Long idAplicacion, String razon, Integer usuarioId) {
        String razonLimpia = razon == null ? "" : razon.trim();
        if (razonLimpia.length() < 3 || razonLimpia.length() > 180) {
            throw new SaldoFavorPedidoInvalidoException(MSG_RAZON);
        }
        if (usuarioId == null) {
            throw new SaldoFavorConflictoException(MSG_USUARIO);
        }
        AplicacionSaldoFavor aplicacion = aplicacionRepository.findByIdParaActualizar(idAplicacion)
            .orElseThrow(() -> new NoSuchElementException("La aplicación no existe"));
        if (aplicacion.getEstado() == EstadoAplicacionSaldoFavor.REVERSADA) {
            throw new SaldoFavorConflictoException(MSG_YA_REVERSADA);
        }
        SaldoFavorCliente saldo = saldoRepository.findByIdParaActualizar(aplicacion.getSaldo().getId())
            .orElseThrow(() -> new NoSuchElementException("El saldo a favor no existe"));
        Factura destino = facturaRepository.findByIdParaActualizar(aplicacion.getFactura().getIdFactura())
            .orElseThrow(() -> new NoSuchElementException("La factura no existe"));
        if (destino.estaAnulada()) {
            // Su pagado ya pasó a un saldo a favor nuevo con la nota que la anuló: restituir este saldo lo duplicaría
            throw new SaldoFavorConflictoException(MSG_REVERSAR_DESTINO_ANULADA);
        }

        Long idPago = aplicacion.getPago().getIdPago();
        pagoService.anularPagoDeSaldoFavor(idPago,
            "Reversión de la aplicación de saldo a favor " + aplicacion.getId() + ": " + razonLimpia, usuarioId);
        AsientoContable reversion = asientoRepository
            .findFirstByAsientoRevertidoIdAsiento(aplicacion.getAsiento().getIdAsiento())
            .orElseThrow(() -> new IllegalStateException("La aplicación " + idAplicacion
                + " no tiene asiento de reversión: el pago no estaba contabilizado"));

        saldo.restituir(aplicacion.getMonto());
        saldoRepository.save(saldo);
        aplicacion.setEstado(EstadoAplicacionSaldoFavor.REVERSADA);
        aplicacion.setRazonReversion(razonLimpia);
        aplicacion.setReversadoPor(usuarioId);
        aplicacion.setFechaReversion(LocalDateTime.now(clock));
        aplicacion.setAsientoReversion(reversion);
        aplicacion = aplicacionRepository.save(aplicacion);
        log.warn("Aplicación {} del saldo a favor {} reversada por el usuario {}: {}", aplicacion.getId(),
            saldo.getId(), usuarioId, razonLimpia);
        return aplicacion;
    }

    // ── Consultas para la interfaz ───────────────────────────────────────────

    /** Saldos VIGENTES del cliente en la moneda (ordenados por fecha de origen). */
    @Transactional(readOnly = true)
    public List<SaldoFavorDTO.Saldo> listarVigentes(Integer idCliente, String moneda) {
        return aSaldoDTOs(saldoRepository.findPorClienteMonedaYEstado(idCliente, moneda.toUpperCase(),
            EstadoSaldoFavor.VIGENTE));
    }

    /**
     * Para el modal de pago de una factura: sus saldos VIGENTES (mismo cliente y moneda, sin los que ella misma originó) y
     * su saldo pendiente; más las aplicaciones ya hechas sobre ella.
     */
    @Transactional(readOnly = true)
    public SaldoFavorDTO.ParaFactura paraFactura(Integer idFactura) {
        Factura factura = facturaRepository.findById(idFactura)
            .orElseThrow(() -> new NoSuchElementException("La factura no existe"));
        String moneda = ConversionMoneda.oCrc(factura.getMonedaFE()).getCodigo();
        List<SaldoFavorCliente> vigentes = saldoRepository.findPorClienteMonedaYEstado(
            factura.getCliente().getIdCliente(), moneda, EstadoSaldoFavor.VIGENTE).stream()
            .filter(s -> !s.getFacturaOrigen().getIdFactura().equals(idFactura))
            .toList();
        boolean puedeRecibir = !factura.estaAnulada() && !factura.estaAcreditadaTotalmente();
        List<SaldoFavorDTO.Aplicacion> aplicadas = aplicacionRepository.findPorFacturaDestino(idFactura).stream()
            .map(this::aAplicacionDTO).toList();
        return new SaldoFavorDTO.ParaFactura(idFactura, factura.getNumeroFactura(), moneda,
            factura.calcularSaldoPendiente(), puedeRecibir, aSaldoDTOs(vigentes), aplicadas);
    }

    /** Todos los saldos del cliente (VIGENTES y AGOTADOS, por moneda) con sus aplicaciones. */
    @Transactional(readOnly = true)
    public List<SaldoFavorDTO.ConAplicaciones> porCliente(Integer idCliente) {
        List<SaldoFavorCliente> saldos = saldoRepository.findTodosDelCliente(idCliente);
        if (saldos.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AplicacionSaldoFavor>> aplicaciones = aplicacionRepository
            .findPorSaldos(saldos.stream().map(SaldoFavorCliente::getId).toList()).stream()
            .collect(Collectors.groupingBy(a -> a.getSaldo().getId()));
        Map<Long, SaldoFavorDTO.Saldo> dtos = aSaldoDTOs(saldos).stream()
            .collect(Collectors.toMap(SaldoFavorDTO.Saldo::id, Function.identity()));
        Map<Long, List<DevolucionSaldoFavorDTO>> devoluciones = devolucionService.porSaldos(
            saldos.stream().map(SaldoFavorCliente::getId).toList());
        return saldos.stream().map(s -> new SaldoFavorDTO.ConAplicaciones(dtos.get(s.getId()),
            aplicaciones.getOrDefault(s.getId(), List.of()).stream().map(this::aAplicacionDTO).toList(),
            devoluciones.getOrDefault(s.getId(), List.of()))).toList();
    }

    /** Aplicaciones por pago NOTA_CREDITO (para mostrar «Nota de origen» en el detalle de la factura destino). */
    @Transactional(readOnly = true)
    public Map<Long, SaldoFavorDTO.Aplicacion> aplicacionesPorPago(Collection<Long> idsPago) {
        if (idsPago.isEmpty()) {
            return Map.of();
        }
        List<AplicacionSaldoFavor> aplicaciones = aplicacionRepository.findPorPagos(idsPago);
        // consecutivos de las notas en una sola consulta (sin N+1)
        Map<Long, String> consecutivos = comprobanteRepository.findByNotaFactura_IdInAndVigenteTrue(aplicaciones.stream()
            .map(a -> a.getSaldo().getNota().getId()).distinct().toList()).stream()
            .filter(c -> c.getConsecutivo() != null && !c.getConsecutivo().isBlank())
            .collect(Collectors.toMap(c -> c.getNotaFactura().getId(), ComprobanteElectronico::getConsecutivo,
                (a, b) -> a));
        return aplicaciones.stream().collect(Collectors.toMap(a -> a.getPago().getIdPago(),
            a -> aAplicacionDTO(a, consecutivos.get(a.getSaldo().getNota().getId())), (a, b) -> a));
    }

    /** Resumen de una aplicación (respuesta de aplicar y reversar). */
    @Transactional(readOnly = true)
    public SaldoFavorDTO.Aplicacion resumen(Long idAplicacion) {
        return aplicacionRepository.findById(idAplicacion).map(this::aAplicacionDTO)
            .orElseThrow(() -> new NoSuchElementException("La aplicación no existe"));
    }

    private List<SaldoFavorDTO.Saldo> aSaldoDTOs(List<SaldoFavorCliente> saldos) {
        return saldos.stream().map(this::aSaldoDTO).toList();
    }

    private SaldoFavorDTO.Saldo aSaldoDTO(SaldoFavorCliente s) {
        NotaFactura nota = s.getNota();
        ComprobanteElectronico comprobante = comprobanteRepository.findFirstByNotaFactura_IdAndVigenteTrue(nota.getId())
            .orElse(null);
        return new SaldoFavorDTO.Saldo(s.getId(), s.getCliente().getIdCliente(), s.getMoneda(), s.getMontoOriginal(),
            s.getSaldoVivo(), s.getFechaOrigen(), s.getTipoCambioOrigen(), s.getEstado().name(), nota.getId(),
            SustitucionElegibilidad.numeroDeNota(nota, comprobante), s.getClaveNota(),
            s.getFacturaOrigen().getIdFactura(), s.getFacturaOrigen().getNumeroFactura(), s.getMontoReservado(),
            s.getDisponible());
    }

    private SaldoFavorDTO.Aplicacion aAplicacionDTO(AplicacionSaldoFavor a) {
        return aAplicacionDTO(a, comprobanteRepository
            .findFirstByNotaFactura_IdAndVigenteTrue(a.getSaldo().getNota().getId())
            .map(ComprobanteElectronico::getConsecutivo).orElse(null));
    }

    /** {@code consecutivo} de la nota (null si no tiene): sin él, el número de la nota es su clave. */
    private SaldoFavorDTO.Aplicacion aAplicacionDTO(AplicacionSaldoFavor a, String consecutivo) {
        Factura f = a.getFactura();
        NotaFactura nota = a.getSaldo().getNota();
        String numeroNota = consecutivo != null && !consecutivo.isBlank() ? consecutivo : a.getSaldo().getClaveNota();
        return new SaldoFavorDTO.Aplicacion(a.getId(), a.getSaldo().getId(), nota.getId(), numeroNota,
            a.getSaldo().getClaveNota(),
            a.getEstado().name(), a.getMonto(), a.getSaldo().getMoneda(), f.getIdFactura(), f.getNumeroFactura(),
            a.getPago().getIdPago(), a.getPago().getNumeroPago(), a.getFechaAplicacion(), a.getUsuarioId(),
            a.getTipoCambioOrigen(), a.getTipoCambioFactura(), a.getTcDiaFecha(), a.getTcDiaValor(), a.getTcDiaTipo(),
            a.getTcDiaFuente(), a.getTcDiaOrigen() != null ? a.getTcDiaOrigen().name() : null,
            a.getDiferencialSaldo(), a.getDiferencialCxc(), a.getDiferencialNeto(), a.getRazonReversion(),
            a.getReversadoPor(), a.getFechaReversion(),
            a.getAsientoReversion() != null ? a.getAsientoReversion().getIdAsiento() : null);
    }
}
