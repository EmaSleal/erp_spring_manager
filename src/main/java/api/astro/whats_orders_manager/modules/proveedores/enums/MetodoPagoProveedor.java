package api.astro.whats_orders_manager.modules.proveedores.enums;

public enum MetodoPagoProveedor {
    TRANSFERENCIA,
    CHEQUE,
    EFECTIVO,
    SINPE,
    DEPOSITO;

    /**
     * Medios que salen de una cuenta bancaria propia (ADR-018): la cuenta bancaria de origen es
     * obligatoria y el asiento acredita su hoja contable. EFECTIVO usa la clave CPP_BANCO_EFECTIVO.
     * @return true para TRANSFERENCIA, CHEQUE, SINPE y DEPOSITO
     */
    public boolean requiereCuentaBancaria() {
        return this != EFECTIVO;
    }
}
