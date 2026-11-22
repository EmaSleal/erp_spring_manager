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
 * T-050 (ADR-060, criterio 10): la fecha de negocio («hoy», «mes», «vencido») sale de un java.time.Clock de la
 * aplicación en America/Costa_Rica, nunca del reloj de la base. Esta prueba recorre todos los literales de cadena de
 * src/main/java (consultas {@code @Query}, SQL nativo, JdbcTemplate, createNativeQuery, etc.; los comentarios se
 * ignoran) y falla si alguno usa CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIME, CURDATE(), NOW(), SYSDATE(),
 * LOCALTIME o LOCALTIMESTAMP. No hay ArchUnit en el proyecto y la reflexión no ve el SQL nativo, así que se leen las
 * fuentes (como EnvioHaciendaEstructuraTest).
 *
 * Excepciones: ninguna. Si alguna vez hace falta una (p. ej. una migración que deba escribir la hora de la base),
 * se agrega a EXCEPCIONES con su justificación; las migraciones SQL de src/main/resources no se recorren porque son
 * historia ya aplicada y no consultas de la aplicación.
 */
@DisplayName("T-050 — arquitectura: ninguna consulta usa el reloj de la base")
class ArquitecturaRelojBaseTest {

    private static final Path FUENTES = Path.of("src/main/java");

    /** archivo (nombre) -> justificación. Vacío a propósito. */
    private static final Map<String, String> EXCEPCIONES = Map.of();

    static final Pattern RELOJ_DE_LA_BASE = Pattern.compile(
            "\\b(CURRENT_DATE|CURRENT_TIMESTAMP|CURRENT_TIME|LOCALTIMESTAMP|LOCALTIME)\\b"
                    + "|\\b(CURDATE|CURTIME|NOW|SYSDATE|UTC_TIMESTAMP|UTC_DATE)\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    /** Contenido de todos los literales de cadena (incluidos los bloques de texto) fuera de los comentarios. */
    static List<String> literales(String codigo) {
        List<String> res = new ArrayList<>();
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
            } else if (codigo.startsWith("\"\"\"", i)) {
                int fin = codigo.indexOf("\"\"\"", i + 3);
                fin = fin < 0 ? n : fin;
                res.add(codigo.substring(i + 3, fin));
                i = Math.min(n, fin + 3);
            } else if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                while (i < n && codigo.charAt(i) != '"' && codigo.charAt(i) != '\n') {
                    if (codigo.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                    }
                    sb.append(codigo.charAt(i));
                    i++;
                }
                res.add(sb.toString());
                i++;
            } else if (c == '\'') {
                i++;
                while (i < n && codigo.charAt(i) != '\'') {
                    if (codigo.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                i++;
            } else {
                i++;
            }
        }
        return res;
    }

    static List<String> violaciones(String codigo) {
        return literales(codigo).stream()
                .filter(l -> RELOJ_DE_LA_BASE.matcher(l).find())
                .map(l -> l.strip().replaceAll("\\s+", " "))
                .toList();
    }

    @Test
    @DisplayName("ningún @Query ni SQL nativo de src/main/java usa CURRENT_DATE, CURRENT_TIMESTAMP, CURDATE(), NOW() ni SYSDATE()")
    void sinRelojDeLaBase() throws IOException {
        List<String> infracciones = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(FUENTES)) {
            for (Path p : archivos.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (EXCEPCIONES.containsKey(p.getFileName().toString())) {
                    continue;
                }
                for (String v : violaciones(Files.readString(p, StandardCharsets.UTF_8))) {
                    infracciones.add(p.getFileName() + ": " + v);
                }
            }
        }
        assertThat(infracciones)
                .as("la fecha de negocio se calcula con el Clock de la aplicación y se pasa como parámetro")
                .isEmpty();
    }

    @Test
    @DisplayName("el detector encuentra el reloj de la base en un literal y ignora comentarios y LocalDate.now()")
    void detectorFunciona() {
        String malo = "@Query(\"SELECT p FROM Pago p WHERE CAST(p.fechaPago AS date) = CURRENT_DATE\") List<Pago> x();"
                + "String s = \"select now() \" + \"from dual\";"
                + "String t = \"\"\"\n  SELECT CURDATE()\n  \"\"\";"
                + "String u = \"x CURRENT_TIMESTAMP\";";
        assertThat(violaciones(malo)).hasSize(4);

        String bueno = "// WHERE f.fecha < CURRENT_DATE\n/* NOW() CURDATE() */\n"
                + "LocalDate hoy = LocalDate.now(clock); LocalDateTime a = LocalDateTime.now();"
                + "String q = \"WHERE f.fechaPago < :hoy\"; String apostrofe = \"it's\"; char c = '\"';";
        assertThat(violaciones(bueno)).isEmpty();
    }
}
