package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Hora fiscal de los comprobantes electrónicos: la de Costa Rica (America/Costa_Rica, UTC-6 sin horario de verano),
 * sin depender de la zona por defecto de la JVM (T-033).
 *
 * La fecha de emisión del comprobante, el segmento de fecha de su clave y la FechaEmision del XML salen de
 * {@link #ahora(Clock)}: la hora local de Costa Rica del instante del reloj. Un comprobante guarda esa hora local
 * ({@code LocalDateTime}), y {@link #formatear(LocalDateTime)} la expresa con su desfase real (-06:00).
 */
public final class HoraFiscal {

    public static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private HoraFiscal() {
    }

    /** Hora local de Costa Rica del instante actual del reloj, sea cual sea la zona del reloj o de la JVM. */
    public static LocalDateTime ahora(Clock clock) {
        return LocalDateTime.now(clock.withZone(ZONA));
    }

    /** Fecha de un comprobante (hora local de Costa Rica) en el formato del XML, con su desfase: 2026-11-12T21:22:23-06:00. */
    public static String formatear(LocalDateTime horaCostaRica) {
        if (horaCostaRica == null) {
            throw new IllegalStateException("Fecha de emisión no disponible");
        }
        return horaCostaRica.atZone(ZONA).format(FORMATO);
    }
}
