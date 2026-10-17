package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.exception.FacturaBloqueadaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
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
 *   <li>BLOQUEADA: ENVIADO o PROCESANDO (esperar a Hacienda) y ACEPTADO (nota de crédito o débito).</li>
 *   <li>REQUIERE_VERIFICACION: ENVIO_INCIERTO (verificar con Hacienda).</li>
 * </ul>
 *
 * El estado se lee con JDBC, no con JPA: el listener de LineaFactura la invoca durante el flush de Hibernate, donde
 * no se deben ejecutar consultas JPA. JdbcTemplate usa la conexión de la transacción en curso.
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

    private final JdbcTemplate jdbc;
    private final ComprobanteElectronicoRepository comprobanteRepository;

    public PoliticaMutabilidadFactura(JdbcTemplate jdbc, ComprobanteElectronicoRepository comprobanteRepository) {
        this.jdbc = jdbc;
        this.comprobanteRepository = comprobanteRepository;
    }

    /** Evalúa la factura según su comprobante vigente. */
    public Decision evaluar(Integer idFactura) {
        if (idFactura == null) {
            return new Decision(Mutabilidad.EDITABLE, null, null, null);
        }
        List<Decision> decisiones = jdbc.query(
                "SELECT estado, clave_numerica, fecha_envio FROM comprobante_electronico "
                        + "WHERE factura_id = ? AND vigente = TRUE",
                (rs, i) -> {
                    Timestamp envio = rs.getTimestamp(3);
                    return decidir(EstadoComprobante.valueOf(rs.getString(1)), rs.getString(2),
                            envio != null ? envio.toLocalDateTime() : null);
                },
                idFactura);
        return decisiones.isEmpty() ? new Decision(Mutabilidad.EDITABLE, null, null, null) : decisiones.get(0);
    }

    /** Regla de ADR-030 §2 para el estado del comprobante vigente. */
    public static Decision decidir(EstadoComprobante estado, String clave, LocalDateTime fechaEnvio) {
        String comprobante = "La factura tiene el comprobante electrónico " + (clave != null ? clave + " " : "");
        return switch (estado) {
            case ACEPTADO -> new Decision(Mutabilidad.BLOQUEADA, estado, clave, comprobante
                    + "ACEPTADO por Hacienda: no se puede modificar. Emita una nota de crédito o débito.");
            case ENVIADO, PROCESANDO -> new Decision(Mutabilidad.BLOQUEADA, estado, clave, comprobante
                    + "en Hacienda (" + estado + "): espere la respuesta de Hacienda antes de modificarla.");
            case ENVIO_INCIERTO -> {
                LocalDateTime desde = ComprobanteElectronicoServiceImpl.disponibleParaVerificar(fechaEnvio);
                yield new Decision(Mutabilidad.REQUIERE_VERIFICACION, estado, clave, comprobante
                        + "con envío incierto: Hacienda pudo haberlo recibido. Verifique el estado con Hacienda"
                        + (desde != null ? " (disponible a partir de las " + desde.format(HORA) + ")" : "")
                        + " antes de modificarla.");
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
        // JDBC, no JPA: cargar el comprobante dejaría un proxy de la factura en el contexto de persistencia y el
        // merge de FacturaServiceImpl.save devolvería ese proxy en lugar de la entidad.
        int filas = jdbc.update("UPDATE comprobante_electronico SET xml_comprobante = NULL, ruta_archivo_xml = NULL, "
                        + "estado = CASE WHEN estado = 'FIRMADO' THEN 'GENERADO' ELSE estado END, updated_at = ? "
                        + "WHERE factura_id = ? AND vigente = TRUE "
                        + "AND estado IN ('GENERADO', 'FIRMADO', 'ERROR', 'NO_TRANSMITIDO')",
                Timestamp.valueOf(LocalDateTime.now()), idFactura);
        if (filas > 0) {
            log.info("Factura {} editada: XML y firma del comprobante {} descartados (misma clave)",
                    idFactura, decision.claveNumerica());
        }
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
