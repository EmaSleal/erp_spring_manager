package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.exception.FacturaBloqueadaException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Único punto que decide si los campos fiscales de una factura se pueden modificar según el estado de su
 * comprobante electrónico vigente (ADR-030). Lo usan todas las rutas que modifican, anulan o eliminan una factura
 * (líneas, guardar, eliminar, anular) y el listener JPA de LineaFactura.
 *
 * <ul>
 *   <li>EDITABLE: sin comprobante vigente, o vigente en GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO, RECHAZADO o
 *       ANULADO. Al editar, el XML y la firma de un vigente que Hacienda no tiene se descartan (se regeneran al
 *       enviar, con la misma clave).</li>
 *   <li>BLOQUEADA: ENVIADO o PROCESANDO (esperar a Hacienda), ERROR_HACIENDA (revisar en ATV) y ACEPTADO (nota
 *       de crédito o débito).</li>
 *   <li>REQUIERE_VERIFICACION: ENVIO_INCIERTO (consultar el estado en Hacienda).</li>
 * </ul>
 *
 * <p>Acceso a datos, siempre dentro del contexto de persistencia de la transacción en curso:
 * <ul>
 *   <li>Lectura: primero los comprobantes de la factura ya gestionados por JPA en esta transacción (su estado en
 *       memoria puede no estar en la BD todavía); si no hay uno vigente gestionado, una proyección JPQL del vigente,
 *       sin cargar la entidad ni la factura. La consulta no provoca flush (FlushModeType.COMMIT) porque el listener
 *       de LineaFactura la invoca durante el flush de Hibernate; los cambios pendientes ya se tomaron de memoria.</li>
 *   <li>Descarte del XML y la firma: sobre la entidad gestionada (la misma instancia si ya estaba en el contexto),
 *       de modo que el flush escribe el descarte y nunca reescribe el XML previo. Se carga con la factura en
 *       join fetch para no dejar un proxy de Factura que el merge de FacturaServiceImpl.save devolvería.</li>
 * </ul>
 */
@Slf4j
@Component
public class PoliticaMutabilidadFactura {

    public enum Mutabilidad { EDITABLE, BLOQUEADA, REQUIERE_VERIFICACION }

    /**
     * Resultado de evaluar una factura.
     *
     * @param mutabilidad   si los campos fiscales se pueden modificar
     * @param estado        estado del comprobante vigente (null si no tiene)
     * @param claveNumerica clave del comprobante vigente (null si no tiene)
     * @param motivo        mensaje accionable cuando no es editable
     */
    public record Decision(Mutabilidad mutabilidad, EstadoComprobante estado, String claveNumerica, String motivo) {

        public boolean permite() {
            return mutabilidad == Mutabilidad.EDITABLE;
        }
    }

    /** Estados del vigente cuyo XML y firma se descartan al editar (Hacienda no tiene el documento). */
    private static final Set<EstadoComprobante> DESCARTAN_XML = EnumSet.of(
            EstadoComprobante.GENERADO, EstadoComprobante.FIRMADO, EstadoComprobante.ERROR,
            EstadoComprobante.NO_TRANSMITIDO);

    /** Comprobantes que se pueden borrar junto con su factura: nunca salieron hacia Hacienda. */
    private static final Set<EstadoComprobante> LOCALES = EnumSet.of(
            EstadoComprobante.GENERADO, EstadoComprobante.FIRMADO, EstadoComprobante.ERROR);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Proyección del comprobante vigente: lo que la política necesita, sin cargar la entidad. */
    public record Vigente(Long id, EstadoComprobante estado, String claveNumerica, LocalDateTime fechaEnvio,
                          Integer consultasErrorHacienda) {
    }

    /**
     * Estado FE de la factura para la UI (modal del listado, ADR-031): estado y clave del comprobante vigente y si
     * quedó señalado para revisarlo en ATV.
     */
    public record EstadoFe(Long idComprobante, EstadoComprobante estado, String claveNumerica, boolean revisarEnAtv) {
    }

    /** Consultas seguidas con ind-estado "error" tras las que un ERROR_HACIENDA queda señalado para revisar en ATV. */
    @Value("${facturacion.hacienda.max-consultas-error:"
            + ComprobanteElectronicoServiceImpl.MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO + "}")
    private int maxConsultasErrorHacienda = ComprobanteElectronicoServiceImpl.MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO;

    @PersistenceContext
    private EntityManager entityManager;

    private final ComprobanteElectronicoRepository comprobanteRepository;

    public PoliticaMutabilidadFactura(ComprobanteElectronicoRepository comprobanteRepository) {
        this.comprobanteRepository = comprobanteRepository;
    }

