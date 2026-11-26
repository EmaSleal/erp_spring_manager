package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.HoraFiscal;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Clave de un comprobante emitido fuera del ERP (T-057, ADR-076 §3-4). Desde que empieza el primer tramo SIMPLIFICADO el
 * ERP ya no emite las facturas GENERAL anteriores: se emiten en la herramienta de Hacienda y aquí se registra la clave de
 * 50 dígitos y la fecha de emisión de ese comprobante, para que la factura no figure como pendiente ni se emita dos veces.
 *
 * Errores, con mensaje fijo: {@link IllegalArgumentException} (400: clave o fecha inválidas), {@link NoSuchElementException}
 * (404: la factura no existe) e {@link IllegalStateException} (409: factura SIMPLIFICADO o anulada, con un comprobante del
 * ERP distinto de RECHAZADO, clave ya registrada en otra factura, o nada que quitar). Registrar, corregir y quitar dejan
 * auditoría con la clave anterior y la nueva. «Hoy» es el del Clock de la aplicación (America/Costa_Rica, ADR-060).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobanteExternoService {

    public static final String MSG_CLAVE = "La clave del comprobante debe tener exactamente 50 dígitos";
    public static final String MSG_FECHA = "Indique la fecha de emisión del comprobante";
    public static final String MSG_FECHA_FUTURA = "La fecha de emisión del comprobante no puede ser posterior a hoy";
    public static final String MSG_NO_EXISTE = "La factura no existe";
    public static final String MSG_SIMPLIFICADO =
        "Las ventas del régimen simplificado no tienen comprobante de Hacienda; no admiten una clave externa";
    public static final String MSG_ANULADA = "La factura está anulada; no admite una clave de comprobante externo";
    public static final String MSG_COMPROBANTE_ERP =
        "La factura ya tiene un comprobante generado por el sistema que no fue rechazado; no admite una clave externa";
    public static final String MSG_DUPLICADA = "Esa clave ya está registrada en otra factura";
    public static final String MSG_SIN_CLAVE = "La factura no tiene una clave de comprobante externo registrada";

    static final String ACTIVIDAD_REGISTRADO = "FACTURA_COMPROBANTE_EXTERNO_REGISTRADO";
    static final String ACTIVIDAD_CORREGIDO = "FACTURA_COMPROBANTE_EXTERNO_CORREGIDO";
    static final String ACTIVIDAD_ELIMINADO = "FACTURA_COMPROBANTE_EXTERNO_ELIMINADO";
    private static final String ENTIDAD = "Factura";
    private static final Pattern CINCUENTA_DIGITOS = Pattern.compile("\\d{50}");

    private final FacturaRepository facturaRepository;
    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final UsuarioActividadService actividadService;
    private final Clock clock;

    /** Registra la clave y la fecha, o corrige las que ya tenía (misma clave con otra fecha incluida). */
    @Transactional
    public Factura registrar(Integer idFactura, String clave, LocalDate fecha, Usuario usuario) {
        String limpia = clave == null ? null : clave.trim();
        if (limpia == null || !CINCUENTA_DIGITOS.matcher(limpia).matches()) {
            throw new IllegalArgumentException(MSG_CLAVE);
        }
        if (fecha == null) {
            throw new IllegalArgumentException(MSG_FECHA);
        }
        if (fecha.isAfter(HoraFiscal.ahora(clock).toLocalDate())) {
            throw new IllegalArgumentException(MSG_FECHA_FUTURA);
        }
        Factura factura = facturaRepository.findByIdParaActualizar(idFactura)
            .orElseThrow(() -> new NoSuchElementException(MSG_NO_EXISTE));
        if (factura.estaAnulada()) {
            throw new IllegalStateException(MSG_ANULADA);
        }
        if (factura.getRegimenEmisor() == Regimen.SIMPLIFICADO) {
            throw new IllegalStateException(MSG_SIMPLIFICADO);
        }
        if (comprobanteRepository.existsByFactura_IdFacturaAndEstadoNot(idFactura, EstadoComprobante.RECHAZADO)) {
            throw new IllegalStateException(MSG_COMPROBANTE_ERP);
        }
        if (facturaRepository.existsByComprobanteExternoClaveAndIdFacturaNot(limpia, idFactura)) {
            throw new IllegalStateException(MSG_DUPLICADA);
        }
        String anterior = factura.getComprobanteExternoClave();
        factura.setComprobanteExternoClave(limpia);
        factura.setComprobanteExternoFecha(fecha);
        Factura guardada;
        try {
            guardada = facturaRepository.saveAndFlush(factura);
        } catch (DataIntegrityViolationException e) {
            // otra transacción registró la misma clave entre la comprobación y el guardado (índice único)
            throw new IllegalStateException(MSG_DUPLICADA);
        }
        if (anterior == null) {
            auditar(usuario, ACTIVIDAD_REGISTRADO, "Clave de comprobante externo registrada en la factura "
                + guardada.getNumeroFactura() + ": " + limpia + " (fecha " + fecha + ")", idFactura);
        } else {
            auditar(usuario, ACTIVIDAD_CORREGIDO, "Clave de comprobante externo corregida en la factura "
                + guardada.getNumeroFactura() + ": de " + anterior + " a " + limpia + " (fecha " + fecha + ")", idFactura);
        }
        return guardada;
    }

    /** Quita la clave y la fecha; queda auditado con la clave que tenía. */
    @Transactional
    public Factura quitar(Integer idFactura, Usuario usuario) {
        Factura factura = facturaRepository.findByIdParaActualizar(idFactura)
            .orElseThrow(() -> new NoSuchElementException(MSG_NO_EXISTE));
        String anterior = factura.getComprobanteExternoClave();
        if (anterior == null) {
            throw new IllegalStateException(MSG_SIN_CLAVE);
        }
        factura.setComprobanteExternoClave(null);
        factura.setComprobanteExternoFecha(null);
        Factura guardada = facturaRepository.saveAndFlush(factura);
        auditar(usuario, ACTIVIDAD_ELIMINADO, "Clave de comprobante externo quitada de la factura "
            + guardada.getNumeroFactura() + ": " + anterior, idFactura);
        return guardada;
    }

    private void auditar(Usuario usuario, String tipo, String descripcion, Integer idFactura) {
        if (usuario != null && usuario.getIdUsuario() != null) {
            actividadService.registrarActividad(usuario.getIdUsuario(), tipo, descripcion, ENTIDAD, idFactura);
        }
        log.info(descripcion);
    }
}
