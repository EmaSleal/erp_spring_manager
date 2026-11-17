package api.astro.whats_orders_manager.shared.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Detecta y repara texto UTF-8 que fue leido como otro charset (doble codificacion, "mojibake").
 * El caso observado en produccion es IBM850 (a veces IBM437 o Windows-1252).
 */
public final class ReparadorMojibake {

    /** Secuencias marcadoras de doble codificacion. Fuente unica (migracion y prueba de seeds). */
    public static final List<String> MARCADORES = List.of(
            "├", // ├
            "┬", // ┬
            "┤", // ┤
            "╗", // ╗
            "Ã", // Ã
            "Â", // Â
            "â€", // â€
            "­", // guion blando (0xF0 de UTF-8 leido como IBM850: emojis)
            "ƒ"); // ƒ (0x9F de UTF-8 leido como IBM850)

    private static final List<String> CHARSETS = List.of("IBM850", "IBM437", "windows-1252", "ISO-8859-1");

    private ReparadorMojibake() {
    }

    public static boolean tieneMarcadores(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (String m : MARCADORES) {
            if (s.contains(m)) {
                return true;
            }
        }
        return false;
    }

    /** Devuelve el texto reparado, o vacio si no hay una reparacion valida. */
    public static Optional<String> reparar(String s) {
        if (s == null || !tieneMarcadores(s)) {
            return Optional.empty();
        }
        for (String nombre : CHARSETS) {
            if (!Charset.isSupported(nombre)) {
                continue;
            }
            Optional<String> r = intentar(s, Charset.forName(nombre));
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> intentar(String s, Charset cs) {
        try {
            ByteBuffer bytes = cs.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(s));
            String r = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(bytes)
                    .toString();
            if (r.equals(s) || tieneMarcadores(r)) {
                return Optional.empty();
            }
            return Optional.of(r);
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }
}
