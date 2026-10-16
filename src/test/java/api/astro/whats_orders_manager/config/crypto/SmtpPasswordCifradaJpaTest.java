package api.astro.whats_orders_manager.config.crypto;

import api.astro.whats_orders_manager.config.startup.StartupConfigurationException;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * T-022 (ADR-026 §2) — La contraseña SMTP se guarda con CredencialCipher (enc:v1:, HACIENDA_CRYPTO_KEY). Al
 * arrancar, SmtpPasswordCifradoRunner migra el texto plano y el formato anterior {enc} (SMTP_ENCRYPTION_KEY).
 * H2; la columna se lee con SQL nativo para ver lo que realmente se guarda. Sin transacción de prueba.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(CredencialCipherConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("T-022 — contraseña SMTP cifrada con CredencialCipher")
class SmtpPasswordCifradaJpaTest {

    /** Clave SMTP heredada SOLO para pruebas (Base64 de 32 bytes). */
    private static final String CLAVE_SMTP_PRUEBA = Base64.getEncoder().encodeToString(
            "t022-clave-smtp-solo-pruebas-32b".getBytes(StandardCharsets.UTF_8));
    private static final String PASSWORD = "app-password-19-chr";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CredencialCipher cipher;
    @Autowired private ConfiguracionEmailRepository repository;

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM configuracion_email");
    }

    private SmtpPasswordCifradoRunner runner(String claveSmtp) {
        return new SmtpPasswordCifradoRunner(jdbcTemplate, cipher, claveSmtp);
    }

    private int insertar(String smtpPassword) {
        jdbcTemplate.update("INSERT INTO configuracion_email (smtp_host, smtp_port, smtp_usuario, smtp_password, "
                + "email_remitente, activo) VALUES ('smtp.example.invalid', 587, 'cuenta@example.invalid', ?, "
                + "'cuenta@example.invalid', FALSE)", smtpPassword);
        return jdbcTemplate.queryForObject("SELECT MAX(id_configuracion) FROM configuracion_email", Integer.class);
    }

    private String columna(int id) {
        return jdbcTemplate.queryForObject(
                "SELECT smtp_password FROM configuracion_email WHERE id_configuracion = ?", String.class, id);
    }

    /** Mismo formato que el converter SMTP anterior (eliminado en T-022): "{enc}" + Base64(IV(12) || cifrado + tag). */
    private static String cifrarLegado(String plano, String claveBase64) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Base64.getDecoder().decode(claveBase64), "AES"),
                new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plano.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(ct, 0, payload, iv.length, ct.length);
        return "{enc}" + Base64.getEncoder().encodeToString(payload);
    }

    @Test
    @DisplayName("(1) Texto plano heredado → tras el arranque queda enc:v1: y JPA lee el mismo valor")
    void textoPlano_quedaCifrado(CapturedOutput output) {
        int id = insertar(PASSWORD);

        assertThat(runner("").migrarContrasenasSmtp()).isEqualTo(1);

        assertThat(columna(id)).startsWith("enc:v1:").doesNotContain(PASSWORD);
        ConfiguracionEmail leida = repository.findById(id).orElseThrow();
        assertThat(leida.getSmtpPassword()).isEqualTo(PASSWORD);
        assertThat(leida.toString()).doesNotContain(PASSWORD).doesNotContain("enc:v1:");
        assertThat(output).contains("1 fila(s) migrada(s)").doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("(2) Valor {enc} con la clave SMTP de prueba → se recifra a enc:v1: y se lee igual")
    void legadoEnc_seRecifra(CapturedOutput output) throws Exception {
        int id = insertar(cifrarLegado(PASSWORD, CLAVE_SMTP_PRUEBA));

        assertThat(runner(CLAVE_SMTP_PRUEBA).migrarContrasenasSmtp()).isEqualTo(1);

        assertThat(columna(id)).startsWith("enc:v1:");
        assertThat(repository.findById(id).orElseThrow().getSmtpPassword()).isEqualTo(PASSWORD);
        assertThat(output).doesNotContain(PASSWORD).doesNotContain(CLAVE_SMTP_PRUEBA);

        // idempotente: un segundo arranque no toca nada
        String cifrado = columna(id);
        assertThat(runner(CLAVE_SMTP_PRUEBA).migrarContrasenasSmtp()).isZero();
        assertThat(columna(id)).isEqualTo(cifrado);
    }

    @Test
    @DisplayName("(3) Valor {enc} sin SMTP_ENCRYPTION_KEY → no arranca con mensaje claro; no modifica nada")
    void legadoEnc_sinClave_falla() throws Exception {
        String legado = cifrarLegado(PASSWORD, CLAVE_SMTP_PRUEBA);
        int id = insertar(legado);
        int plano = insertar("otra-clave-plana");

        StartupConfigurationException e = catchThrowableOfType(StartupConfigurationException.class,
                () -> runner("").migrarContrasenasSmtp());

        assertThat(e).isNotNull();
        assertThat(e.getDescription()).contains("SMTP_ENCRYPTION_KEY").contains("{enc}");
        assertThat(e.getAction()).contains("SMTP_ENCRYPTION_KEY");
        assertThat(columna(id)).isEqualTo(legado);
        assertThat(columna(plano)).isEqualTo("otra-clave-plana");
    }

    @Test
    @DisplayName("Valor {enc} con otra clave SMTP → no arranca, sin mostrar valores")
    void legadoEnc_otraClave_falla() throws Exception {
        insertar(cifrarLegado(PASSWORD, CLAVE_SMTP_PRUEBA));
        String otra = Base64.getEncoder().encodeToString(new byte[32]);

        StartupConfigurationException e = catchThrowableOfType(StartupConfigurationException.class,
                () -> runner(otra).migrarContrasenasSmtp());

        assertThat(e.getDescription()).contains("SMTP_ENCRYPTION_KEY").doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("(4) Sin valores {enc} ni SMTP_ENCRYPTION_KEY → arranca (tabla vacía y con texto plano)")
    void sinLegado_sinClaveSmtp_arranca() {
        assertThatCode(() -> runner("").migrarContrasenasSmtp()).doesNotThrowAnyException();

        insertar(PASSWORD);
        assertThatCode(() -> runner("").migrarContrasenasSmtp()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sin HACIENDA_CRYPTO_KEY y con filas → no arranca nombrando la variable; tabla vacía → arranca")
    void sinClaveDeCifrado() {
        SmtpPasswordCifradoRunner sinClave = new SmtpPasswordCifradoRunner(jdbcTemplate, new CredencialCipher(""), "");
        assertThatCode(sinClave::migrarContrasenasSmtp).doesNotThrowAnyException();

        insertar(PASSWORD);
        StartupConfigurationException e = catchThrowableOfType(StartupConfigurationException.class,
                sinClave::migrarContrasenasSmtp);
        assertThat(e.getDescription()).contains("HACIENDA_CRYPTO_KEY").contains("1 fila");
    }

    @Test
    @DisplayName("Guardar por JPA escribe enc:v1:")
    void jpa_escribeCifrado() {
        ConfiguracionEmail guardada = repository.save(ConfiguracionEmail.builder()
                .smtpHost("smtp.example.invalid").smtpPort(587).smtpUsuario("u@example.invalid")
                .smtpPassword(PASSWORD).emailRemitente("u@example.invalid").activo(false).build());

        assertThat(columna(guardada.getIdConfiguracion())).startsWith("enc:v1:");
        assertThat(repository.findById(guardada.getIdConfiguracion()).orElseThrow().getSmtpPassword())
                .isEqualTo(PASSWORD);
    }
}
