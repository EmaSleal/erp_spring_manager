package api.astro.whats_orders_manager.modules.regimen.verificacion;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaRegimenDTO;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.regimen.aviso.NotificadorAdministradores;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Verificación del régimen contra /fe/ae (T-056, ADR-072). Consulta a Hacienda con la identificación de
 * configuracion_empresa, traduce {@code regimen.codigo} (1 GENERAL, 2 SIMPLIFICADO, otro OTRO) y lo compara con el régimen
 * vigente hoy. SOLO AVISA: nunca modifica regimen_tributario.
 *
 * Mapeo del resultado de la consulta: EXITOSA con código de régimen: COINCIDE o DIFIERE; NO_ENCONTRADA: NO_ENCONTRADO;
 * ERROR, una excepción, una respuesta EXITOSA sin el campo regimen, la empresa sin identificación o sin régimen vigente:
 * SIN_RESPUESTA (no hay nada que comparar).
 *
 * Avisos sin repetir: DIFIERE notifica a los administradores solo si la verificación anterior no era DIFIERE o difería con
 * otros valores (régimen del ERP o código de Hacienda). SIN_RESPUESTA notifica una sola vez, el día en que se completan N
 * días con verificación seguidos sin respuesta (N = regimen.verificacion.dias-sin-respuesta-aviso; varias el mismo día
 * cuentan como un día). El panel muestra el aviso mientras la última verificación lo justifique.
 *
 * Se conserva la última verificación; las de más de regimen.verificacion.retencion-dias días se borran en cada ejecución.
 */
@Slf4j
@Service
public class VerificacionRegimenService {

    private static final int LARGO_DESCRIPCION = 200;
    private static final int LARGO_DETALLE = 300;

    private final HaciendaApiService hacienda;
    private final ConfiguracionEmpresaRepository empresaRepository;
    private final RegimenTributarioService regimenService;
    private final VerificacionRegimenRepository repository;
    private final NotificadorAdministradores notificador;
    private final Clock clock;
    private final int diasSinRespuestaAviso;
    private final int retencionDias;

    public VerificacionRegimenService(HaciendaApiService hacienda, ConfiguracionEmpresaRepository empresaRepository,
                                      RegimenTributarioService regimenService, VerificacionRegimenRepository repository,
                                      NotificadorAdministradores notificador, Clock clock,
                                      @Value("${regimen.verificacion.dias-sin-respuesta-aviso:3}") int diasSinRespuestaAviso,
                                      @Value("${regimen.verificacion.retencion-dias:90}") int retencionDias) {
        this.hacienda = hacienda;
        this.empresaRepository = empresaRepository;
        this.regimenService = regimenService;
        this.repository = repository;
        this.notificador = notificador;
        this.clock = clock;
        this.diasSinRespuestaAviso = diasSinRespuestaAviso;
        this.retencionDias = retencionDias;
    }

    /** 1 GENERAL, 2 SIMPLIFICADO; cualquier otro código (3 no contribuyente, desconocidos) o ausente: OTRO. */
    public static String traducir(Integer codigo) {
        if (codigo != null && codigo == 1) {
            return "GENERAL";
        }
        if (codigo != null && codigo == 2) {
            return "SIMPLIFICADO";
        }
        return "OTRO";
    }

    // ── Verificar ────────────────────────────────────────────────────────────

    /** Verificación del job diario. */
    @Transactional
    public VerificacionRegimen verificar() {
        return verificar(null);
    }

    /** Verificación pedida a mano por {@code idUsuario}. */
    @Transactional
    public VerificacionRegimen verificar(Integer idUsuario) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        VerificacionRegimen.VerificacionRegimenBuilder b = VerificacionRegimen.builder().fechaHora(ahora).usuarioId(idUsuario);

        Regimen local = regimenLocal(LocalDate.now(clock));
        b.regimenLocal(local == null ? null : local.name());
        String identificacion = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
            .map(ConfiguracionEmpresa::getNumeroIdentificacion).orElse(null);

