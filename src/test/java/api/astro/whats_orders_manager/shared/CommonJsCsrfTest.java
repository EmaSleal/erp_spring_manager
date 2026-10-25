package api.astro.whats_orders_manager.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * T-026 (ADR-031 §1): ejecuta las pruebas JS del interceptor CSRF de common.js (src/test/js/csrf-fetch.test.mjs)
 * con el runner nativo de Node. Se omite si no hay node en el PATH; a mano: {@code node --test 'src/test/js/*.test.mjs'}.
 */
@DisplayName("T-026 — interceptor CSRF de common.js (node --test)")
class CommonJsCsrfTest {

    @Test
    @DisplayName("node --test src/test/js/*.test.mjs termina sin fallos")
    void pruebasJs() throws Exception {
        assumeTrue(nodeDisponible(), "node no está en el PATH");

        Process node = new ProcessBuilder("node", "--test", "src/test/js/*.test.mjs")
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        // Se lee la salida antes de esperar: con el pipe lleno node no terminaría
        String salida = new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean termino = node.waitFor(2, TimeUnit.MINUTES);

        assertThat(termino).as("node --test terminó a tiempo").isTrue();
        assertThat(node.exitValue()).as(salida).isZero();
        assertThat(salida).contains("# fail 0");
    }

    private static boolean nodeDisponible() {
        try {
            Process p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
            return p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
