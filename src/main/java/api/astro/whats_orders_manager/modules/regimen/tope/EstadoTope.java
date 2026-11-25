package api.astro.whats_orders_manager.modules.regimen.tope;

import java.math.BigDecimal;

/**
 * Estado del tope de compras del RTS de un año (T-056). Sin parámetros del año ({@code configurado} false) solo vienen el
 * año y el mensaje «Tope de AAAA sin configurar»: nunca se usan los parámetros de otro año.
 *
 * @param tope        salario base x multiplicador (con IVA, en CRC)
 * @param acumulado   suma en CRC de las CxP no anuladas con fecha de comprobante en el año y en tramos SIMPLIFICADO
 * @param porcentaje  acumulado / tope x 100, con 2 decimales
 * @param mensaje     texto de aviso o de «sin configurar»; null mientras el nivel es BAJO
 */
public record EstadoTope(int anio, boolean configurado, BigDecimal tope, BigDecimal acumulado, BigDecimal porcentaje,
                         NivelTope nivel, BigDecimal umbralAviso, BigDecimal umbralTope, String mensaje) {

    public static EstadoTope sinConfigurar(int anio) {
        return new EstadoTope(anio, false, null, null, null, null, null, null, "Tope de " + anio + " sin configurar");
    }
}
