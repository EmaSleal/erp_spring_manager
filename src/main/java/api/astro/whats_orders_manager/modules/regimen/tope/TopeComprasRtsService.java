package api.astro.whats_orders_manager.modules.regimen.tope;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.util.ConversionMoneda;
import api.astro.whats_orders_manager.modules.regimen.aviso.NotificadorAdministradores;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService.Intervalo;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Tope anual de compras del régimen simplificado (T-056, ADR-072): parámetros por año (como la prorrata del IVA), acumulado
 * y avisos que nunca bloquean el registro.
 *
 * <ul>
 *   <li>Tope del año = salario base x multiplicador (con IVA, en CRC). Multiplicador 186 y umbrales 80 y 100 son los
 *       valores por defecto de la columna, no del código. Sin parámetros del año no hay tope: «Tope de AAAA sin
 *       configurar», sin usar los de otro año.</li>
 *   <li>Acumulado = suma en CRC del total con IVA de las CxP no anuladas con fecha de comprobante (o de registro, si no
 *       tiene) dentro del año y dentro de los tramos SIMPLIFICADO. Se calcula con los intervalos SIMPLIFICADO del año
 *       (RegimenTributarioService.intervalos) y una consulta por intervalo; la conversión a CRC se hace en memoria porque
 *       depende de la moneda y el tipo de cambio de cada CxP (los volúmenes de un año son pequeños).</li>
 *   <li>Sin repetir avisos: {@code nivel_notificado} guarda el último nivel avisado del año. Se notifica solo cuando el nivel
 *       actual es mayor que ese (cruce del umbral de aviso o del tope; saltar de BAJO a TOPE avisa una vez, del tope). Si el
 *       nivel baja (anulación, corrección de parámetros) se guarda el nuevo nivel sin avisar, y un nuevo cruce vuelve a avisar.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopeComprasRtsService {

    public static final String MSG_ANIO = "El año debe estar entre 2000 y 2100";
    public static final String MSG_SALARIO = "El salario base debe ser mayor que 0, con hasta 2 decimales";
    public static final String MSG_MULTIPLICADOR = "El multiplicador debe ser mayor que 0, con hasta 2 decimales";
    public static final String MSG_UMBRALES = "Los umbrales deben estar entre 0 y 100, y el de aviso menor que el de tope";
    public static final String MSG_NOTA = "La nota no puede exceder 300 caracteres";
    public static final String ADVERTENCIA_SOLO_ERP = "El acumulado solo incluye las compras registradas en el ERP";

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = 2100;
    private static final int LARGO_NOTA = 300;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal MAXIMO_SALARIO = new BigDecimal("9999999999999.99");
    private static final BigDecimal MAXIMO_MULTIPLICADOR = new BigDecimal("99999.99");

    private final TopeComprasRtsRepository repository;
    private final CuentaPorPagarRepository cuentaRepository;
    private final RegimenTributarioService regimenService;
    private final UsuarioActividadService actividadService;
    private final NotificadorAdministradores notificador;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;
    private final EntityManager entityManager;

    // ── Parámetros por año ───────────────────────────────────────────────────

    /**
     * @throws IllegalArgumentException datos inválidos (400)
     * @throws IllegalStateException    ya hay parámetros para ese año (409)
     */
    @Transactional
    public TopeComprasRts crear(TopeComprasRtsDTO dto, Usuario usuario) {
        Integer anio = validarAnio(dto.getAnio());
        BigDecimal salario = validarSalario(dto.getSalarioBase());
        BigDecimal multiplicador = validarMultiplicador(dto.getMultiplicador());
        validarUmbrales(dto.getUmbralAviso(), dto.getUmbralTope());
        String nota = validarNota(dto.getNota());
        if (repository.findByAnio(anio).isPresent()) {
            throw new IllegalStateException("Ya existe un tope de compras del RTS para " + anio + "; corríjalo en lugar de crear otro");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        TopeComprasRts guardado = repository.saveAndFlush(TopeComprasRts.builder().anio(anio).salarioBase(salario)
            .multiplicador(multiplicador).umbralAviso(escala(dto.getUmbralAviso())).umbralTope(escala(dto.getUmbralTope()))
            .nota(nota).usuarioId(idDe(usuario)).fechaRegistro(ahora).fechaActualizacion(ahora).build());
        entityManager.refresh(guardado); // trae los valores por defecto de la columna que se dejaron en null
        validarUmbralesFinales(guardado.getUmbralAviso(), guardado.getUmbralTope());
        auditar(usuario, "TOPE_COMPRAS_RTS_CREADO", "Tope de compras del RTS de " + anio + " registrado: salario base "
            + salario.toPlainString() + " x " + guardado.getMultiplicador().toPlainString(), guardado);
        return guardado;
    }

    /**
     * Corrige los parámetros del año; lo que viene en null se conserva. Conserva la fecha de registro.
     *
     * @throws IllegalArgumentException datos inválidos (400)
     * @throws NoSuchElementException   no hay parámetros para ese año (404)
     */
    @Transactional
    public TopeComprasRts corregir(Integer anio, TopeComprasRtsDTO dto, Usuario usuario) {
        validarAnio(anio);
        BigDecimal salario = dto.getSalarioBase() == null ? null : validarSalario(dto.getSalarioBase());
        BigDecimal multiplicador = validarMultiplicador(dto.getMultiplicador());
        validarUmbrales(dto.getUmbralAviso(), dto.getUmbralTope());
        String nota = validarNota(dto.getNota());
        TopeComprasRts existente = repository.findByAnio(anio)
            .orElseThrow(() -> new NoSuchElementException("No hay tope de compras del RTS configurado para " + anio));
        BigDecimal topeAnterior = existente.tope();
        if (salario != null) {
            existente.setSalarioBase(salario);
        }
        if (multiplicador != null) {
            existente.setMultiplicador(multiplicador);
        }
        if (dto.getUmbralAviso() != null) {
            existente.setUmbralAviso(escala(dto.getUmbralAviso()));
        }
        if (dto.getUmbralTope() != null) {
            existente.setUmbralTope(escala(dto.getUmbralTope()));
        }
        validarUmbralesFinales(existente.getUmbralAviso(), existente.getUmbralTope());
        existente.setNota(nota);
        existente.setUsuarioId(idDe(usuario));
        existente.setFechaActualizacion(LocalDateTime.now(clock));
        TopeComprasRts guardado = repository.saveAndFlush(existente);
        auditar(usuario, "TOPE_COMPRAS_RTS_CORREGIDO", "Tope de compras del RTS de " + anio + " corregido de "
            + topeAnterior.toPlainString() + " a " + guardado.tope().toPlainString(), guardado);
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<TopeComprasRts> listar() {
        return repository.findAllByOrderByAnioDesc();
    }

    @Transactional(readOnly = true)
    public Optional<TopeComprasRts> buscar(Integer anio) {
        return repository.findByAnio(anio);
    }

    // ── Acumulado y avisos ───────────────────────────────────────────────────

    /** Estado del año sin avisar a nadie. */
    @Transactional(readOnly = true)
    public EstadoTope estado(int anio) {
        return repository.findByAnio(anio).map(this::calcular).orElseGet(() -> EstadoTope.sinConfigurar(anio));
    }

    /**
     * Estado del año y aviso a los administradores si el nivel subió respecto del último avisado (ver la descripción de la
     * clase). Sin parámetros del año no avisa.
     */
    @Transactional
    public EstadoTope evaluar(int anio) {
        Optional<TopeComprasRts> parametros = repository.findByAnio(anio);
        if (parametros.isEmpty()) {
            return EstadoTope.sinConfigurar(anio);
        }
        TopeComprasRts p = parametros.get();
        EstadoTope estado = calcular(p);
        NivelTope previo = p.getNivelNotificado() == null ? NivelTope.BAJO : p.getNivelNotificado();
        if (estado.nivel().compareTo(previo) > 0) {
            notificador.notificar(titulo(estado), estado.mensaje());
        }
        if (estado.nivel() != previo) {
            p.setNivelNotificado(estado.nivel());
            repository.save(p);
        }
        return estado;
    }

    /** Evaluación del job diario: el año del reloj de la aplicación, en su propia transacción. */
    public void evaluarAnioActual() {
        int anio = LocalDate.now(clock).getYear();
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> evaluar(anio));
    }

    /**
     * Evaluación al registrar una CxP. Nunca lanza ni bloquea: con una transacción en curso espera a que confirme (así ve la
     * CxP nueva y un fallo no puede revertirla) y corre en su propia transacción; un error solo se registra.
     */
    public void evaluarTrasRegistro(int anio) {
        Runnable tarea = () -> {
            try {
                TransactionTemplate plantilla = new TransactionTemplate(transactionManager);
                plantilla.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                plantilla.executeWithoutResult(s -> evaluar(anio));
            } catch (RuntimeException e) {
                log.warn("No se pudo evaluar el tope de compras del RTS de {}: {}", anio, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tarea.run();
                }
            });
        } else {
            tarea.run();
        }
    }

    private EstadoTope calcular(TopeComprasRts p) {
        int anio = p.getAnio();
        BigDecimal tope = p.tope();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (Intervalo i : regimenService.intervalos(Regimen.SIMPLIFICADO, LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31))) {
            for (Object[] fila : cuentaRepository.montosParaTope(i.desde(), i.hasta(), EstadoCuentaPorPagar.ANULADA)) {
                acumulado = acumulado.add(ConversionMoneda.montoEnCrcReporte((BigDecimal) fila[0], (MonedaFE) fila[1], (BigDecimal) fila[2]));
            }
        }
        acumulado = acumulado.setScale(2, RoundingMode.HALF_UP);
        BigDecimal porcentaje = acumulado.multiply(CIEN).divide(tope, 2, RoundingMode.HALF_UP);
        BigDecimal enCien = acumulado.multiply(CIEN);
        NivelTope nivel = enCien.compareTo(p.getUmbralTope().multiply(tope)) >= 0 ? NivelTope.TOPE
            : enCien.compareTo(p.getUmbralAviso().multiply(tope)) >= 0 ? NivelTope.AVISO : NivelTope.BAJO;
        String mensaje = nivel == NivelTope.BAJO ? null
            : "Las compras del régimen simplificado de " + anio + " llegaron al " + formato(porcentaje) + " % del tope (₡"
            + formato(acumulado) + " de ₡" + formato(tope) + "). " + ADVERTENCIA_SOLO_ERP + ".";
        return new EstadoTope(anio, true, tope, acumulado, porcentaje, nivel, p.getUmbralAviso(), p.getUmbralTope(), mensaje);
    }

    private static String titulo(EstadoTope e) {
        BigDecimal umbral = e.nivel() == NivelTope.TOPE ? e.umbralTope() : e.umbralAviso();
        return "Tope de compras del RTS " + e.anio() + ": " + umbral.stripTrailingZeros().toPlainString() + " % alcanzado";
    }

    /** 1.234.567,89: separador de miles «.» y de decimales «,», como en las pantallas. */
    static String formato(BigDecimal valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", simbolos).format(valor);
    }

    // ── Validación ───────────────────────────────────────────────────────────

    private static Integer validarAnio(Integer anio) {
        if (anio == null || anio < ANIO_MINIMO || anio > ANIO_MAXIMO) {
            throw new IllegalArgumentException(MSG_ANIO);
        }
        return anio;
    }

    private static BigDecimal validarSalario(BigDecimal salario) {
        if (salario == null || salario.signum() <= 0 || salario.stripTrailingZeros().scale() > 2
            || salario.compareTo(MAXIMO_SALARIO) > 0) {
            throw new IllegalArgumentException(MSG_SALARIO);
        }
        return salario.setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Null es válido: queda el valor por defecto de la columna (al crear) o el actual (al corregir). */
    private static BigDecimal validarMultiplicador(BigDecimal multiplicador) {
        if (multiplicador == null) {
            return null;
        }
        if (multiplicador.signum() <= 0 || multiplicador.stripTrailingZeros().scale() > 2
            || multiplicador.compareTo(MAXIMO_MULTIPLICADOR) > 0) {
            throw new IllegalArgumentException(MSG_MULTIPLICADOR);
        }
        return multiplicador.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static void validarUmbrales(BigDecimal aviso, BigDecimal tope) {
        for (BigDecimal u : new BigDecimal[] {aviso, tope}) {
            if (u != null && (u.signum() <= 0 || u.compareTo(CIEN) > 0 || u.stripTrailingZeros().scale() > 2)) {
                throw new IllegalArgumentException(MSG_UMBRALES);
            }
        }
        if (aviso != null && tope != null && aviso.compareTo(tope) >= 0) {
            throw new IllegalArgumentException(MSG_UMBRALES);
        }
    }

    /** Con los valores ya resueltos (los explícitos o los por defecto de la columna). */
    private static void validarUmbralesFinales(BigDecimal aviso, BigDecimal tope) {
        if (aviso == null || tope == null || aviso.compareTo(tope) >= 0) {
            throw new IllegalArgumentException(MSG_UMBRALES);
        }
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String validarNota(String nota) {
        if (nota == null || nota.isBlank()) {
            return null;
        }
        String limpia = nota.trim();
        if (limpia.length() > LARGO_NOTA) {
            throw new IllegalArgumentException(MSG_NOTA);
        }
        return limpia;
    }

    private static Integer idDe(Usuario usuario) {
        return usuario != null ? usuario.getIdUsuario() : null;
    }

    private void auditar(Usuario usuario, String tipo, String descripcion, TopeComprasRts t) {
        if (usuario != null && usuario.getIdUsuario() != null) {
            actividadService.registrarActividad(usuario.getIdUsuario(), tipo, descripcion, "TopeComprasRts", t.getId().intValue());
        }
        log.info(descripcion);
    }
}
