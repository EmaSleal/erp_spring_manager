package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.ProrrataIvaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.model.ProrrataIva;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ProrrataIvaRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Porcentaje provisional de prorrata del IVA por año (T-054, ADR-067 §4). Un registro por año, entre 0 y 100 con 2
 * decimales. Cada alta y cada corrección deja registro en la auditoría con el valor anterior y el nuevo.
 *
 * Corregir el porcentaje de un año NO recalcula las CxP ya registradas: cada línea de uso común guarda el porcentaje con el
 * que se creó, y la liquidación anual de la prorrata es un asiento manual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProrrataIvaService {

    public static final String MSG_ANIO = "El año debe estar entre 2000 y 2100";
    public static final String MSG_PORCENTAJE = "El porcentaje de prorrata debe estar entre 0 y 100";
    public static final String MSG_PORCENTAJE_DECIMALES = "El porcentaje de prorrata admite hasta 2 decimales";
    public static final String MSG_NOTA = "La nota no puede exceder 300 caracteres";

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = 2100;
    private static final int LARGO_NOTA = 300;

    private final ProrrataIvaRepository repository;
    private final UsuarioActividadService actividadService;

    /**
     * @throws IllegalArgumentException año, porcentaje o nota inválidos (400)
     * @throws IllegalStateException    ya hay un porcentaje para ese año (409)
     */
    @Transactional
    public ProrrataIva crear(ProrrataIvaDTO dto, Usuario usuario) {
        Integer anio = validarAnio(dto.getAnio());
        BigDecimal porcentaje = validarPorcentaje(dto.getPorcentaje());
        String nota = validarNota(dto.getNota());
        if (repository.findByAnio(anio).isPresent()) {
            throw new IllegalStateException(
                "Ya existe un porcentaje de prorrata para " + anio + "; corríjalo en lugar de crear otro");
        }
        ProrrataIva guardada = repository.saveAndFlush(ProrrataIva.builder().anio(anio).porcentaje(porcentaje).nota(nota)
            .usuarioId(idDe(usuario)).build());
        auditar(usuario, "PRORRATA_IVA_CREADA",
            "Prorrata del IVA de " + anio + " registrada: " + porcentaje.toPlainString() + " %", guardada);
        return guardada;
    }

    /**
     * Corrige el porcentaje y la nota del año; conserva la fecha de registro.
     *
     * @throws IllegalArgumentException porcentaje o nota inválidos (400)
     * @throws NoSuchElementException   no hay porcentaje para ese año (404)
     */
    @Transactional
    public ProrrataIva corregir(Integer anio, ProrrataIvaDTO dto, Usuario usuario) {
        validarAnio(anio);
        BigDecimal porcentaje = validarPorcentaje(dto.getPorcentaje());
        String nota = validarNota(dto.getNota());
        ProrrataIva existente = repository.findByAnio(anio)
            .orElseThrow(() -> new NoSuchElementException("No hay porcentaje de prorrata configurado para " + anio));
        BigDecimal anterior = existente.getPorcentaje();
        existente.setPorcentaje(porcentaje);
        existente.setNota(nota);
        existente.setUsuarioId(idDe(usuario));
        ProrrataIva guardada = repository.saveAndFlush(existente);
        auditar(usuario, "PRORRATA_IVA_CORREGIDA", "Prorrata del IVA de " + anio + " corregida de "
            + anterior.setScale(2, RoundingMode.HALF_UP).toPlainString() + " % a " + porcentaje.toPlainString() + " %", guardada);
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<ProrrataIva> listar() {
        return repository.findAllByOrderByAnioDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ProrrataIva> buscar(Integer anio) {
        return repository.findByAnio(anio);
    }

    private static Integer validarAnio(Integer anio) {
        if (anio == null || anio < ANIO_MINIMO || anio > ANIO_MAXIMO) {
            throw new IllegalArgumentException(MSG_ANIO);
        }
        return anio;
    }

    private static BigDecimal validarPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.signum() < 0 || porcentaje.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(MSG_PORCENTAJE);
        }
        if (porcentaje.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(MSG_PORCENTAJE_DECIMALES);
        }
        return porcentaje.setScale(2, RoundingMode.UNNECESSARY);
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

    private void auditar(Usuario usuario, String tipo, String descripcion, ProrrataIva p) {
        if (usuario != null && usuario.getIdUsuario() != null) {
            actividadService.registrarActividad(usuario.getIdUsuario(), tipo, descripcion, "ProrrataIva", p.getId().intValue());
        }
        log.info(descripcion);
    }
}
