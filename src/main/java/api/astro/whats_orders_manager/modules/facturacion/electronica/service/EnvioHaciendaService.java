package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;

/**
 * Único servicio de envío de comprobantes a Hacienda (T-037, ADR-043 §1). Lo usan la emisión de facturas, la nota de
 * crédito, el envío y el reenvío manuales y ComprobanteReintentosJob; ninguna otra clase llama a
 * {@code HaciendaApiService.enviarComprobante}.
 *
 * <p>Etapas, cada una con su propia transacción salvo el POST:
 * <ol type="a">
 *   <li>preparación: consecutivo con bloqueo (al emitir), regeneración y firma si hace falta, XSD y configuración;
 *       una falla local deja ERROR (ADR-030) y no hay marca ni POST;</li>
 *   <li>marca: UPDATE condicional de un estado transmisible a ENVIO_INCIERTO, con fecha_envio; con 0 filas no se
 *       transmite (otro proceso ya envía);</li>
 *   <li>token y POST de recepción, sin transacción activa y sin reintento automático;</li>
 *   <li>registro de la clasificación (ENVIADO, NO_TRANSMITIDO o ENVIO_INCIERTO); si falla, la marca queda;</li>
 *   <li>sondeo en segundo plano tras un ENVIADO (TaskScheduler, +30 s, hasta 5 consultas).</li>
 * </ol>
 * La petición del usuario espera solo hasta la etapa d.
 */
public interface EnvioHaciendaService {

    /**
     * Emite una factura: genera el comprobante (etapa a, consecutivo con bloqueo) y lo envía.
     *
     * @throws api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ProductosSinCabysException 409,
     *         sin consumir consecutivo
     */
    ComprobanteElectronicoDTO emitirFactura(Long facturaId);

    /**
     * Envía un comprobante existente (también el de una nota de crédito ya firmado).
     *
     * @throws ComprobanteNoEnviableException si su estado no es transmisible (Hacienda lo tiene o pudo recibirlo)
     */
    ComprobanteElectronicoDTO enviar(Long comprobanteId);

    /**
     * Reenvío manual o del job: el mismo envío, con el mensaje de «no se puede reenviar» si el estado no lo permite.
     *
     * @throws ComprobanteNoEnviableException si su estado no es transmisible
     */
    ComprobanteElectronicoDTO reenviar(Long comprobanteId);

    /**
     * Registra en el comprobante el fallo de un reintento del job sin pisar su estado: relee la fila y solo anota el
     * error y suma un intento si sigue en un estado transmisible.
     */
    void registrarFalloDeReintento(Long comprobanteId, String detalle);

    /** El comprobante no está en un estado desde el que se pueda transmitir (GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO). */
    class ComprobanteNoEnviableException extends IllegalStateException {
        private final EstadoComprobante estado;
        private final boolean reenvio;

        public ComprobanteNoEnviableException(EstadoComprobante estado, boolean reenvio) {
            super(reenvio
                ? "Comprobante no puede ser reenviado en estado " + estado
                : "El comprobante en estado " + estado + " no se puede enviar: Hacienda ya lo tiene o pudo recibirlo");
            this.estado = estado;
            this.reenvio = reenvio;
        }

        public EstadoComprobante getEstado() {
            return estado;
        }

        public boolean isReenvio() {
            return reenvio;
        }
    }
}
