package api.astro.whats_orders_manager.modules.facturacion.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Códigos de referencia de la nota 9 del Anexo «Anexos y Estructuras v4.4» (bitácora al 22/04/2026), campo
 * InformacionReferencia/Codigo. El código 03 no existe.
 *
 * <ul>
 *   <li>{@code validoEnNotaCredito}: 07, 15 y 16 son códigos del comprobante sustituto y 17 es exclusivo del recibo
 *       electrónico de pago; ninguno se usa al emitir una nota de crédito (T-032, criterio 24).</li>
 *   <li>{@code periodoEfecto}: nota al pie de la nota 9. 01, 02, 06 y 12 → periodo de la nota; 13 y 14 → periodo del
 *       comprobante que se modifica (ADR-037 §4). El resto no tiene regla (null).</li>
 *   <li>{@code habilitadoEnT032}: solo la nota total (01 y 13) se emite en esta fase; el resto llega con T-033.</li>
 * </ul>
 */
@Getter
public enum CodigoReferenciaNota {

    ANULA_DOCUMENTO("01", "Anula documento de referencia", true, PeriodoEfectoNota.NOTA, true),
    CORRIGE_MONTO("02", "Corrige monto", true, PeriodoEfectoNota.NOTA, false),
    REFERENCIA_OTRO_DOCUMENTO("04", "Referencia a otro documento", true, null, false),
    SUSTITUYE_PROVISIONAL_CONTINGENCIA("05", "Sustituye comprobante provisional por contingencia", true, null, false),
    DEVOLUCION_MERCANCIA("06", "Devolución de mercancía", true, PeriodoEfectoNota.NOTA, false),
    SUSTITUYE_COMPROBANTE("07", "Sustituye comprobante electrónico", false, null, false),
    FACTURA_ENDOSADA("08", "Factura endosada", true, null, false),
    NOTA_CREDITO_FINANCIERA("09", "Nota de crédito financiera", true, null, false),
    NOTA_DEBITO_FINANCIERA("10", "Nota de débito financiera", true, null, false),
    PROVEEDOR_NO_DOMICILIADO("11", "Proveedor no domiciliado", true, null, false),
    CREDITO_EXONERACION_POSTERIOR("12", "Nota de crédito financiera por exoneración posterior a la facturación", true,
            PeriodoEfectoNota.NOTA, false),
    ANULA_DOCUMENTO_ERROR_MATERIAL("13", "Anula documento de referencia por error material", true,
            PeriodoEfectoNota.DOCUMENTO_ORIGINAL, true),
    CORRIGE_MONTO_ERROR_MATERIAL("14", "Corrige monto por error material", true,
            PeriodoEfectoNota.DOCUMENTO_ORIGINAL, false),
    SUSTITUYE_COMPROBANTE_ERROR_MATERIAL("15", "Sustituye comprobante electrónico por error material", false, null,
            false),
    SUSTITUYE_COMPROBANTE_RECHAZADO("16", "Sustituye comprobante electrónico rechazado", false, null, false),
    PAGO_A_COMPROBANTE("17", "Pago a comprobante electrónico", false, null, false),
    OTROS("99", "Otros", true, null, false);

    private final String codigo;
    private final String nombre;
    private final boolean validoEnNotaCredito;
    private final PeriodoEfectoNota periodoEfecto;
    private final boolean habilitadoEnT032;

    CodigoReferenciaNota(String codigo, String nombre, boolean validoEnNotaCredito, PeriodoEfectoNota periodoEfecto,
                         boolean habilitadoEnT032) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.validoEnNotaCredito = validoEnNotaCredito;
        this.periodoEfecto = periodoEfecto;
        this.habilitadoEnT032 = habilitadoEnT032;
    }

    /** Código de la nota 9, o vacío si no existe (p. ej. "03"). */
    public static Optional<CodigoReferenciaNota> deCodigo(String codigo) {
        return Arrays.stream(values()).filter(c -> c.codigo.equals(codigo)).findFirst();
    }
}
