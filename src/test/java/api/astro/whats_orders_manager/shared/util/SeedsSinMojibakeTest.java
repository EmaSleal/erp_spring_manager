package api.astro.whats_orders_manager.shared.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** T-035: los seeds no deben tener BOM ni secuencias de doble codificacion. */
class SeedsSinMojibakeTest {

    static List<String> violaciones(Path dir) throws IOException {
        List<String> res = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".sql")).sorted()::iterator) {
                byte[] b = Files.readAllBytes(f);
                if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
                    res.add(f.getFileName() + ": BOM");
                }
                String[] lineas = new String(b, StandardCharsets.UTF_8).split("\n", -1);
                for (int i = 0; i < lineas.length; i++) {
                    if (ReparadorMojibake.tieneMarcadores(lineas[i])) {
                        res.add(f.getFileName() + ":" + (i + 1) + ": marcador de doble codificacion");
                    }
                }
            }
        }
        return res;
    }

    private static Path seedsDir() {
        Path p = Paths.get("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("database").resolve("seeds"))) {
            p = p.getParent();
        }
        assertNotNull(p, "no se encontro database/seeds");
        return p.resolve("database").resolve("seeds");
    }

    @Test
    void seedsDelProyectoLimpios() throws IOException {
        assertEquals(List.of(), violaciones(seedsDir()));
    }

    @Test
    void controlNegativoMarcador(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("x.sql"), "INSERT INTO t VALUES ('m┬▓');\n");
        assertFalse(violaciones(tmp).isEmpty());
    }

    @Test
    void controlNegativoBom(@TempDir Path tmp) throws IOException {
        Files.write(tmp.resolve("y.sql"), new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'S', 'E', 'L'});
        assertFalse(violaciones(tmp).isEmpty());
    }
}
