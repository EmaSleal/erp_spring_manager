package api.astro.whats_orders_manager.shared.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReparadorMojibakeTest {

    private static String rep(String s) {
        return ReparadorMojibake.reparar(s).orElse(null);
    }

    @Test
    void charsetsDisponibles() {
        assertTrue(Charset.isSupported("IBM850"));
        assertTrue(Charset.isSupported("IBM437"));
    }

    @Test
    void reparaCasosIbm850() {
        assertEquals("poliéster", rep("poli├®ster"));
        assertEquals("aplicación", rep("aplicaci├│n"));
        assertEquals("m²", rep("m┬▓"));
        assertEquals("✅ ¡Pago confirmado!", rep("Ô£à ┬í" + "Pago confirmado!"));
        String campana = new String("🔔".getBytes(StandardCharsets.UTF_8), Charset.forName("IBM850"));
        assertTrue(campana.startsWith("­"));
        assertEquals("🔔", rep(campana));
    }

    @Test
    void reparaRoundTripGenerico() {
        String original = "Café ñandú ¿qué? € 😀";
        for (String cs : new String[] {"IBM850", "windows-1252"}) {
            String roto = new String(original.getBytes(StandardCharsets.UTF_8), Charset.forName(cs));
            if (cs.equals("windows-1252")) {
                // bytes indefinidos en 1252 (p.ej. 0x81) no son reversibles; el caso es solo de ejemplo
                roto = new String("Café ñandú".getBytes(StandardCharsets.UTF_8), Charset.forName(cs));
                assertEquals("Café ñandú", rep(roto));
            } else {
                assertEquals(original, rep(roto));
            }
        }
    }

    @Test
    void textoCorrectoNoCambia() {
        assertFalse(ReparadorMojibake.tieneMarcadores("poliéster m² ✅"));
        assertTrue(ReparadorMojibake.reparar("poliéster").isEmpty());
        assertTrue(ReparadorMojibake.reparar(null).isEmpty());
    }

    @Test
    void mezclaIrreparableQuedaVacia() {
        // mitad correcta (tilde real) + mitad rota: no es mappable en IBM850 como UTF-8 valido
        assertTrue(ReparadorMojibake.reparar("caña poli├®ster 中").isEmpty());
        assertTrue(ReparadorMojibake.reparar("├ solo").isEmpty());
    }

    @Test
    void detectaMarcadores() {
        for (String m : ReparadorMojibake.MARCADORES) {
            assertTrue(ReparadorMojibake.tieneMarcadores("x" + m + "y"));
        }
    }
}