    /** Evalúa la factura según su comprobante vigente. */
    public Decision evaluar(Integer idFactura) {
        Vigente vigente = idFactura != null ? vigente(idFactura) : null;
        return vigente == null
                ? new Decision(Mutabilidad.EDITABLE, null, null, null)
                : decidir(vigente.estado(), vigente.claveNumerica(), vigente.fechaEnvio());
    }

    /**
     * Estado FE de la factura según su comprobante vigente (misma proyección que la política; sin cargar la entidad).
     *
     * @return null si la factura no tiene comprobante vigente
     */
    @Transactional(readOnly = true)
    public EstadoFe estadoFe(Integer idFactura) {
        Vigente vigente = idFactura != null ? vigente(idFactura) : null;
        if (vigente == null) {
            return null;
        }
        boolean revisarEnAtv = vigente.estado() == EstadoComprobante.ERROR_HACIENDA
                && vigente.consultasErrorHacienda() != null
                && vigente.consultasErrorHacienda() >= maxConsultasErrorHacienda;
        return new EstadoFe(vigente.id(), vigente.estado(), vigente.claveNumerica(), revisarEnAtv);
    }

    /**
     * Comprobante vigente de la factura tal como lo ve la transacción en curso: el gestionado en memoria si lo hay;
     * si no, la proyección JPQL (ignorando los gestionados que en memoria dejaron de ser vigentes).
     */
    private Vigente vigente(Integer idFactura) {
        List<ComprobanteElectronico> gestionados = gestionadosDeLaFactura(idFactura);
        ComprobanteElectronico gestionado = vigenteEnMemoria(gestionados);
        if (gestionado != null) {
            return new Vigente(gestionado.getId(), gestionado.getEstado(), gestionado.getClaveNumerica(),
                    gestionado.getFechaEnvio(), gestionado.getConsultasErrorHacienda());
        }
        Set<Long> idsGestionados = ids(gestionados);
        return entityManager.createQuery(
                        "SELECT new " + Vigente.class.getName() + "(c.id, c.estado, c.claveNumerica, c.fechaEnvio, "
                                + "c.consultasErrorHacienda) "
                                + "FROM ComprobanteElectronico c "
                                + "WHERE c.factura.idFactura = :idFactura AND c.vigente = true", Vigente.class)
                .setParameter("idFactura", idFactura)
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList().stream()
                .filter(v -> !idsGestionados.contains(v.id()))
                .findFirst()
                .orElse(null);
    }

    private static ComprobanteElectronico vigenteEnMemoria(List<ComprobanteElectronico> gestionados) {
        return gestionados.stream().filter(c -> Boolean.TRUE.equals(c.getVigente())).findFirst().orElse(null);
    }

    private static Set<Long> ids(List<ComprobanteElectronico> comprobantes) {
        Set<Long> ids = new HashSet<>();
        comprobantes.forEach(c -> ids.add(c.getId()));
        return ids;
    }

    /** Comprobantes de la factura presentes en el contexto de persistencia (sin cargar nada ni inicializar proxys). */
    private List<ComprobanteElectronico> gestionadosDeLaFactura(Integer idFactura) {
        List<ComprobanteElectronico> gestionados = new ArrayList<>();
        SessionImplementor session = entityManager.unwrap(SessionImplementor.class);
        for (Map.Entry<Object, EntityEntry> entry : session.getPersistenceContextInternal().reentrantSafeEntityEntries()) {
            if (entry.getKey() instanceof ComprobanteElectronico c
                    && entry.getValue().getStatus() != Status.DELETED
                    && entry.getValue().getStatus() != Status.GONE
                    && Objects.equals(idFactura, idFactura(c))) {
                gestionados.add(c);
            }
        }
        return gestionados;
    }

    private static Integer idFactura(ComprobanteElectronico comprobante) {
        Object factura = comprobante.getFactura();
        if (factura instanceof HibernateProxy proxy) {
            return (Integer) proxy.getHibernateLazyInitializer().getIdentifier();
        }
        return factura != null ? comprobante.getFactura().getIdFactura() : null;
    }

