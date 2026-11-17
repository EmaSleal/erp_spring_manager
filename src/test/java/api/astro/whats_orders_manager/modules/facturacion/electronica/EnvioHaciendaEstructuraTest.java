package api.astro.whats_orders_manager.modules.facturacion.electronica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-037 (criterios 1 y 3): prueba estructural sobre el código fuente de producción. No hay ArchUnit en el proyecto y
 * la reflexión no ve las llamadas, así que se recorre src/main/java:
 * <ul>
 *   <li>solo EnvioHaciendaServiceImpl llama a {@code HaciendaApiService.enviarComprobante} (además de su declaración
 *       y su implementación);</li>
 *   <li>no queda {@code procesarFacturaAsync} ni ningún {@code Thread.sleep} en facturación electrónica.</li>
 * </ul>
 */
@DisplayName("T-037 — estructura: un único servicio de envío, sin sleep ni @Async en el envío")
class EnvioHaciendaEstructuraTest {

    private static final Path FUENTES = Path.of("src/main/java");
    private static final Path FE = FUENTES.resolve("api/astro/whats_orders_manager/modules/facturacion");
    private static final Pattern LLAMADA_ENVIAR = Pattern.compile("\\.enviarComprobante\\s*\\(");

    private static List<Path> fuentes(Path raiz) throws IOException {
        try (Stream<Path> archivos = Files.walk(raiz)) {
            return archivos.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static String leer(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("solo EnvioHaciendaServiceImpl llama a HaciendaApiService.enviarComprobante")
    void unSoloLlamadorDelPost() throws IOException {
        List<String> llamadores = fuentes(FUENTES).stream()
            .filter(p -> LLAMADA_ENVIAR.matcher(leer(p)).find())
            .map(p -> p.getFileName().toString())
            .toList();
        assertThat(llamadores).containsExactly("EnvioHaciendaServiceImpl.java");
    }

    @Test
    @DisplayName("sin procesarFacturaAsync, sin @Async en el envío y sin Thread.sleep en facturación")
    void sinSleepNiAsyncEnElEnvio() throws IOException {
        for (Path p : fuentes(FE)) {
            String codigo = leer(p);
            assertThat(codigo).as(p.toString()).doesNotContain("procesarFacturaAsync").doesNotContain("Thread.sleep");
        }
        for (Path p : fuentes(FE.resolve("electronica"))) {
            assertThat(leer(p)).as(p.toString()).doesNotContain("@Async");
        }
    }
}
