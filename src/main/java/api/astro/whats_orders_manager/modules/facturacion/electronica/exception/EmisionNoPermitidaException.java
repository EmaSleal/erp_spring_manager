package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

/**
 * La factura no se puede emitir por una regla del negocio (cliente sin facturación electrónica, comprobante vigente que
 * no fue rechazado, empresa sin cédula...). Lleva un mensaje para el usuario escrito por la aplicación, nunca el texto
 * de otra excepción (T-037, P-070). Se responde 409. Es IllegalStateException para conservar el tipo de antes.
 */
public class EmisionNoPermitidaException extends IllegalStateException {

    public EmisionNoPermitidaException(String mensajeUsuario) {
        super(mensajeUsuario);
    }

    public String getMensajeUsuario() {
        return getMessage();
    }
}
