package api.astro.whats_orders_manager.modules.facturacion.correo.service;

import org.springframework.http.HttpStatus;

/** Error de negocio del reenvío manual: lleva el estado HTTP (400, 404, 409) y el mensaje para el usuario. */
public class EntregaCorreoException extends RuntimeException {

    private final HttpStatus estado;

    public EntregaCorreoException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
