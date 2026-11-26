package api.astro.whats_orders_manager.modules.regimen.service;

import api.astro.whats_orders_manager.modules.regimen.dto.RegimenTributarioDTO;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Régimen tributario con vigencia (T-056, ADR-072). Un tramo rige desde su {@code vigente_desde} hasta el día anterior al
 * del siguiente: {@link #vigenteEn(LocalDate)} devuelve el de mayor inicio menor o igual a la fecha y lanza
 * {@link RegimenNoDefinidoException} si no hay ninguno (no existe régimen por defecto).
 *
 * Reglas de escritura (errores: IllegalArgumentException -> 400, IllegalStateException -> 409, NoSuchElementException ->
 * 404, con mensaje fijo):
 * <ul>
 *   <li>un tramo cuyo inicio es hoy o anterior no se edita ni se borra;</li>
 *   <li>un tramo se crea (con inicio futuro, hoy o anterior), edita o borra solo si no hay facturas, notas ni CxP con
 *       fecha igual o posterior al inicio (en una edición, al menor entre el inicio anterior y el nuevo); al crear, el
 *       mensaje del 409 dice cuántos documentos hay;</li>
 *   <li>dos tramos seguidos no pueden tener el mismo régimen (al crear, editar o borrar);</li>
 *   <li>las fechas de inicio no se repiten.</li>
 * </ul>
 * «Hoy» es el del Clock de la aplicación (America/Costa_Rica, ADR-060). Cada escritura deja auditoría. Los tramos se
 * guardan en una caché que se invalida en cada escritura (y otra vez al confirmarse la transacción).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegimenTributarioService {

    public static final String MSG_REGIMEN = "Indique el régimen (GENERAL o SIMPLIFICADO)";
    public static final String MSG_FECHA = "Indique la fecha de inicio del tramo";
    public static final String MSG_NOTA = "La nota no puede exceder 300 caracteres";
    /** Al editar, el nuevo inicio de un tramo futuro debe seguir siendo posterior a hoy. */
    public static final String MSG_PASADO_CREAR = "El inicio de un tramo debe ser posterior a hoy";
    public static final String MSG_PASADO_EDITAR = "Un tramo que ya empezó no se edita ni se borra";
    public static final String MSG_PASADO_ELIMINAR = MSG_PASADO_EDITAR;
    public static final String MSG_FECHA_REPETIDA = "Ya existe un tramo con esa fecha de inicio";
    public static final String MSG_CONSECUTIVOS = "Dos tramos seguidos no pueden tener el mismo régimen";
    public static final String MSG_DOCUMENTOS =
        "Hay facturas, notas de crédito o cuentas por pagar con fecha igual o posterior al inicio del tramo";
    /** Al crear: dice que ya hay documentos desde la fecha y cuántos ({@code %s} fecha ISO, {@code %d} cantidad). */
    public static final String MSG_DOCUMENTOS_CANTIDAD =
        "Ya existen documentos (facturas, notas de crédito o cuentas por pagar) con fecha igual o posterior al %s: %d en total";
    public static final String MSG_NO_EXISTE = "El tramo no existe";

    private static final int LARGO_NOTA = 300;
    private static final String ENTIDAD = "RegimenTributario";

    private final RegimenTributarioRepository repository;
    private final ConsultaDocumentosPorFecha documentos;
    private final UsuarioActividadService actividadService;
    private final Clock clock;

    private volatile List<RegimenTributario> cache;

    /** Un tramo del historial con su estado respecto de hoy. {@code bloqueo} es null si es editable. */
    public record TramoEstado(RegimenTributario tramo, boolean vigente, boolean proximo, boolean editable, String bloqueo) {
    }

    /** Rango de fechas cerrado (ambos extremos incluidos). */
    public record Intervalo(LocalDate desde, LocalDate hasta) {
    }

    private record Punto(LocalDate desde, Regimen regimen) {
    }

    // ── Consulta ─────────────────────────────────────────────────────────────

    /**
     * Régimen vigente en la fecha de un documento: el tramo con el mayor {@code vigente_desde} menor o igual a la fecha.
     *
     * @throws RegimenNoDefinidoException ningún tramo empieza en esa fecha o antes
     * @throws IllegalArgumentException   fecha nula
     */
    public RegimenTributario vigenteEn(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha del documento es obligatoria para conocer su régimen");
        }
        RegimenTributario vigente = null;
        for (RegimenTributario t : tramos()) {
            if (t.getVigenteDesde().isAfter(fecha)) {
                break;
            }
            vigente = t;
        }
        if (vigente == null) {
            throw new RegimenNoDefinidoException(fecha);
        }
        return vigente;
    }

    /** Tramo que rige hoy (fecha del Clock). */
    public RegimenTributario vigenteHoy() {
        return vigenteEn(hoy());
    }

    /** El primer tramo que empieza después de hoy (fecha del Clock), si hay alguno. */
    public java.util.Optional<RegimenTributario> proximoTramo() {
        LocalDate hoy = hoy();
        return tramos().stream().filter(t -> t.getVigenteDesde().isAfter(hoy)).findFirst();
    }

    /**
     * Si empieza algún tramo de {@code regimen} con inicio posterior a {@code despuesDe} y no posterior a {@code hasta}
     * (T-057, ADR-076 §2: «desde el primer tramo SIMPLIFICADO posterior al régimen de la factura»). Mira el inicio de los
     * tramos, no los días que cubren: un tramo que ya regía en {@code despuesDe} no cuenta.
     */
    public boolean empiezaTramo(Regimen regimen, LocalDate despuesDe, LocalDate hasta) {
        return tramos().stream().anyMatch(t -> t.getRegimen() == regimen && t.getVigenteDesde().isAfter(despuesDe)
            && !t.getVigenteDesde().isAfter(hasta));
    }

    /**
     * Los días de {@code [desde, hasta]} en que rige {@code regimen}, como rangos cerrados disjuntos y en orden. Cada tramo
     * rige hasta el día anterior al inicio del siguiente; un rango anterior al primer tramo no devuelve nada.
     */
    public List<Intervalo> intervalos(Regimen regimen, LocalDate desde, LocalDate hasta) {
        List<RegimenTributario> lista = tramos();
        List<Intervalo> res = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            RegimenTributario t = lista.get(i);
            if (t.getRegimen() != regimen) {
                continue;
            }
            LocalDate inicio = t.getVigenteDesde().isAfter(desde) ? t.getVigenteDesde() : desde;
            LocalDate fin = i + 1 < lista.size() ? lista.get(i + 1).getVigenteDesde().minusDays(1) : hasta;
            if (fin.isAfter(hasta)) {
                fin = hasta;
            }
            if (!inicio.isAfter(fin)) {
                res.add(new Intervalo(inicio, fin));
            }
        }
        return res;
    }

    /** Historial completo, del más antiguo al más reciente, con el vigente hoy, el próximo y si cada tramo se puede tocar. */
    @Transactional(readOnly = true)
    public List<TramoEstado> historial() {
        LocalDate hoy = hoy();
        List<RegimenTributario> todos = repository.findAllByOrderByVigenteDesdeAsc();
        Long idVigente = todos.stream().filter(t -> !t.getVigenteDesde().isAfter(hoy)).reduce((a, b) -> b)
            .map(RegimenTributario::getId).orElse(null);
        Long idProximo = todos.stream().filter(t -> t.getVigenteDesde().isAfter(hoy)).findFirst()
            .map(RegimenTributario::getId).orElse(null);
        List<TramoEstado> res = new ArrayList<>();
        for (RegimenTributario t : todos) {
            String bloqueo = null;
            if (!t.getVigenteDesde().isAfter(hoy)) {
                bloqueo = MSG_PASADO_EDITAR;
            } else if (documentos.existenDesde(t.getVigenteDesde())) {
                bloqueo = MSG_DOCUMENTOS;
            }
            res.add(new TramoEstado(t, t.getId().equals(idVigente), t.getId().equals(idProximo), bloqueo == null, bloqueo));
        }
        return res;
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    /**
     * @throws IllegalArgumentException régimen, fecha o nota inválidos (400)
     * @throws IllegalStateException    fecha repetida, documentos con fecha igual o posterior al inicio (también si el inicio
     *                                  es hoy o anterior) o dos tramos seguidos con el mismo régimen (409)
     */
    @Transactional
    public RegimenTributario crear(RegimenTributarioDTO dto, Usuario usuario) {
        Regimen regimen = validarRegimen(dto.getRegimen());
        LocalDate desde = validarFecha(dto.getVigenteDesde());
        String nota = validarNota(dto.getNota());
        List<RegimenTributario> actuales = repository.findAllByOrderByVigenteDesdeAsc();
        validarFechaLibre(actuales, desde, null);
        validarSinDocumentosAlCrear(desde);
        validarSecuencia(actuales, null, new Punto(desde, regimen));

        RegimenTributario guardado = repository.saveAndFlush(RegimenTributario.builder().regimen(regimen).vigenteDesde(desde)
            .nota(nota).usuarioId(idDe(usuario)).fechaRegistro(LocalDateTime.now(clock)).build());
        invalidarCache();
        auditar(usuario, "REGIMEN_TRIBUTARIO_CREADO", "Tramo de régimen " + regimen + " desde " + desde + " registrado", guardado);
        return guardado;
    }

    /**
     * Corrige el régimen, el inicio y la nota de un tramo futuro; conserva la fecha de registro.
     *
     * @throws IllegalArgumentException datos inválidos (400)
     * @throws NoSuchElementException   el tramo no existe (404)
     * @throws IllegalStateException    el tramo ya empezó, o se viola alguna regla del tramo (409)
     */
    @Transactional
    public RegimenTributario corregir(Long id, RegimenTributarioDTO dto, Usuario usuario) {
        Regimen regimen = validarRegimen(dto.getRegimen());
        LocalDate desde = validarFecha(dto.getVigenteDesde());
        String nota = validarNota(dto.getNota());
        RegimenTributario existente = repository.findById(id).orElseThrow(() -> new NoSuchElementException(MSG_NO_EXISTE));
        if (!existente.getVigenteDesde().isAfter(hoy())) {
            throw new IllegalStateException(MSG_PASADO_EDITAR);
        }
        validarFutura(desde);
        List<RegimenTributario> actuales = repository.findAllByOrderByVigenteDesdeAsc();
        validarFechaLibre(actuales, desde, id);
        LocalDate anterior = existente.getVigenteDesde();
        validarSinDocumentos(desde.isBefore(anterior) ? desde : anterior);
        validarSecuencia(actuales, id, new Punto(desde, regimen));

        Regimen regimenAnterior = existente.getRegimen();
        existente.setRegimen(regimen);
        existente.setVigenteDesde(desde);
        existente.setNota(nota);
        existente.setUsuarioId(idDe(usuario));
        RegimenTributario guardado = repository.saveAndFlush(existente);
        invalidarCache();
        auditar(usuario, "REGIMEN_TRIBUTARIO_CORREGIDO", "Tramo de régimen corregido de " + regimenAnterior + " desde " + anterior
            + " a " + regimen + " desde " + desde, guardado);
        return guardado;
    }

    /**
     * @throws NoSuchElementException el tramo no existe (404)
     * @throws IllegalStateException  el tramo ya empezó, tiene documentos, o dejaría dos tramos seguidos con el mismo
     *                                régimen (409)
     */
    @Transactional
    public void eliminar(Long id, Usuario usuario) {
        RegimenTributario existente = repository.findById(id).orElseThrow(() -> new NoSuchElementException(MSG_NO_EXISTE));
        if (!existente.getVigenteDesde().isAfter(hoy())) {
            throw new IllegalStateException(MSG_PASADO_ELIMINAR);
        }
        validarSinDocumentos(existente.getVigenteDesde());
        validarSecuencia(repository.findAllByOrderByVigenteDesdeAsc(), id, null);

        repository.delete(existente);
        repository.flush();
        invalidarCache();
        auditar(usuario, "REGIMEN_TRIBUTARIO_ELIMINADO",
            "Tramo de régimen " + existente.getRegimen() + " desde " + existente.getVigenteDesde() + " eliminado", existente);
    }

    // ── Caché ────────────────────────────────────────────────────────────────

    /**
     * Vacía la caché. Dentro de una transacción también se vacía al confirmarse, para que una lectura concurrente que
     * recargó antes del commit no deje tramos viejos.
     */
    public void invalidarCache() {
        cache = null;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache = null;
                }
            });
        }
    }

    private List<RegimenTributario> tramos() {
        List<RegimenTributario> actual = cache;
        if (actual == null) {
            actual = List.copyOf(repository.findAllByOrderByVigenteDesdeAsc());
            cache = actual;
        }
        return actual;
    }

    // ── Reglas ───────────────────────────────────────────────────────────────

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }

    private static Regimen validarRegimen(Regimen regimen) {
        if (regimen == null) {
            throw new IllegalArgumentException(MSG_REGIMEN);
        }
        return regimen;
    }

    private static LocalDate validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException(MSG_FECHA);
        }
        return fecha;
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

    private void validarFutura(LocalDate desde) {
        if (!desde.isAfter(hoy())) {
            throw new IllegalStateException(MSG_PASADO_CREAR);
        }
    }

    private static void validarFechaLibre(List<RegimenTributario> actuales, LocalDate desde, Long idPropio) {
        boolean repetida = actuales.stream().anyMatch(t -> t.getVigenteDesde().equals(desde) && !t.getId().equals(idPropio));
        if (repetida) {
            throw new IllegalStateException(MSG_FECHA_REPETIDA);
        }
    }

    private void validarSinDocumentos(LocalDate desde) {
        if (documentos.existenDesde(desde)) {
            throw new IllegalStateException(MSG_DOCUMENTOS);
        }
    }

    private void validarSinDocumentosAlCrear(LocalDate desde) {
        long cantidad = documentos.contarDesde(desde);
        if (cantidad > 0) {
            throw new IllegalStateException(String.format(MSG_DOCUMENTOS_CANTIDAD, desde, cantidad));
        }
    }

    /** Quita el tramo {@code idQuitado} (si hay), agrega {@code nuevo} (si hay) y exige que ningún par seguido repita régimen. */
    private static void validarSecuencia(List<RegimenTributario> actuales, Long idQuitado, Punto nuevo) {
        List<Punto> puntos = new ArrayList<>();
        for (RegimenTributario t : actuales) {
            if (!t.getId().equals(idQuitado)) {
                puntos.add(new Punto(t.getVigenteDesde(), t.getRegimen()));
            }
        }
        if (nuevo != null) {
            puntos.add(nuevo);
        }
        puntos.sort(Comparator.comparing(Punto::desde));
        for (int i = 1; i < puntos.size(); i++) {
            if (puntos.get(i).regimen() == puntos.get(i - 1).regimen()) {
                throw new IllegalStateException(MSG_CONSECUTIVOS);
            }
        }
    }

    private static Integer idDe(Usuario usuario) {
        return usuario != null ? usuario.getIdUsuario() : null;
    }

    private void auditar(Usuario usuario, String tipo, String descripcion, RegimenTributario t) {
        if (usuario != null && usuario.getIdUsuario() != null) {
            actividadService.registrarActividad(usuario.getIdUsuario(), tipo, descripcion, ENTIDAD, t.getId().intValue());
        }
        log.info(descripcion);
    }
}
