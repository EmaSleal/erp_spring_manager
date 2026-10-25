package api.astro.whats_orders_manager.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-026 (ADR-031 §1): toda página que hace una mutación por JS (fetch con POST/PUT/PATCH/DELETE o con método
 * variable, $.ajax/$.post de mutación, o los helpers httpPost/httpPut/httpDelete de common.js) debe tener las meta
 * _csrf/_csrf_header y cargar common.js, que agrega el encabezado CSRF. Vale la plantilla o los scripts locales que
 * incluye con th:src. Las meta y common.js pueden venir de shared/layout (head y scripts) o estar en la plantilla.
 * Los fragmentos sin &lt;body&gt; se validan en la página que los incluye.
 */
@DisplayName("T-026 — plantillas con mutaciones incluyen las meta CSRF y common.js")
class PlantillasCsrfTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path STATIC = Path.of("src/main/resources/static");

    private static final List<Pattern> MUTACIONES = List.of(
            Pattern.compile("method\\s*:\\s*['\"`](POST|PUT|PATCH|DELETE)['\"`]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("method\\s*:\\s*(?!['\"`])[A-Za-z_$][\\w$.]*"),
            Pattern.compile("type\\s*:\\s*['\"](POST|PUT|PATCH|DELETE)['\"]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\$\\.post\\s*\\("),
            Pattern.compile("\\b(httpPost|httpPut|httpDelete|httpDeleteWithConfirm)\\s*\\("));

    private static final Pattern SCRIPT_LOCAL = Pattern.compile("th:src=\"@\\{(/[^}\"]+\\.js)\\}\"");

    @Test
    @DisplayName("cada plantilla con una mutación por JS tiene meta _csrf/_csrf_header y common.js")
    void plantillasConMutacion() throws IOException {
        List<String> faltantes = new ArrayList<>();
        int conMutacion = 0;
        try (Stream<Path> plantillas = Files.walk(TEMPLATES)) {
            for (Path plantilla : plantillas.filter(p -> p.toString().endsWith(".html")).sorted().toList()) {
                String html = Files.readString(plantilla, StandardCharsets.UTF_8);
                if (!html.contains("<body")) {
                    continue; // fragmento
                }
                if (!tieneMutacion(html + scriptsLocales(html))) {
                    continue;
                }
                conMutacion++;
                boolean meta = html.contains("shared/layout :: head")
                        || (html.contains("name=\"_csrf\"") && html.contains("name=\"_csrf_header\""));
                boolean commonJs = html.contains("shared/layout :: scripts") || html.contains("/shared/js/common.js");
                if (!meta || !commonJs) {
                    faltantes.add(TEMPLATES.relativize(plantilla) + (meta ? "" : " (sin meta _csrf/_csrf_header)")
                            + (commonJs ? "" : " (sin common.js)"));
                }
            }
        }

        assertThat(conMutacion).as("plantillas con mutaciones encontradas").isGreaterThan(10);
        assertThat(faltantes).as("plantillas con mutaciones sin meta CSRF o sin common.js").isEmpty();
    }

    private static boolean tieneMutacion(String codigo) {
        return MUTACIONES.stream().anyMatch(p -> p.matcher(codigo).find());
    }

    /** Contenido de los scripts locales (static/) que la plantilla incluye con th:src. */
    private static String scriptsLocales(String html) throws IOException {
        StringBuilder codigo = new StringBuilder();
        Matcher m = SCRIPT_LOCAL.matcher(html);
        while (m.find()) {
            Path js = STATIC.resolve(m.group(1).substring(1));
            if (Files.exists(js) && !js.endsWith(Path.of("shared/js/common.js"))) {
                codigo.append('\n').append(Files.readString(js, StandardCharsets.UTF_8));
            }
        }
        return codigo.toString();
    }
}