        if (local == null) {
            b.resultado(ResultadoVerificacion.SIN_RESPUESTA).detalle("No hay un tramo de régimen vigente hoy en el ERP");
        } else if (identificacion == null || identificacion.isBlank()) {
            b.resultado(ResultadoVerificacion.SIN_RESPUESTA).detalle("La empresa no tiene número de identificación configurado");
        } else {
            consultar(b, identificacion, local);
        }

        VerificacionRegimen anteriorUltima = repository.findFirstByOrderByFechaHoraDescIdDesc().orElse(null);
        VerificacionRegimen guardada = repository.saveAndFlush(b.build());
        avisar(guardada, anteriorUltima);
        purgarAntiguas();
        return guardada;
    }

    private void consultar(VerificacionRegimen.VerificacionRegimenBuilder b, String identificacion, Regimen local) {
        HaciendaConsultaDTO respuesta;
        try {
            respuesta = hacienda.consultarContribuyente(identificacion);
        } catch (RuntimeException e) {
            log.warn("Verificación del régimen: la consulta a Hacienda lanzó {}", e.getClass().getSimpleName());
            b.resultado(ResultadoVerificacion.SIN_RESPUESTA).detalle(recortar("Error al consultar a Hacienda: " + e.getMessage(), LARGO_DETALLE));
            return;
        }
        if (respuesta == null || respuesta.getResultado() == null || respuesta.getResultado() == HaciendaConsultaDTO.ResultadoConsulta.ERROR) {
            b.resultado(ResultadoVerificacion.SIN_RESPUESTA)
                .detalle(recortar(respuesta == null || respuesta.getMensajeError() == null ? "Hacienda no respondió" : respuesta.getMensajeError(), LARGO_DETALLE));
            return;
        }
        if (respuesta.getResultado() == HaciendaConsultaDTO.ResultadoConsulta.NO_ENCONTRADA) {
            b.resultado(ResultadoVerificacion.NO_ENCONTRADO).detalle("Hacienda no encontró la identificación de la empresa");
            return;
        }
        HaciendaRegimenDTO regimen = respuesta.getRegimen();
        if (regimen == null || regimen.getCodigo() == null) {
            b.resultado(ResultadoVerificacion.SIN_RESPUESTA).detalle("La respuesta de Hacienda no trae el régimen del contribuyente");
            return;
        }
        String deHacienda = traducir(regimen.getCodigo());
        b.codigoHacienda(regimen.getCodigo()).descripcionHacienda(recortar(regimen.getDescripcion(), LARGO_DESCRIPCION))
            .regimenHacienda(deHacienda)
            .resultado(local.name().equals(deHacienda) ? ResultadoVerificacion.COINCIDE : ResultadoVerificacion.DIFIERE);
    }

    private Regimen regimenLocal(LocalDate hoy) {
        try {
            return regimenService.vigenteEn(hoy).getRegimen();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ── Avisos ───────────────────────────────────────────────────────────────

    private void avisar(VerificacionRegimen actual, VerificacionRegimen anterior) {
        if (actual.getResultado() == ResultadoVerificacion.DIFIERE) {
            boolean igual = anterior != null && anterior.getResultado() == ResultadoVerificacion.DIFIERE
                && java.util.Objects.equals(anterior.getRegimenLocal(), actual.getRegimenLocal())
                && java.util.Objects.equals(anterior.getCodigoHacienda(), actual.getCodigoHacienda());
            if (!igual) {
                notificador.notificar("El régimen en Hacienda difiere del ERP", textoDifiere(actual));
            }
        } else if (actual.getResultado() == ResultadoVerificacion.SIN_RESPUESTA) {
            int dias = diasSinRespuestaSeguidos();
            if (dias == diasSinRespuestaAviso && !yaSeAvisoElDia(actual, anterior)) {
                notificador.notificar("Sin respuesta de Hacienda para verificar el régimen", textoSinRespuesta(dias));
            }
        }
    }

    /** Un segundo SIN_RESPUESTA del mismo día no vuelve a avisar: solo cuenta el que completó los N días. */
    private boolean yaSeAvisoElDia(VerificacionRegimen actual, VerificacionRegimen anterior) {
        return anterior != null && anterior.getResultado() == ResultadoVerificacion.SIN_RESPUESTA
            && anterior.getFechaHora().toLocalDate().equals(actual.getFechaHora().toLocalDate());
    }

    /** Días distintos, contados desde la última verificación hacia atrás, mientras todas sean SIN_RESPUESTA. */
    private int diasSinRespuestaSeguidos() {
        Set<LocalDate> dias = new LinkedHashSet<>();
        for (VerificacionRegimen v : repository.findTop60ByOrderByFechaHoraDescIdDesc()) {
            if (v.getResultado() != ResultadoVerificacion.SIN_RESPUESTA) {
                break;
            }
            dias.add(v.getFechaHora().toLocalDate());
        }
        return dias.size();
    }

    private String textoDifiere(VerificacionRegimen v) {
        StringBuilder sb = new StringBuilder("Hacienda reporta ").append(etiqueta(v.getRegimenHacienda())).append(" (código ")
            .append(v.getCodigoHacienda());
        if (v.getDescripcionHacienda() != null && !v.getDescripcionHacienda().isBlank()) {
            sb.append(": ").append(v.getDescripcionHacienda());
        }
        sb.append(") pero el ERP tiene ").append(etiqueta(v.getRegimenLocal())).append(" vigente hoy.");
        regimenService.proximoTramo().ifPresent(t -> sb.append(" Próximo tramo: ").append(etiqueta(t.getRegimen().name()))
            .append(" desde ").append(t.getVigenteDesde()).append('.'));
        sb.append(" El ERP no cambió el régimen: revíselo en Configuración > Régimen tributario.");
        return sb.toString();
    }

    private String textoSinRespuesta(int dias) {
        return "Hacienda no respondió la verificación del régimen en " + dias + " días seguidos.";
    }

    private static String etiqueta(String regimen) {
        if (regimen == null) {
            return "sin régimen";
        }
        return switch (regimen) {
            case "GENERAL" -> "Régimen General";
            case "SIMPLIFICADO" -> "Régimen Simplificado";
            default -> "otro régimen";
        };
    }

    // ── Lectura para pantalla y panel ────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<VerificacionRegimen> ultima() {
        return repository.findFirstByOrderByFechaHoraDescIdDesc();
    }

    /**
     * Aviso del panel de Facturación según la última verificación: DIFIERE (los dos valores y el próximo tramo si existe) o
     * SIN_RESPUESTA repetida N días seguidos; vacío en los demás casos.
     */
    @Transactional(readOnly = true)
    public Optional<String> avisoPanel() {
        Optional<VerificacionRegimen> ultima = repository.findFirstByOrderByFechaHoraDescIdDesc();
        if (ultima.isEmpty()) {
            return Optional.empty();
        }
        VerificacionRegimen v = ultima.get();
        if (v.getResultado() == ResultadoVerificacion.DIFIERE) {
            return Optional.of(textoDifiere(v));
        }
        if (v.getResultado() == ResultadoVerificacion.SIN_RESPUESTA) {
            int dias = diasSinRespuestaSeguidos();
            if (dias >= diasSinRespuestaAviso) {
                return Optional.of(textoSinRespuesta(dias));
            }
        }
        return Optional.empty();
    }

    // ── Retención ────────────────────────────────────────────────────────────

    /** Borra las verificaciones de más de {@code retencion-dias} días; la última siempre se conserva. */
    @Transactional
    public void purgarAntiguas() {
        repository.findFirstByOrderByFechaHoraDescIdDesc().ifPresent(ultima -> {
            LocalDateTime corte = LocalDateTime.now(clock).minusDays(retencionDias);
            int borradas = repository.purgarAnterioresA(corte, ultima.getId());
            if (borradas > 0) {
                log.info("Verificaciones de régimen borradas por retención: {}", borradas);
            }
        });
    }

    private static String recortar(String texto, int largo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= largo ? texto : texto.substring(0, largo);
    }
}
