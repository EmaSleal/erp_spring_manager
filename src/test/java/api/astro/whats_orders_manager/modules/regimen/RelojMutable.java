package api.astro.whats_orders_manager.modules.regimen;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Reloj de pruebas con la hora de Costa Rica que se puede mover (T-056). */
public class RelojMutable extends Clock {

    public static final ZoneId CR = ZoneId.of("America/Costa_Rica");

    private volatile Instant instante;

    public RelojMutable(LocalDate dia) {
        fijar(dia);
    }

    public void fijar(LocalDate dia) {
        this.instante = dia.atTime(10, 0).atZone(CR).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return CR;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