    /** Regla de ADR-030 §2 para el estado del comprobante vigente. */
    public static Decision decidir(EstadoComprobante estado, String clave, LocalDateTime fechaEnvio) {
        String comprobante = "La factura tiene el comprobante electrónico " + (clave != null ? clave + " " : "");
        return switch (estado) {
            case ACEPTADO -> new Decision(Mutabilidad.BLOQUEADA, estado, clave, comprobante
                    + "ACEPTADO por Hacienda: no se puede modificar. Emita una nota de crédito o débito.");
            case ENVIADO, PROCESANDO -> new Decision(Mutabilidad.BLOQUEADA, estado, clave, comprobante
                    + "en Hacienda (" + estado + "): espere la respuesta de Hacienda antes de modificarla.");
            case ERROR_HACIENDA -> new Decision(Mutabilidad.BLOQUEADA, estado, clave, comprobante
                    + "en Hacienda, que informó un error al procesarlo: revise el comprobante en ATV antes de "
                    + "modificarla.");
            case ENVIO_INCIERTO -> {
                LocalDateTime desde = ComprobanteElectronicoServiceImpl.disponibleParaVerificar(fechaEnvio);
                yield new Decision(Mutabilidad.REQUIERE_VERIFICACION, estado, clave, comprobante
                        + "con envío incierto: Hacienda pudo haberlo recibido. Consulte el estado en Hacienda antes "
                        + "de modificarla"
                        + (desde != null ? " (si Hacienda no lo tiene, quedará como no transmitido con una consulta "
                                + "a partir de las " + desde.format(HORA) + ")" : "")
                        + ".");
            }
            default -> new Decision(Mutabilidad.EDITABLE, estado, clave, null);
        };
    }

    /**
     * Exige que la factura se pueda modificar.
     *
     * @throws FacturaBloqueadaException (409) si el comprobante vigente lo impide
     */
    public Decision exigirEditable(Integer idFactura) {
        Decision decision = evaluar(idFactura);
        if (!decision.permite()) {
            log.warn("Factura {} bloqueada: {}", idFactura, decision.motivo());
            throw new FacturaBloqueadaException(decision);
        }
        return decision;
    }

    /**
     * Antes de modificar campos fiscales: exige que la factura sea editable y descarta el XML y la firma del
     * comprobante vigente que Hacienda no tiene (GENERADO y FIRMADO vuelven a GENERADO; ERROR y NO_TRANSMITIDO
     * conservan su estado). La clave no cambia. Un RECHAZADO no se toca (historial inmutable).
     */
    public void prepararEdicionFiscal(Integer idFactura) {
        Decision decision = exigirEditable(idFactura);
        if (idFactura == null || decision.estado() == null || !DESCARTAN_XML.contains(decision.estado())) {
            return;
        }
        ComprobanteElectronico comprobante = gestionadoOCargado(idFactura);
        if (comprobante == null || !DESCARTAN_XML.contains(comprobante.getEstado())) {
            return;
        }
        comprobante.setXmlComprobante(null);
        comprobante.setRutaArchivoXml(null);
        if (comprobante.getEstado() == EstadoComprobante.FIRMADO) {
            comprobante.setEstado(EstadoComprobante.GENERADO);
        }
        log.info("Factura {} editada: XML y firma del comprobante {} descartados (misma clave)",
                idFactura, comprobante.getClaveNumerica());
    }

    /**
     * El comprobante vigente como entidad gestionada: la instancia del contexto si ya estaba; si no, se carga con su
     * factura en join fetch (sin proxy de Factura en el contexto).
     */
    private ComprobanteElectronico gestionadoOCargado(Integer idFactura) {
        List<ComprobanteElectronico> gestionados = gestionadosDeLaFactura(idFactura);
        ComprobanteElectronico gestionado = vigenteEnMemoria(gestionados);
        if (gestionado != null) {
            return gestionado;
        }
        Set<Long> idsGestionados = ids(gestionados);
        return entityManager.createQuery("SELECT c FROM ComprobanteElectronico c JOIN FETCH c.factura "
                        + "WHERE c.factura.idFactura = :idFactura AND c.vigente = true", ComprobanteElectronico.class)
                .setParameter("idFactura", idFactura)
                .getResultList().stream()
                .filter(c -> !idsGestionados.contains(c.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Antes de eliminar la factura: exige que sea editable y que todos sus comprobantes sean locales (GENERADO,
     * FIRMADO o ERROR: nunca salieron hacia Hacienda). Un comprobante que pasó por Hacienda (incluido el historial
     * RECHAZADO o un NO_TRANSMITIDO) quedaría huérfano, así que la eliminación se rechaza.
     *
     * @return los comprobantes locales, que el llamador borra explícitamente antes que la factura (sin cascade)
     */
    public List<ComprobanteElectronico> exigirEliminable(Integer idFactura) {
        exigirEditable(idFactura);
        List<ComprobanteElectronico> comprobantes = comprobanteRepository.findByFactura_IdFacturaOrderByIdAsc(idFactura);
        comprobantes.stream()
                .filter(c -> !LOCALES.contains(c.getEstado()))
                .findFirst()
                .ifPresent(c -> {
                    throw new FacturaBloqueadaException(new Decision(Mutabilidad.BLOQUEADA, c.getEstado(),
                            c.getClaveNumerica(), "La factura tiene el comprobante electrónico "
                            + c.getClaveNumerica() + " en estado " + c.getEstado()
                            + ", que forma parte de su historial ante Hacienda: no se puede eliminar. Anúlela."));
                });
        return comprobantes;
    }
}
