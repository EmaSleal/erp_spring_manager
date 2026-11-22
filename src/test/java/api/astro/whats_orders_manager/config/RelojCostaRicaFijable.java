package api.astro.whats_orders_manager.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Clock de pruebas en America/Costa_Rica (T-050): corre con la hora real hasta que una prueba lo fija a un instante
 * (p. ej. las 20:00 de Costa Rica, 02:00 UTC del día siguiente) y vuelve a la hora real con {@link #liberar()}.
 */
public class RelojCostaRicaFijable extends Clock {

    public static final ZoneId CR = ZoneId.of(MysqlZonaCostaRica.ZONA);

    private volatile Instant fijo;

    public void fijar(ZonedDateTime momento) {
        this.fijo = momento.toInstant();
    }

    public void liberar() {
        this.fijo = null;
    }

    @Override
    public ZoneId getZone() {
        return CR;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        Instant f = fijo;
        return f != null ? f : Instant.now();
    }
}
