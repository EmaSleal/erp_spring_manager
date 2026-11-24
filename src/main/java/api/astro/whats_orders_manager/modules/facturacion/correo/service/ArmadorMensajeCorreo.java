package api.astro.whats_orders_manager.modules.facturacion.correo.service;

import api.astro.whats_orders_manager.modules.facturacion.correo.client.AdjuntoCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.client.MensajeCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.config.CorreoProperties;
import api.astro.whats_orders_manager.modules.facturacion.correo.model.EntregaComprobante;
import api.astro.whats_orders_manager.modules.facturacion.correo.repository.EntregaComprobanteRepository;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.ContenidoCorreoComprobante.DatosCorreoComprobante;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma el mensaje de una entrega en una transacción de solo lectura (carga el comprobante, el receptor, el PDF y los XML);
 * el envío ocurre después, fuera de toda transacción (ADR-043).
 */
@Service
@RequiredArgsConstructor
public class ArmadorMensajeCorreo {

    /** Qué hacer con la entrega reclamada. */
    public enum Accion { ENVIAR, OMITIR_SANDBOX, OMITIR_SIN_CORREO }

    public record Preparado(Accion accion, MensajeCorreo mensaje) {
    }

    private final EntregaComprobanteRepository repository;
    private final FacturaPdfService pdfService;
    private final ComprobanteElectronicoService comprobanteService;
    private final CorreoProperties props;

    @Transactional(readOnly = true)
    public Preparado armar(Long entregaId) {
        EntregaComprobante entrega = repository.findById(entregaId)
            .orElseThrow(() -> new IllegalStateException("Entrega " + entregaId + " no encontrada"));
        ComprobanteElectronico c = entrega.getComprobante();
        String destinatario = entrega.getDestinatario() == null ? "" : entrega.getDestinatario().trim();
        if (destinatario.isEmpty()) {
            return new Preparado(Accion.OMITIR_SIN_CORREO, null);
        }
        boolean sandbox = EntregaComprobanteService.esSandbox(c);
        // Defensa en profundidad: un comprobante de SANDBOX solo sale hacia la dirección de prueba configurada
        if (sandbox && !destinatario.equalsIgnoreCase(props.destinatarioPrueba())) {
            return new Preparado(Accion.OMITIR_SANDBOX, null);
        }
        Cliente receptor = EntregaComprobanteService.receptorDe(c);
        DatosCorreoComprobante datos = datosDe(c, receptor);

        List<AdjuntoCorreo> adjuntos = new ArrayList<>();
        byte[] pdf = c.esDeNota() ? pdfService.generarPdfNotaCredito(c.getNotaFactura().getId())
            : pdfService.generarPdfFactura(c.getFactura().getIdFactura());
        adjuntos.add(new AdjuntoCorreo(ContenidoCorreoComprobante.nombrePdf(c.getClaveNumerica()), pdf));
        adjuntos.add(new AdjuntoCorreo(ContenidoCorreoComprobante.nombreXml(c.getClaveNumerica()),
            comprobanteService.descargarXml(c.getId()).getBytes(StandardCharsets.UTF_8)));
        if (c.getXmlRespuesta() != null && !c.getXmlRespuesta().isBlank()) {
            adjuntos.add(new AdjuntoCorreo(ContenidoCorreoComprobante.nombreRespuestaHacienda(c.getClaveNumerica()),
                c.getXmlRespuesta().getBytes(StandardCharsets.UTF_8)));
        }
        String replyTo = props.tieneReplyTo() ? props.getReplyTo().trim() : null;
        MensajeCorreo mensaje = new MensajeCorreo(props.getRemitente().trim(), destinatario, props.ccoLista(), replyTo,
            ContenidoCorreoComprobante.asunto(datos, sandbox), ContenidoCorreoComprobante.html(datos, replyTo != null),
            adjuntos);
        return new Preparado(Accion.ENVIAR, mensaje);
    }

    private static DatosCorreoComprobante datosDe(ComprobanteElectronico c, Cliente receptor) {
        ConfiguracionEmpresa empresa = c.getEmpresa();
        String emisor = empresa == null ? "" : empresa.getRazonSocial() != null && !empresa.getRazonSocial().isBlank()
            ? empresa.getRazonSocial() : empresa.getNombreEmpresa();
        BigDecimal total;
        String moneda;
        if (c.esDeNota()) {
            total = c.getNotaFactura().getTotal();
            moneda = c.getNotaFactura().getMoneda();
        } else {
            total = c.getFactura().getTotal();
            moneda = c.getFactura().getMonedaFE() != null ? c.getFactura().getMonedaFE().name() : "CRC";
        }
        return new DatosCorreoComprobante(c.esDeNota(), receptor != null ? receptor.getNombre() : "", c.getConsecutivo(),
            c.getClaveNumerica(), c.getFechaEmision(), total, moneda, emisor == null ? "" : emisor);
    }
}
