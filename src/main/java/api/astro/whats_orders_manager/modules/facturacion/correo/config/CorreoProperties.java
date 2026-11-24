package api.astro.whats_orders_manager.modules.facturacion.correo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * T-055 (ADR-070): propiedades {@code correo.*} del envío de comprobantes. Valores por defecto en application.yml; la
 * clave de Resend sale solo de la variable de entorno RESEND_API_KEY ({@code correo.resend.api-key}) y no se imprime
 * nunca. Las pruebas cambian los valores de este bean; el código los lee en cada uso (no los copia).
 */
@Data
@Component
@ConfigurationProperties(prefix = "correo")
public class CorreoProperties {

    private Comprobantes comprobantes = new Comprobantes();
    /** Remitente (From) de los correos, con nombre: «Monrachem &lt;no-reply@astro-solutions.net&gt;». */
    private String remitente = "";
    /** Reply-To; vacío = el correo no recibe respuestas. */
    private String replyTo = "";
    /** CCO (BCC) de cada correo; varias direcciones separadas por coma. */
    private String cco = "";
    private Sandbox sandbox = new Sandbox();
    /** Tope de correos ENVIADOS por día (día de America/Costa_Rica). */
    private int topeDiario = 100;
    private Reintentos reintentos = new Reintentos();
    private Resend resend = new Resend();
    private Job job = new Job();

    @Data
    public static class Comprobantes {
        /** Apagado por defecto: las entregas igual se registran PENDIENTE y se envían al habilitarlo. */
        private boolean habilitado = false;
    }

    @Data
    public static class Sandbox {
        /** Los comprobantes de SANDBOX solo se envían a esta dirección (con el prefijo [SANDBOX]); vacía = no se envían. */
        private String destinatarioPrueba = "";
    }

    @Data
    public static class Reintentos {
        /** Intentos fallidos tras los cuales la entrega pasa a FALLIDO. */
        private int maximo = 5;
        /** Espera tras el primer fallo; se duplica en cada intento siguiente. */
        private Duration esperaInicial = Duration.ofMinutes(5);
    }

    @Data
    public static class Resend {
        private String urlBase = "https://api.resend.com";
        /** Solo de la variable de entorno RESEND_API_KEY. Nunca se registra ni se guarda en errores. */
        private String apiKey = "";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(20);

        @Override
        public String toString() {
            return "Resend(urlBase=" + urlBase + ", apiKey=" + (apiKey == null || apiKey.isBlank() ? "<vacía>" : "<oculta>") + ")";
        }
    }

    @Data
    public static class Job {
        /** Entregas que toma cada corrida. */
        private int lote = 20;
        /** Pausa entre corridas del job (fixedDelay). */
        private Duration intervalo = Duration.ofMinutes(1);
        /** Tiempo que una entrega reclamada queda reservada si el proceso cae antes de registrar el resultado. */
        private Duration reserva = Duration.ofMinutes(10);
    }

    public boolean habilitado() {
        return comprobantes.isHabilitado();
    }

    /** CCO como lista (separada por coma o punto y coma), sin vacíos. */
    public List<String> ccoLista() {
        if (cco == null || cco.isBlank()) {
            return List.of();
        }
        return Arrays.stream(cco.split("[,;]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public String destinatarioPrueba() {
        return sandbox.getDestinatarioPrueba() == null ? "" : sandbox.getDestinatarioPrueba().trim();
    }

    public boolean tieneReplyTo() {
        return replyTo != null && !replyTo.isBlank();
    }
}
