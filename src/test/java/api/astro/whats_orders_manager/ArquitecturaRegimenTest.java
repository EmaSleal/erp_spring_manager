package api.astro.whats_orders_manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-056 (ADR-072, criterio 8): el régimen tributario se consulta solo en los lugares previstos. Recorre src/main/java
 * (como ArquitecturaRelojBaseTest: no hay ArchUnit en el proyecto) y falla si un archivo fuera de la lista permitida
 * menciona, en código (los comentarios y los literales de cadena se ignoran), RegimenTributarioService,
 * RegimenTributarioRepository o getRegimenFiscal(.
 *
 * Lista permitida (por ruta; la verificación con Hacienda y el tope del RTS de la parte 2 viven dentro del paquete):
 *  - modules/regimen/ y todo lo que cuelga de él: modelo, repositorio, servicio, controladores, DTO;
 *    modules/regimen/verificacion/ (VerificacionRegimenService y su job) y modules/regimen/tope/ (servicio del tope);
 *  - modules/politicas/: el paquete de políticas por régimen (P-129, P-130, P-131), por ahora solo su package-info.
 * Todo lo demás (ventas, emisión, compras, reportes) obtiene el régimen a través de una política, nunca directamente.
 *
 * Excepciones: ninguna. Si alguna vez hace falta una, se agrega a EXCEPCIONES (nombre de archivo -> justificación).
 */
@DisplayName("T-056 — arquitectura: el régimen se consulta solo desde el paquete del régimen y las políticas")
class ArquitecturaRegimenTest {

    private static final Path FUENTES = Path.of("src/main/java");

    /** archivo (nombre) -> justificación. Vacío a propósito. */
    private static final Map<String, String> EXCEPCIONES = Map.of();

    /** Rutas (con separador «/») cuyos archivos pueden usar el régimen. */
    static final List<String> RUTAS_PERMITIDAS = List.of(
            "/modules/regimen/",
            "/modules/politicas/");

    static final Pattern PROHIBIDO = Pattern.compile(
            "\\bRegimenTributarioService\\b|\\bRegimenTributarioRepository\\b|\\bgetRegimenFiscal\\s*\\(");

    /** El código sin comentarios ni literales de cadena ni de carácter. */
    static String soloCodigo(String codigo) {
        StringBuilder sb = new StringBuilder();
        int n = codigo.length();
        int i = 0;
        while (i < n) {
            char c = codigo.charAt(i);
            if (c == '/' && i + 1 < n && codigo.charAt(i + 1) == '/') {
                while (i < n && codigo.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && codigo.charAt(i + 1) == '*') {
                int fin = codigo.indexOf("*/", i + 2);
                i = fin < 0 ? n : fin + 2;
                sb.append(' ');
            } else if (codigo.startsWith("\"\"\"", i)) {
                int fin = codigo.indexOf("\"\"\"", i + 3);
                i = fin < 0 ? n : fin + 3;
                sb.append(" \"\" ");
            } else if (c == '"') {
                i++;
                while (i < n && codigo.charAt(i) != '"' && codigo.charAt(i) != '\n') {
                    if (codigo.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                sb.append(" \"\" ");
            } else if (c == '\'') {
                i++;
                while (i < n && codigo.charAt(i) != '\'') {
                    if (codigo.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
                sb.append(" '' ");
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    static List<String> usosProhibidos(String codigo) {
        List<String> usos = new ArrayList<>();
        var m = PROHIBIDO.matcher(soloCodigo(codigo));
        while (m.find()) {
            usos.add(m.group().replaceAll("\\s+", ""));
        }
        return usos;
    }

    static boolean permitido(Path archivo) {
        String ruta = archivo.toString().replace('\\', '/');
        return RUTAS_PERMITIDAS.stream().anyMatch(ruta::contains);
    }

    @Test
    @DisplayName("ningún archivo fuera del paquete del régimen y de las políticas menciona el servicio, el repositorio ni getRegimenFiscal(")
    void soloLugaresPermitidos() throws IOException {
        List<String> infracciones = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(FUENTES)) {
            for (Path p : archivos.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (permitido(p) || EXCEPCIONES.containsKey(p.getFileName().toString())) {
                    continue;
                }
                for (String uso : usosProhibidos(Files.readString(p, StandardCharsets.UTF_8))) {
                    infracciones.add(p + ": " + uso);
                }
            }
        }
        assertThat(infracciones)
                .as("el régimen se obtiene a través de una política (modules/politicas) y no directamente (ADR-072)")
                .isEmpty();
    }

    @Test
    @DisplayName("las excepciones empiezan vacías, y los paquetes permitidos existen")
    void excepcionesVaciasYPaquetes() {
        assertThat(EXCEPCIONES).isEmpty();
        assertThat(Files.isDirectory(FUENTES.resolve("api/astro/whats_orders_manager/modules/regimen"))).isTrue();
        assertThat(Files.isRegularFile(FUENTES.resolve("api/astro/whats_orders_manager/modules/politicas/package-info.java"))).isTrue();
    }

    @Test
    @DisplayName("autoprueba: detecta cada uso prohibido en código, ignora comentarios y literales, y respeta la lista permitida")
    void detectorFunciona() {
        assertThat(usosProhibidos("class A { RegimenTributarioService s; }")).containsExactly("RegimenTributarioService");
        assertThat(usosProhibidos("import x.RegimenTributarioRepository; class A { }")).containsExactly("RegimenTributarioRepository");
        assertThat(usosProhibidos("String r = empresa.getRegimenFiscal();")).containsExactly("getRegimenFiscal(");
        assertThat(usosProhibidos("var a = e.getRegimenFiscal (); var b = new RegimenTributarioService(x);")).hasSize(2);

        String inocuo = "// usa RegimenTributarioService\n/* RegimenTributarioRepository getRegimenFiscal() */\n"
                + "String s = \"getRegimenFiscal()\"; String t = \"\"\"\n RegimenTributarioService\n \"\"\"; char c = '\"';"
                + "String u = e.getRegimenFiscalHistorico(); class RegimenTributarioServiceTest {}";
        assertThat(usosProhibidos(inocuo)).isEmpty();

        assertThat(permitido(Path.of("src/main/java/api/astro/whats_orders_manager/modules/regimen/service/RegimenTributarioService.java"))).isTrue();
        assertThat(permitido(Path.of("src/main/java/api/astro/whats_orders_manager/modules/regimen/verificacion/VerificacionRegimenService.java"))).isTrue();
        assertThat(permitido(Path.of("src/main/java/api/astro/whats_orders_manager/modules/regimen/tope/TopeComprasRtsService.java"))).isTrue();
        assertThat(permitido(Path.of("src/main/java/api/astro/whats_orders_manager/modules/politicas/package-info.java"))).isTrue();
        assertThat(permitido(Path.of("src/main/java/api/astro/whats_orders_manager/modules/facturacion/service/FacturaService.java"))).isFalse();
        assertThat(permitido(Path.of("src/main/java/api/astro/whats_orders_manager/modules/configuracion/model/ConfiguracionEmpresa.java"))).isFalse();
    }
}
