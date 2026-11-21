package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.service.CuentaBancariaService;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.dto.DevolucionSaldoFavorDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoDevolucionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.enums.MedioSolicitudDevolucion;
import api.astro.whats_orders_manager.modules.facturacion.enums.OrigenTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.exception.SaldoFavorConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.SaldoFavorPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.DevolucionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.SaldoFavorCliente;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.DevolucionSaldoFavorRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.SaldoFavorClienteRepository;
import api.astro.whats_orders_manager.modules.facturacion.util.CalculoDevolucionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.util.IbanCostaRica;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Devolución en dinero de un saldo a favor (T-049, ADR-058, fase B2 de ADR-053).
 *
 * <ul>
 *   <li>{@link #solicitar}: sobre un saldo VIGENTE, con el saldo bloqueado FOR UPDATE; reserva el monto.</li>
 *   <li>{@link #aprobar}: con la declaración de verificación; el aprobador no es el solicitante ni quien emitió la nota.</li>
 *   <li>{@link #rechazar} (desde SOLICITADA) y {@link #anular} (desde SOLICITADA o APROBADA): con razón; liberan la reserva.</li>
 *   <li>{@link #pagar}: desde APROBADA, por un usuario distinto del solicitante; en una sola transacción baja el saldo
 *       vivo, libera la reserva y genera el único asiento (DR 2.1.05 × TC origen, CR hoja del banco × TC del día,
 *       diferencial realizado). Una PAGADA no se reversa en B2.</li>
 * </ul>
 *
 * Orden de bloqueo: siempre el saldo y luego la devolución (igual que {@code SaldoFavorService}: saldo antes que factura).
 * La devolución no es un {@code Pago}: nunca entra en caja ni en los pagos del día. Autorización por permiso en el
 * controlador; aquí solo la segregación de funciones.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DevolucionSaldoFavorService {

    public static final String MSG_NO_VIGENTE = SaldoFavorService.MSG_NO_VIGENTE;
    public static final String MSG_MONTO = SaldoFavorService.MSG_MONTO;
    public static final String MSG_EXCEDE_DISPONIBLE = SaldoFavorService.MSG_EXCEDE_DISPONIBLE;
    public static final String MSG_IBAN = "El IBAN no es válido: debe ser una cuenta costarricense (CR y 20 dígitos) "
        + "con dígito de control correcto";
    public static final String MSG_IDENTIFICACION = "La identificación del titular debe ser la del cliente del saldo a favor";
    public static final String MSG_TITULAR = "El nombre del titular debe tener entre 3 y 100 caracteres";
    public static final String MSG_SOLICITUD = "El medio y la fecha de la solicitud del cliente son obligatorios y la "
        + "fecha no puede ser futura";
    public static final String MSG_SEG_APROBAR = "La devolución debe aprobarla un usuario distinto del que la solicitó "
        + "y del que emitió la nota";
    public static final String MSG_SEG_PAGAR = "El pago de la devolución debe hacerlo un usuario distinto del que la solicitó";
    public static final String MSG_TRANSICION = "La devolución no está en un estado que permita esta operación";
    public static final String MSG_DECLARACION = "Debe declarar que verificó que la cuenta pertenece al cliente";
    public static final String MSG_RAZON = "La razón debe tener entre 3 y 180 caracteres";
    public static final String MSG_COMPROBANTE = "El número y la fecha del comprobante bancario son obligatorios y la "
        + "fecha no puede ser futura";
    public static final String MSG_CUENTA = "La cuenta bancaria de la empresa es obligatoria, debe estar activa y en la "
        + "misma moneda del saldo a favor";
    public static final String MSG_USUARIO = SaldoFavorService.MSG_USUARIO;

    private final SaldoFavorClienteRepository saldoRepository;
    private final DevolucionSaldoFavorRepository devolucionRepository;
    private final CuentaBancariaService cuentaBancariaService;
    private final AsientoContableService asientoService;
    private final TipoCambioService tipoCambioService;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    /** Datos de la solicitud del cliente (ADR-058 §4 y §5). */
    public record Solicitud(BigDecimal monto, String iban, String nombreTitular, String identificacionTitular,
                            MedioSolicitudDevolucion medioSolicitud, LocalDate fechaSolicitud) {
    }

    // ── Solicitud ────────────────────────────────────────────────────────────

    /**
     * Registra la solicitud y reserva el monto. Con el saldo bloqueado FOR UPDATE valida (409, mensaje fijo, sin crear
     * nada): saldo VIGENTE; monto &gt; 0 con a lo sumo dos decimales y ≤ saldo vivo − reservado; IBAN costarricense válido;
     * identificación del titular igual a la del cliente (normalizada); nombre de 3 a 100 caracteres; medio y fecha de la
     * solicitud obligatorios, fecha no futura.
     */
    @Transactional
    public DevolucionSaldoFavor solicitar(Long idSaldo, Solicitud pedido, Integer usuarioId) {
        if (usuarioId == null) {
            throw new SaldoFavorConflictoException(MSG_USUARIO);
        }
        if (pedido == null) {
            throw new SaldoFavorPedidoInvalidoException("El pedido es obligatorio");
        }
        SaldoFavorCliente saldo = saldoRepository.findByIdParaActualizar(idSaldo)
            .orElseThrow(() -> new NoSuchElementException("El saldo a favor no existe"));
        if (saldo.getEstado() != EstadoSaldoFavor.VIGENTE) {
            throw new SaldoFavorConflictoException(MSG_NO_VIGENTE);
        }
        BigDecimal monto = pedido.monto();
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 2) {
            throw new SaldoFavorConflictoException(MSG_MONTO);
        }
        if (monto.compareTo(saldo.getDisponible()) > 0) {
            throw new SaldoFavorConflictoException(MSG_EXCEDE_DISPONIBLE);
        }
        if (!IbanCostaRica.esValido(pedido.iban())) {
            throw new SaldoFavorConflictoException(MSG_IBAN);
        }
        String identificacion = normalizarIdentificacion(pedido.identificacionTitular());
        if (identificacion.isEmpty() || !identificacion.equals(identificacionDelCliente(saldo.getCliente()))) {
            throw new SaldoFavorConflictoException(MSG_IDENTIFICACION);
        }
        String nombre = pedido.nombreTitular() == null ? "" : pedido.nombreTitular().trim();
        if (nombre.length() < 3 || nombre.length() > 100) {
            throw new SaldoFavorConflictoException(MSG_TITULAR);
        }
        LocalDate hoy = LocalDate.now(clock);
        if (pedido.medioSolicitud() == null || pedido.fechaSolicitud() == null || pedido.fechaSolicitud().isAfter(hoy)) {
            throw new SaldoFavorConflictoException(MSG_SOLICITUD);
        }

        DevolucionSaldoFavor d = new DevolucionSaldoFavor();
        d.setSaldo(saldo);
        d.setMonto(monto);
        d.setEstado(EstadoDevolucionSaldoFavor.SOLICITADA);
        d.setMedioSolicitud(pedido.medioSolicitud());
        d.setFechaSolicitud(pedido.fechaSolicitud());
        d.setIban(IbanCostaRica.normalizar(pedido.iban()));
        d.setNombreTitular(nombre);
        d.setIdentificacionTitular(identificacion);
        d.setSolicitadoPor(usuarioId);
        d.setFechaCreacion(LocalDateTime.now(clock));
        saldo.reservar(monto);
        saldoRepository.save(saldo);
        d = devolucionRepository.save(d);
        log.info("Devolución {} de {} {} solicitada sobre el saldo a favor {} por el usuario {}", d.getId(), monto,
            saldo.getMoneda(), saldo.getId(), usuarioId);
        return d;
    }

    // ── Aprobación, rechazo y anulación ──────────────────────────────────────

    /**
     * Aprueba una devolución SOLICITADA. Segregación: el aprobador no puede ser quien la solicitó ni quien emitió la nota de
     * origen (se compara el nombre o el correo del aprobador, sin distinguir mayúsculas, con {@code usuarioCreador} de la
     * nota, que guarda el nombre de autenticación del emisor).
     *
     * @throws SaldoFavorPedidoInvalidoException falta la declaración de verificación (400)
     */
    @Transactional
    public DevolucionSaldoFavor aprobar(Long idDevolucion, boolean verificoCuenta, Integer usuarioId) {
        if (!verificoCuenta) {
            throw new SaldoFavorPedidoInvalidoException(MSG_DECLARACION);
        }
        if (usuarioId == null) {
            throw new SaldoFavorConflictoException(MSG_USUARIO);
        }
        DevolucionSaldoFavor d = cargarBloqueada(idDevolucion).devolucion();
        exigirEstado(d, EstadoDevolucionSaldoFavor.SOLICITADA);
        if (usuarioId.equals(d.getSolicitadoPor()) || esEmisorDeLaNota(d.getSaldo().getNota(), usuarioId)) {
            throw new SaldoFavorConflictoException(MSG_SEG_APROBAR);
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        d.setEstado(EstadoDevolucionSaldoFavor.APROBADA);
        d.setAprobadoPor(usuarioId);
        d.setFechaAprobacion(ahora);
        d.setVerificoCuenta(true);
        d.setFechaVerificacion(ahora);
        log.info("Devolución {} aprobada por el usuario {}", d.getId(), usuarioId);
        return devolucionRepository.save(d);
    }

    /** Rechaza una devolución SOLICITADA con razón de 3 a 180 caracteres y libera la reserva. */
    @Transactional
    public DevolucionSaldoFavor rechazar(Long idDevolucion, String razon, Integer usuarioId) {
        String razonLimpia = validarRazon(razon);
        if (usuarioId == null) {
            throw new SaldoFavorConflictoException(MSG_USUARIO);
        }
        Bloqueadas b = cargarBloqueada(idDevolucion);
        exigirEstado(b.devolucion(), EstadoDevolucionSaldoFavor.SOLICITADA);
        DevolucionSaldoFavor d = b.devolucion();
        b.saldo().liberarReserva(d.getMonto());
        saldoRepository.save(b.saldo());
        d.setEstado(EstadoDevolucionSaldoFavor.RECHAZADA);
        d.setRazon(razonLimpia);
        d.setRechazadoPor(usuarioId);
        d.setFechaRechazo(LocalDateTime.now(clock));
        log.warn("Devolución {} rechazada por el usuario {}: {}", d.getId(), usuarioId, razonLimpia);
        return devolucionRepository.save(d);
    }

    /** Anula una devolución SOLICITADA o APROBADA (antes de pagarse) con razón de 3 a 180 caracteres; libera la reserva. */
    @Transactional
    public DevolucionSaldoFavor anular(Long idDevolucion, String razon, Integer usuarioId) {
        String razonLimpia = validarRazon(razon);
        if (usuarioId == null) {
            throw new SaldoFavorConflictoException(MSG_USUARIO);
        }
        Bloqueadas b = cargarBloqueada(idDevolucion);
        DevolucionSaldoFavor d = b.devolucion();
        if (d.getEstado() != EstadoDevolucionSaldoFavor.SOLICITADA && d.getEstado() != EstadoDevolucionSaldoFavor.APROBADA) {
            throw new SaldoFavorConflictoException(MSG_TRANSICION);
        }
        b.saldo().liberarReserva(d.getMonto());
        saldoRepository.save(b.saldo());
        d.setEstado(EstadoDevolucionSaldoFavor.ANULADA);
        d.setRazon(razonLimpia);
        d.setAnuladoPor(usuarioId);
        d.setFechaAnulacion(LocalDateTime.now(clock));
        log.warn("Devolución {} anulada por el usuario {}: {}", d.getId(), usuarioId, razonLimpia);
        return devolucionRepository.save(d);
    }

    // ── Pago ─────────────────────────────────────────────────────────────────

    /**
     * Paga una devolución APROBADA. Valida la segregación (pagador distinto del solicitante), la cuenta bancaria de la
     * empresa (activa y de la moneda del saldo) y el comprobante (número y fecha no futura). Con el saldo bloqueado: baja
     * el saldo vivo, libera la reserva y genera el asiento. En USD el TC es solo el de {@code tipos_cambio} de la fecha del
     * pago (ADR-056 §3): sin él, 409 y no se crea nada.
     */
    @Transactional
    public DevolucionSaldoFavor pagar(Long idDevolucion, Integer idCuentaBancaria, String numeroComprobante,
                                      LocalDate fechaComprobante, Integer usuarioId) {
        if (usuarioId == null) {
            throw new SaldoFavorConflictoException(MSG_USUARIO);
        }
        Bloqueadas b = cargarBloqueada(idDevolucion);
        DevolucionSaldoFavor d = b.devolucion();
        SaldoFavorCliente saldo = b.saldo();
        exigirEstado(d, EstadoDevolucionSaldoFavor.APROBADA);
        if (usuarioId.equals(d.getSolicitadoPor())) {
            throw new SaldoFavorConflictoException(MSG_SEG_PAGAR);
        }
        MonedaFE moneda = MonedaFE.valueOf(saldo.getMoneda());
        CuentaBancaria cuenta;
        try {
            cuenta = cuentaBancariaService.resolverParaPago(idCuentaBancaria, moneda, MSG_CUENTA);
        } catch (IllegalArgumentException e) {
            throw new SaldoFavorConflictoException(MSG_CUENTA);
        }
        LocalDate hoy = LocalDate.now(clock);
        String comprobante = numeroComprobante == null ? "" : numeroComprobante.trim();
        if (comprobante.isEmpty() || comprobante.length() > 50 || fechaComprobante == null
            || fechaComprobante.isAfter(hoy)) {
            throw new SaldoFavorConflictoException(MSG_COMPROBANTE);
        }

        BigDecimal tcDia = null;
        TipoCambio filaDelDia = null;
        if (moneda != MonedaFE.CRC) {
            filaDelDia = tipoCambioService.obtenerTipoCambioDeLaFecha(moneda, hoy)
                .orElseThrow(() -> new SaldoFavorConflictoException("No hay tipo de cambio registrado para " + hoy
                    + "; regístrelo antes de pagar la devolución"));
            tcDia = filaDelDia.getTasaVenta();
        }
        CalculoDevolucionSaldoFavor calculo = CalculoDevolucionSaldoFavor.calcular(d.getMonto(),
            saldo.getTipoCambioOrigen(), tcDia);

        d.setNumeroComprobante(comprobante);
        d.setFechaComprobante(fechaComprobante);
        AsientoContable asiento;
        try {
            asiento = asientoService.generarAsientoDevolucionSaldoFavor(d, saldo, cuenta, calculo, hoy);
        } catch (IllegalStateException e) {
            throw new SaldoFavorConflictoException(e.getMessage());
        }

        saldo.pagarDevolucion(d.getMonto());
        saldoRepository.save(saldo);

        d.setEstado(EstadoDevolucionSaldoFavor.PAGADA);
        d.setPagadoPor(usuarioId);
        d.setFechaPago(LocalDateTime.now(clock));
        d.setCuentaBancaria(cuenta);
        d.setTipoCambioOrigen(saldo.getTipoCambioOrigen());
        if (filaDelDia != null) {
            d.setTcDiaFecha(filaDelDia.getFecha());
            d.setTcDiaValor(filaDelDia.getTasaVenta());
            d.setTcDiaTipo("VENTA");
            d.setTcDiaFuente(filaDelDia.getFuente().name());
            d.setTcDiaOrigen(OrigenTipoCambio.de(filaDelDia.getFuente()));
        }
        d.setDiferencialSaldo(calculo.diferencialSaldo());
        d.setDiferencialNeto(calculo.diferencialNeto());
        d.setAsiento(asiento);
        d = devolucionRepository.save(d);
        log.info("Devolución {} pagada por el usuario {}: {} {} desde {} (asiento {})", d.getId(), usuarioId, d.getMonto(),
            saldo.getMoneda(), cuenta.getEntidad(), asiento.getNumero());
        return d;
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DevolucionSaldoFavorDTO resumen(Long idDevolucion) {
        DevolucionSaldoFavor d = devolucionRepository.findById(idDevolucion)
            .orElseThrow(() -> new NoSuchElementException("La devolución no existe"));
        return aDtos(List.of(d)).get(0);
    }

    /** Devoluciones de un saldo, de la más antigua a la más reciente. */
    @Transactional(readOnly = true)
    public List<DevolucionSaldoFavorDTO> porSaldo(Long idSaldo) {
        if (!saldoRepository.existsById(idSaldo)) {
            throw new NoSuchElementException("El saldo a favor no existe");
        }
        return aDtos(devolucionRepository.findPorSaldo(idSaldo));
    }

    /** Devoluciones de varios saldos, agrupadas por saldo (estado de cuenta del cliente). */
    @Transactional(readOnly = true)
    public Map<Long, List<DevolucionSaldoFavorDTO>> porSaldos(Collection<Long> idsSaldo) {
        if (idsSaldo.isEmpty()) {
            return Map.of();
        }
        return aDtos(devolucionRepository.findPorSaldos(idsSaldo)).stream()
            .collect(Collectors.groupingBy(DevolucionSaldoFavorDTO::idSaldoFavor));
    }

    // ── Apoyo ────────────────────────────────────────────────────────────────

    private record Bloqueadas(SaldoFavorCliente saldo, DevolucionSaldoFavor devolucion) {
    }

    /** Bloquea el saldo y luego la devolución (siempre en ese orden); la devolución se relee ya bloqueada. */
    private Bloqueadas cargarBloqueada(Long idDevolucion) {
        Long idSaldo = devolucionRepository.findById(idDevolucion)
            .orElseThrow(() -> new NoSuchElementException("La devolución no existe")).getSaldo().getId();
        SaldoFavorCliente saldo = saldoRepository.findByIdParaActualizar(idSaldo)
            .orElseThrow(() -> new NoSuchElementException("El saldo a favor no existe"));
        DevolucionSaldoFavor d = devolucionRepository.findByIdParaActualizar(idDevolucion)
            .orElseThrow(() -> new NoSuchElementException("La devolución no existe"));
        return new Bloqueadas(saldo, d);
    }

    private static void exigirEstado(DevolucionSaldoFavor d, EstadoDevolucionSaldoFavor esperado) {
        if (d.getEstado() != esperado) {
            throw new SaldoFavorConflictoException(MSG_TRANSICION);
        }
    }

    private static String validarRazon(String razon) {
        String limpia = razon == null ? "" : razon.trim();
        if (limpia.length() < 3 || limpia.length() > 180) {
            throw new SaldoFavorPedidoInvalidoException(MSG_RAZON);
        }
        return limpia;
    }

    /** Solo letras y dígitos, en mayúsculas: sin guiones, espacios ni puntos. */
    static String normalizarIdentificacion(String valor) {
        return valor == null ? "" : valor.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /** Identificación del cliente normalizada: {@code numeroIdentificacion}; si es nula, la {@code identificacion} heredada. */
    static String identificacionDelCliente(Cliente cliente) {
        String numero = normalizarIdentificacion(cliente.getNumeroIdentificacion());
        return numero.isEmpty() ? normalizarIdentificacion(cliente.getIdentificacion()) : numero;
    }

    /** ¿El usuario es quien emitió la nota? {@code usuarioCreador} es texto: el nombre de autenticación (nombre o correo). */
    private boolean esEmisorDeLaNota(NotaFactura nota, Integer usuarioId) {
        String emisor = nota.getUsuarioCreador();
        if (emisor == null || emisor.isBlank()) {
            return false;
        }
        Usuario u = usuarioRepository.findById(usuarioId).orElse(null);
        if (u == null) {
            return false;
        }
        String e = emisor.trim();
        return e.equalsIgnoreCase(u.getEmail() == null ? null : u.getEmail().trim())
            || e.equalsIgnoreCase(u.getNombre() == null ? null : u.getNombre().trim());
    }

    private List<DevolucionSaldoFavorDTO> aDtos(List<DevolucionSaldoFavor> devoluciones) {
        Set<Integer> ids = new HashSet<>();
        for (DevolucionSaldoFavor d : devoluciones) {
            for (Integer id : new Integer[]{d.getSolicitadoPor(), d.getAprobadoPor(), d.getRechazadoPor(),
                d.getAnuladoPor(), d.getPagadoPor()}) {
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        Map<Integer, String> nombres = new HashMap<>();
        usuarioRepository.findAllById(ids).forEach(u -> nombres.put(u.getIdUsuario(), u.getNombre()));
        return devoluciones.stream().map(d -> aDto(d, nombres)).toList();
    }

    private static DevolucionSaldoFavorDTO aDto(DevolucionSaldoFavor d, Map<Integer, String> n) {
        CuentaBancaria c = d.getCuentaBancaria();
        AsientoContable a = d.getAsiento();
        return new DevolucionSaldoFavorDTO(d.getId(), d.getSaldo().getId(), d.getSaldo().getMoneda(), d.getMonto(),
            d.getEstado().name(), d.getMedioSolicitud().name(), d.getFechaSolicitud(), d.getIban(), d.getNombreTitular(),
            d.getIdentificacionTitular(), d.getSolicitadoPor(), n.get(d.getSolicitadoPor()), d.getFechaCreacion(),
            d.getAprobadoPor(), d.getAprobadoPor() == null ? null : n.get(d.getAprobadoPor()), d.getFechaAprobacion(),
            d.isVerificoCuenta(), d.getFechaVerificacion(),
            d.getRechazadoPor(), d.getRechazadoPor() == null ? null : n.get(d.getRechazadoPor()), d.getFechaRechazo(),
            d.getAnuladoPor(), d.getAnuladoPor() == null ? null : n.get(d.getAnuladoPor()), d.getFechaAnulacion(),
            d.getRazon(), d.getPagadoPor(), d.getPagadoPor() == null ? null : n.get(d.getPagadoPor()), d.getFechaPago(),
            c == null ? null : c.getIdCuentaBancaria(), c == null ? null : c.getEntidad(), d.getNumeroComprobante(),
            d.getFechaComprobante(), d.getTipoCambioOrigen(), d.getTcDiaFecha(), d.getTcDiaValor(), d.getTcDiaTipo(),
            d.getTcDiaFuente(), d.getTcDiaOrigen() == null ? null : d.getTcDiaOrigen().name(), d.getDiferencialSaldo(),
            d.getDiferencialNeto(), a == null ? null : a.getIdAsiento(), a == null ? null : a.getNumero());
    }
}
