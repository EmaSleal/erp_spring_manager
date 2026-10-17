package api.astro.whats_orders_manager.config.crypto;

import api.astro.whats_orders_manager.config.startup.StartupConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * T-022 (ADR-026 §2) — Al arrancar, pasa configuracion_email.smtp_password al formato de {@link CredencialCipher}
 * ({@code enc:v1:}, clave HACIENDA_CRYPTO_KEY), el mismo de las credenciales de Hacienda.
 *
 * <ul>
 *   <li>Texto plano heredado → se cifra.</li>
 *   <li>Formato heredado {@code {enc}} (el del converter SMTP anterior, eliminado en T-022: Base64(IV || cifrado + tag),
 *       AES-256-GCM, clave SMTP_ENCRYPTION_KEY) → se descifra con SMTP_ENCRYPTION_KEY y se vuelve a cifrar.
 *       Esta clase es el único lugar que conoce ese formato.</li>
 *   <li>Hay algún valor {@code {enc}} y falta SMTP_ENCRYPTION_KEY (o es inválida o no corresponde): la
 *       aplicación no arranca. Sin valores {@code {enc}}, SMTP_ENCRYPTION_KEY no hace falta.</li>
 *   <li>Falta HACIENDA_CRYPTO_KEY y la tabla tiene filas: la aplicación no arranca (no se podría cifrar ni
 *       descifrar la contraseña). Con la tabla vacía arranca.</li>
 * </ul>
 * Se actualiza por SQL nativo (el dirty checking de Hibernate no vería el cambio). Solo selecciona columnas
 * explícitas y solo registra cuántas filas se migraron, nunca valores.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SmtpPasswordCifradoRunner implements ApplicationRunner {

    static final String PREFIJO_LEGADO = "{enc}";
    static final String SMTP_KEY_ENV_VAR = "SMTP_ENCRYPTION_KEY";

    private static final String TRANSFORMACION_LEGADA = "AES/GCM/NoPadding";
    private static final int IV_LEGADO_BYTES = 12;
    private static final int TAG_LEGADO_BITS = 128;

    private final JdbcTemplate jdbcTemplate;
    private final CredencialCipher cipher;
    private final String claveSmtpLegada;

    public SmtpPasswordCifradoRunner(JdbcTemplate jdbcTemplate, CredencialCipher cipher,
                                     @Value("${smtp.encryption.key:}") String claveSmtpLegada) {
        this.jdbcTemplate = jdbcTemplate;
        this.cipher = cipher;
        this.claveSmtpLegada = claveSmtpLegada;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrarContrasenasSmtp();
    }

    /**
     * @return cantidad de filas migradas
     * @throws StartupConfigurationException si falta una clave necesaria o un valor heredado no se puede descifrar
     */
    @Transactional
    public int migrarContrasenasSmtp() {
        List<Object[]> registros = jdbcTemplate.query(
                "SELECT id_configuracion, smtp_password FROM configuracion_email",
                (rs, n) -> new Object[]{rs.getInt("id_configuracion"), rs.getString("smtp_password")});

        if (!cipher.isConfigured()) {
            if (!registros.isEmpty()) {
                throw new StartupConfigurationException(
                        "Falta la variable de entorno " + CredencialCipher.KEY_ENV_VAR + " y configuracion_email tiene "
                                + registros.size() + " fila(s): sin ella la contraseña SMTP no se puede cifrar ni "
                                + "descifrar.",
                        "Defina " + CredencialCipher.KEY_ENV_VAR + " (la misma clave que cifra las credenciales de "
                                + "Hacienda, respaldada aparte de la base). Si se perdió, genere una con 'openssl "
                                + "rand -base64 32' y vuelva a ingresar la contraseña SMTP desde la pantalla de "
                                + "configuración de email.");
            }
            return 0;
        }

        List<Object[]> pendientes = new ArrayList<>();
        SecretKeySpec claveLegada = null;
        for (Object[] registro : registros) {
            String valor = (String) registro[1];
            if (valor == null || CredencialCipher.isEncrypted(valor)) {
                continue;
            }
            String textoPlano = valor;
            if (valor.startsWith(PREFIJO_LEGADO)) {
                if (claveLegada == null) {
                    claveLegada = claveLegada();
                }
                textoPlano = descifrarLegado(valor, claveLegada, (Integer) registro[0]);
            }
            pendientes.add(new Object[]{cipher.encrypt(textoPlano), registro[0]});
        }

        if (!pendientes.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "UPDATE configuracion_email SET smtp_password = ? WHERE id_configuracion = ?", pendientes);
            log.info("Contraseña SMTP cifrada al arrancar (enc:v1:): {} fila(s) migrada(s).", pendientes.size());
        }
        return pendientes.size();
    }

    private SecretKeySpec claveLegada() {
        if (claveSmtpLegada == null || claveSmtpLegada.isBlank()) {
            throw new StartupConfigurationException(
                    "configuracion_email tiene una contraseña SMTP en el formato cifrado anterior ({enc}) y falta "
                            + "la variable de entorno " + SMTP_KEY_ENV_VAR + " para descifrarla.",
                    "Defina " + SMTP_KEY_ENV_VAR + " con la clave con la que se cifró (solo hace falta en este "
                            + "arranque: la contraseña se vuelve a cifrar con " + CredencialCipher.KEY_ENV_VAR
                            + "). Si se perdió, la contraseña no se puede recuperar: hay que reemplazar ese valor en "
                            + "la base y volver a ingresarla desde la pantalla de configuración de email.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(claveSmtpLegada.trim());
        } catch (IllegalArgumentException e) {
            throw claveLegadaInvalida(SMTP_KEY_ENV_VAR + " no es Base64 válido.");
        }
        if (bytes.length != 32) {
            throw claveLegadaInvalida(SMTP_KEY_ENV_VAR + " debe decodificar a exactamente 32 bytes y decodifica a "
                    + bytes.length + ".");
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private static String descifrarLegado(String valor, SecretKeySpec clave, Integer id) {
        try {
            byte[] payload = Base64.getDecoder().decode(valor.substring(PREFIJO_LEGADO.length()));
            if (payload.length < IV_LEGADO_BYTES + TAG_LEGADO_BITS / 8) {
                throw new IllegalArgumentException("contenido demasiado corto");
            }
            Cipher legado = Cipher.getInstance(TRANSFORMACION_LEGADA);
            legado.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_LEGADO_BITS, payload, 0, IV_LEGADO_BYTES));
            byte[] plano = legado.doFinal(payload, IV_LEGADO_BYTES, payload.length - IV_LEGADO_BYTES);
            return new String(plano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw claveLegadaInvalida("La contraseña SMTP {enc} de configuracion_email (id " + id + ") no se pudo "
                    + "descifrar con " + SMTP_KEY_ENV_VAR + ": el valor está alterado o se cifró con otra clave.");
        }
    }

    private static StartupConfigurationException claveLegadaInvalida(String descripcion) {
        return new StartupConfigurationException(descripcion,
                "Defina " + SMTP_KEY_ENV_VAR + " con la clave original (Base64 de 32 bytes). Si se perdió, la "
                        + "contraseña no se puede recuperar: hay que reemplazar ese valor en la base y volver a "
                        + "ingresarla desde la pantalla de configuración de email.");
    }
}
