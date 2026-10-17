package api.astro.whats_orders_manager.shared.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * T-022 (ADR-027) — El envío de correos lee la configuración de email con el único método de lectura. Con la
 * fila en activo=0 (o sin fila) no se conecta al servidor SMTP y lo registra sin datos sensibles. Un
 * ServerSocket local hace de servidor SMTP: se verifica que no reciba ninguna conexión.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@DisplayName("T-022 — envío de correos con la configuración inactiva")
class EmailServiceEnvioDeshabilitadoTest {

    private static final String PASSWORD = "clave-smtp-no-debe-salir";
    private static final String DESTINO = "cliente.t022@example.invalid";

    @Mock private ConfiguracionEmailService configuracionEmailService;
    @InjectMocks private EmailServiceImpl emailService;

    private ServerSocket servidorSmtp;

    @BeforeEach
    void abrirServidor() throws Exception {
        servidorSmtp = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        servidorSmtp.setSoTimeout(300);
    }

    @AfterEach
    void cerrarServidor() throws Exception {
        servidorSmtp.close();
    }

    private ConfiguracionEmail configuracion(boolean activo) {
        return ConfiguracionEmail.builder().idConfiguracion(1)
                .smtpHost("127.0.0.1").smtpPort(servidorSmtp.getLocalPort())
                .smtpUsuario("cuenta@example.invalid").smtpPassword(PASSWORD)
                .smtpTls(false).smtpSsl(false).smtpAuth(true).smtpTimeout(500)
                .emailRemitente("cuenta@example.invalid").activo(activo).build();
    }

    private boolean recibioConexion() throws Exception {
        try (Socket ignored = servidorSmtp.accept()) {
            return true;
        } catch (SocketTimeoutException e) {
            return false;
        }
    }

    @Test
    @DisplayName("activo=0: ningún método de envío conecta al SMTP; el log no trae destinatario, servidor ni clave")
    void inactiva_noConecta(CapturedOutput output) throws Exception {
        when(configuracionEmailService.obtenerConfiguracion()).thenReturn(Optional.of(configuracion(false)));

        assertThatThrownBy(() -> emailService.enviarEmail(DESTINO, "asunto", "cuerpo"))
                .isInstanceOf(MessagingException.class).hasMessage(EmailServiceImpl.ENVIO_DESHABILITADO);
        assertThatThrownBy(() -> emailService.enviarEmailHtml(DESTINO, "asunto", "<p>x</p>"))
                .isInstanceOf(MessagingException.class).hasMessage(EmailServiceImpl.ENVIO_DESHABILITADO);
        assertThatThrownBy(() -> emailService.enviarEmailConAdjunto(DESTINO, "a", "c", new byte[]{1}, "f.pdf"))
                .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> emailService.enviarEmailHtmlConAdjunto(DESTINO, "a", "c", new byte[]{1}, "f.pdf"))
                .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> emailService.enviarEmailConMultiplesAdjuntos(DESTINO, "a", "c",
                Map.of("f.pdf", new byte[]{1}))).isInstanceOf(MessagingException.class);
        assertThat(emailService.enviarEmailPrueba(DESTINO)).isFalse();

        assertThat(recibioConexion()).as("no debe haber conexión al servidor SMTP").isFalse();
        assertThat(output).contains("Envío de correo omitido").doesNotContain(PASSWORD);
        assertThat(output.getAll().lines().filter(l -> l.contains("omitido")))
                .hasSize(6)
                .allSatisfy(l -> assertThat(l).doesNotContain(DESTINO).doesNotContain("cuenta@example.invalid")
                        .doesNotContain("127.0.0.1"));
    }

    @Test
    @DisplayName("Sin configuración de email: tampoco conecta")
    void sinConfiguracion_noConecta() throws Exception {
        when(configuracionEmailService.obtenerConfiguracion()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailService.enviarEmail(DESTINO, "asunto", "cuerpo"))
                .isInstanceOf(MessagingException.class).hasMessage(EmailServiceImpl.ENVIO_DESHABILITADO);
        assertThat(recibioConexion()).isFalse();
    }

    @Test
    @DisplayName("Control: con activo=1 sí intenta conectar al servidor de la configuración")
    void activa_conecta() throws Exception {
        when(configuracionEmailService.obtenerConfiguracion()).thenReturn(Optional.of(configuracion(true)));

        Thread envio = new Thread(() -> {
            try {
                emailService.enviarEmail(DESTINO, "asunto", "cuerpo");
            } catch (MessagingException esperado) {
                // el servidor de prueba no habla SMTP
            }
        });
        envio.start();
        servidorSmtp.setSoTimeout(5000);
        try {
            assertThat(recibioConexion()).isTrue();
        } finally {
            envio.join(5000);
        }
    }
}
