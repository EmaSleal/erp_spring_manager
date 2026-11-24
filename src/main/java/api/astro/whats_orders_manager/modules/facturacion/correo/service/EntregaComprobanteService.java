package api.astro.whats_orders_manager.modules.facturacion.correo.service;

import api.astro.whats_orders_manager.modules.facturacion.correo.config.CorreoProperties;
import api.astro.whats_orders_manager.modules.facturacion.correo.dto.CorreoDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.correo.dto.EntregaDTO;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.EntregaComprobante;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.EstadoEntrega;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.OrigenEntrega;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.TipoDocumentoEntrega;
import api.astro.whats_orders_manager.modules.facturacion.correo.repository.EntregaComprobanteRepository;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * T-055 (ADR-070): registro de entregas de comprobantes por correo. La entrega AUTOMATICO se crea en la misma transacción
 * en que el comprobante pasa a ACEPTADO ({@link #registrarAutomatica}); el reenvío manual la crea a pedido de un usuario.
 * Aquí se decide el destino: el correo del receptor, o la regla de SANDBOX (nunca al receptor real).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntregaComprobanteService {

    public static final String PERMISO_REENVIAR = "COMPROBANTE_REENVIAR_CORREO";
    public static final String ACTIVIDAD_REENVIO = "COMPROBANTE_REENVIO_CORREO";

    /** Formato práctico de correo (una dirección, con punto en el dominio); rechaza listas, espacios y nombres con &lt; &gt;. */
    private static final Pattern CORREO = Pattern.compile(
        "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");

    private final EntregaComprobanteRepository repository;
    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final CorreoProperties props;
    private final Clock clock;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioActividadService actividadService;
    private final PermisoService permisoService;

    /** Receptor del comprobante: el cliente de la factura (o de la factura de la nota). */
    public static Cliente receptorDe(ComprobanteElectronico c) {
        if (c.getFactura() != null) {
            return c.getFactura().getCliente();
        }
        if (c.getNotaFactura() != null && c.getNotaFactura().getFactura() != null) {
            return c.getNotaFactura().getFactura().getCliente();
        }
        return null;
    }

    public static boolean esCorreoValido(String correo) {
        return correo != null && correo.length() <= 320 && CORREO.matcher(correo).matches();
    }

    /** Un comprobante de SANDBOX (o sin ambiente, por seguridad) nunca se envía al receptor real. */
    public static boolean esSandbox(ComprobanteElectronico c) {
        return c.getAmbiente() != AmbienteHacienda.PRODUCCION;
    }

    /**
     * Crea la entrega AUTOMATICO del comprobante recién ACEPTADO, en la transacción del cambio de estado (MANDATORY: sin
     * transacción abierta falla). Idempotente: si ya existe una AUTOMATICO para el comprobante no hace nada (el índice único
     * es la última defensa). Se registra también con el envío deshabilitado, para enviarla al habilitarlo.
     *
     * @return la entrega creada, o null si ya existía o el comprobante no tiene factura ni nota
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public EntregaComprobante registrarAutomatica(ComprobanteElectronico comprobante) {
        if (comprobante.getFactura() == null && comprobante.getNotaFactura() == null) {
            return null;
        }
        if (repository.existsByClaveAutomatica(comprobante.getId())) {
            log.debug("Comprobante {} ya tiene su entrega automática", comprobante.getClaveNumerica());
            return null;
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        Cliente receptor = receptorDe(comprobante);
        String correoReceptor = receptor != null && receptor.getEmail() != null ? receptor.getEmail().trim() : "";

        EntregaComprobante entrega = nueva(comprobante, OrigenEntrega.AUTOMATICO, null, ahora);
        entrega.setClaveAutomatica(comprobante.getId());
        if (correoReceptor.isEmpty()) {
            entrega.setEstado(EstadoEntrega.OMITIDO_SIN_CORREO);
        } else if (!esCorreoValido(correoReceptor)) {
            entrega.setEstado(EstadoEntrega.OMITIDO_SIN_CORREO);
            entrega.setUltimoError("El correo del receptor no es válido");
        } else {
            aplicarDestino(entrega, comprobante, correoReceptor, ahora);
        }
        EntregaComprobante guardada = repository.save(entrega);
        log.info("Entrega {} del comprobante {}: {}", guardada.getId(), comprobante.getClaveNumerica(), guardada.getEstado());
        return guardada;
    }

    /**
     * Reenvío manual (ADR-070): crea una entrega MANUAL PENDIENTE al destinatario indicado (por defecto, el correo del
     * receptor), con la misma regla de SANDBOX, y deja la actividad auditada.
     *
     * @param destinatario null o vacío = el correo del receptor
     * @param username     usuario autenticado (su correo de acceso)
     * @throws EntregaCorreoException 404 comprobante inexistente, 409 no ACEPTADO, 400 correo inválido
     */
    @Transactional
    public EntregaDTO registrarManual(Long comprobanteId, String destinatario, String username) {
        ComprobanteElectronico comprobante = comprobanteRepository.findById(comprobanteId)
            .orElseThrow(() -> new EntregaCorreoException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
        if (comprobante.getEstado() != EstadoComprobante.ACEPTADO
            || (comprobante.getFactura() == null && comprobante.getNotaFactura() == null)) {
            throw new EntregaCorreoException(HttpStatus.CONFLICT,
                "Solo se reenvía por correo un comprobante ACEPTADO por Hacienda; este está en estado " + comprobante.getEstado());
        }
        String destino = destinatario == null ? "" : destinatario.trim();
        if (destino.isEmpty()) {
            Cliente receptor = receptorDe(comprobante);
            destino = receptor != null && receptor.getEmail() != null ? receptor.getEmail().trim() : "";
            if (destino.isEmpty()) {
                throw new EntregaCorreoException(HttpStatus.BAD_REQUEST,
                    "El receptor no tiene correo: indique el destinatario");
            }
        }
        if (!esCorreoValido(destino)) {
            throw new EntregaCorreoException(HttpStatus.BAD_REQUEST, "El destinatario no es un correo válido");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        Usuario usuario = username == null ? null : usuarioRepository.findByEmail(username).orElse(null);
        EntregaComprobante entrega = nueva(comprobante, OrigenEntrega.MANUAL, usuario != null ? usuario.getIdUsuario() : null, ahora);
        aplicarDestino(entrega, comprobante, destino, ahora);
        EntregaComprobante guardada = repository.save(entrega);
        if (usuario != null) {
            actividadService.registrarActividad(usuario.getIdUsuario(), ACTIVIDAD_REENVIO,
                "Reenvío por correo del comprobante " + comprobante.getClaveNumerica() + " a "
                    + (guardada.getDestinatario() != null ? guardada.getDestinatario() : "(omitido: SANDBOX)")
                    + " (entrega " + guardada.getId() + ", " + guardada.getEstado() + ")",
                "ComprobanteElectronico", comprobanteId.intValue());
        }
        log.info("Reenvío manual {} del comprobante {}: {}", guardada.getId(), comprobante.getClaveNumerica(),
            guardada.getEstado());
        return EntregaDTO.de(guardada);
    }

    @Transactional(readOnly = true)
    public List<EntregaDTO> listar(Long comprobanteId) {
        return repository.findPorComprobante(comprobanteId).stream().map(EntregaDTO::de).toList();
    }

    /**
     * Campo {@code correo} del detalle: null si no hay comprobante vigente. {@code puedeReenviar} exige el permiso del
     * usuario y el estado ACEPTADO.
     */
    @Transactional(readOnly = true)
    public CorreoDetalleDTO resumenCorreo(Long comprobanteId, String username) {
        if (comprobanteId == null) {
            return null;
        }
        return comprobanteRepository.findById(comprobanteId).map(c -> {
            Cliente receptor = receptorDe(c);
            String correo = receptor != null && receptor.getEmail() != null && !receptor.getEmail().isBlank()
                ? receptor.getEmail().trim() : null;
            boolean puede = c.getEstado() == EstadoComprobante.ACEPTADO && username != null
                && permisoService.tienePermisoPorCodigo(username, PERMISO_REENVIAR);
            return new CorreoDetalleDTO(c.getId(), c.getEstado().name(), correo, puede);
        }).orElse(null);
    }

    private EntregaComprobante nueva(ComprobanteElectronico c, OrigenEntrega origen, Integer usuarioId, LocalDateTime ahora) {
        String cco = String.join(", ", props.ccoLista());
        return EntregaComprobante.builder()
            .comprobante(c)
            .tipoDocumento(c.esDeNota() ? TipoDocumentoEntrega.NOTA_CREDITO : TipoDocumentoEntrega.FACTURA)
            .origen(origen)
            .usuarioId(usuarioId)
            .cco(cco.isEmpty() ? null : cco.length() > 1000 ? cco.substring(0, 1000) : cco)
            .intentos(0)
            .fechaCreacion(ahora)
            .build();
    }

    /** SANDBOX → dirección de prueba (PENDIENTE) o OMITIDO_SANDBOX; PRODUCCION → el destino pedido (PENDIENTE). */
    private void aplicarDestino(EntregaComprobante entrega, ComprobanteElectronico c, String destino, LocalDateTime ahora) {
        if (esSandbox(c)) {
            String prueba = props.destinatarioPrueba();
            if (prueba.isEmpty()) {
                entrega.setEstado(EstadoEntrega.OMITIDO_SANDBOX);
                entrega.setDestinatario(null);
                return;
            }
            destino = prueba;
        }
        entrega.setDestinatario(destino);
        entrega.setEstado(EstadoEntrega.PENDIENTE);
        entrega.setProximoIntento(ahora);
    }
}
