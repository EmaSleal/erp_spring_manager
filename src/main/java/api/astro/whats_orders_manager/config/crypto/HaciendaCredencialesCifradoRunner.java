package api.astro.whats_orders_manager.config.crypto;

import api.astro.whats_orders_manager.config.startup.StartupConfigurationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * T-021 — Al arrancar, cifra las credenciales de configuracion_hacienda que siguen en texto plano.
 *
 * <ul>
 *   <li>Sin {@code HACIENDA_CRYPTO_KEY} y con filas en la tabla (cifradas o no): la aplicación no arranca.</li>
 *   <li>Sin clave y con la tabla vacía: arranca y registra un WARN.</li>
 *   <li>Con clave: cada valor no nulo sin prefijo {@code enc:v1:} se cifra y se guarda por SQL nativo (el dirty
 *       checking de Hibernate no lo detectaría: el valor descifrado es el mismo).</li>
 * </ul>
 * Solo selecciona columnas explícitas y solo registra conteos, nunca valores.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HaciendaCredencialesCifradoRunner implements ApplicationRunner {

    static final String[] COLUMNAS = {"password_hacienda", "pin_certificado", "access_token", "refresh_token"};

    private final JdbcTemplate jdbcTemplate;
    private final CredencialCipher cipher;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        cifrarCredencialesEnTextoPlano();
    }

    /**
     * @return cantidad de campos cifrados
     * @throws StartupConfigurationException si falta la clave y la tabla tiene filas
     */
    @Transactional
    public int cifrarCredencialesEnTextoPlano() {
        Long filas = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM configuracion_hacienda", Long.class);
        long total = filas == null ? 0 : filas;

        if (!cipher.isConfigured()) {
            if (total > 0) {
                throw new StartupConfigurationException(
                        "Falta la variable de entorno " + CredencialCipher.KEY_ENV_VAR + " y configuracion_hacienda "
                                + "tiene " + total + " fila(s): sin ella las credenciales de Hacienda no se pueden "
                                + "cifrar ni descifrar.",
                        "Defina " + CredencialCipher.KEY_ENV_VAR + " con la clave original (respaldada aparte de la "
                                + "base). Si se perdió, las credenciales cifradas no se pueden recuperar: genere una "
                                + "clave nueva con 'openssl rand -base64 32' y vuelva a ingresar contraseña y PIN de "
                                + "Hacienda (ver database/README.md).");
            }
            log.warn("{} no está definida; configuracion_hacienda está vacía. Defínala antes de registrar "
                    + "credenciales de Hacienda.", CredencialCipher.KEY_ENV_VAR);
            return 0;
        }

        List<Object[]> pendientes = new ArrayList<>();
        int campos = 0;
        List<Object[]> registros = jdbcTemplate.query(
                "SELECT id, password_hacienda, pin_certificado, access_token, refresh_token FROM configuracion_hacienda",
                (rs, n) -> new Object[]{rs.getLong("id"), rs.getString("password_hacienda"),
                        rs.getString("pin_certificado"), rs.getString("access_token"), rs.getString("refresh_token")});
        for (Object[] registro : registros) {
            boolean cambio = false;
            Object[] valores = new Object[COLUMNAS.length + 1];
            for (int i = 0; i < COLUMNAS.length; i++) {
                String valor = (String) registro[i + 1];
                if (valor != null && !CredencialCipher.isEncrypted(valor)) {
                    valor = cipher.encrypt(valor);
                    cambio = true;
                    campos++;
                }
                valores[i] = valor;
            }
            if (cambio) {
                valores[COLUMNAS.length] = registro[0];
                pendientes.add(valores);
            }
        }

        if (!pendientes.isEmpty()) {
            jdbcTemplate.batchUpdate("UPDATE configuracion_hacienda SET password_hacienda = ?, pin_certificado = ?, "
                    + "access_token = ?, refresh_token = ? WHERE id = ?", pendientes);
            log.info("Credenciales de Hacienda cifradas al arrancar: {} campo(s) en {} fila(s).",
                    campos, pendientes.size());
        }
        return campos;
    }
}
