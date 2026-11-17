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

    @Test
    @DisplayName("review 1 (P-091): ningún literal de client_id, realm ni URL del IdP o la API de Hacienda fuera de AmbienteHacienda")
    void constantesDelAmbienteSoloEnElEnum() throws IOException {
        Pattern literal = Pattern.compile("\"(api-stag|api-prod)\"|rut-stag|realms/rut\\b|(idp|api|api-sandbox)\\.comprobanteselectronicos\\.go\\.cr"
            + "|/recepcion/v1|mensajes-receptor/v1");
        List<String> conLiterales = fuentes(FUENTES).stream()
            .filter(p -> !p.getFileName().toString().equals("AmbienteHacienda.java"))
            .filter(p -> literal.matcher(sinComentarios(leer(p))).find())
            .map(p -> p.getFileName().toString())
            .toList();
        assertThat(conLiterales).isEmpty();
    }

    /**
     * Quita los comentarios de bloque y los de línea que empiezan la línea (javadoc, //...). No toca el "//" de una URL
     * dentro de un literal: así un literal "https://idp.comprobanteselectronicos.go.cr/..." sí se detecta (review 1, D1).
     */
    static String sinComentarios(String codigo) {
        return codigo.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)^\\s*//.*$", "");
    }

    @Test
    @DisplayName("control negativo del filtro: detecta client_id, realm y URLs completas de Hacienda en literales")
    void filtroDetectaLiteralesDeUrl() {
        Pattern literal = Pattern.compile("\"(api-stag|api-prod)\"|rut-stag|realms/rut\\b|(idp|api|api-sandbox)\\.comprobanteselectronicos\\.go\\.cr"
            + "|/recepcion/v1|mensajes-receptor/v1");
        for (String codigo : List.of(
                "String u = \"https://idp.comprobanteselectronicos.go.cr/auth/realms/rut/protocol/openid-connect/token\";",
                "String b = \"https://api.comprobanteselectronicos.go.cr\"; // base",
                "body.add(\"client_id\", \"api-prod\");",
                "String r = base + \"/recepcion/v1/recepcion\";")) {
            assertThat(literal.matcher(sinComentarios(codigo)).find()).as(codigo).isTrue();
        }
        // Los namespaces XML (cdn.comprobanteselectronicos.go.cr) no dependen del ambiente
        assertThat(literal.matcher("String ns = \"https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/x\";").find())
            .isFalse();
        assertThat(literal.matcher(sinComentarios("// https://api.comprobanteselectronicos.go.cr en un comentario\n"
            + "/* realm rut-stag */ int x = 1;")).find()).isFalse();
    }
}
