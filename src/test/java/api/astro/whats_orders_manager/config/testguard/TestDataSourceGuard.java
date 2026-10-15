package api.astro.whats_orders_manager.config.testguard;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * T-021 — Guardia de pruebas: ningún contexto de prueba puede resolver un DataSource que no sea H2 o MySQL en
 * 127.0.0.1/localhost. Lee la URL sin conectarse cuando el tipo lo permite (Hikari, DriverManager/SimpleDriver,
 * EmbeddedDatabase); solo para tipos desconocidos consulta los metadatos de una conexión.
 */
public class TestDataSourceGuard implements BeanPostProcessor {

    private static final Pattern ALLOWED = Pattern.compile(
            "^jdbc:h2:.*|^jdbc:mysql://(127\\.0\\.0\\.1|localhost)([:/?].*)?$", Pattern.CASE_INSENSITIVE);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            verify(dataSource, beanName);
        }
        return bean;
    }

    static void verify(DataSource dataSource, String beanName) {
        DataSource target = unwrap(dataSource);
        if (target instanceof EmbeddedDatabase) {
            return; // @AutoConfigureTestDatabase: base embebida en memoria
        }
        String url = urlOf(target);
        if (url == null || !ALLOWED.matcher(url).matches()) {
            throw new IllegalStateException("Test datasource guard: el DataSource '" + beanName + "' ("
                    + target.getClass().getName() + ") apunta a una URL no permitida: " + mask(url)
                    + ". Las pruebas solo pueden usar jdbc:h2: o jdbc:mysql://127.0.0.1 / jdbc:mysql://localhost.");
        }
    }

    static boolean isAllowed(String url) {
        return url != null && ALLOWED.matcher(url).matches();
    }

    private static DataSource unwrap(DataSource dataSource) {
        DataSource current = dataSource;
        while (current instanceof DelegatingDataSource delegating && delegating.getTargetDataSource() != null) {
            current = delegating.getTargetDataSource();
        }
        return current;
    }

    private static String urlOf(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            return hikari.getJdbcUrl();
        }
        if (dataSource instanceof AbstractDriverBasedDataSource driverBased) {
            return driverBased.getUrl(); // DriverManagerDataSource, SimpleDriverDataSource
        }
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        } catch (SQLException e) {
            throw new IllegalStateException("Test datasource guard: no se pudo leer la URL del DataSource "
                    + dataSource.getClass().getName(), e);
        }
    }

    /** Oculta usuario:clave@ y parámetros password/user de la URL. */
    static String mask(String url) {
        if (url == null) {
            return "<sin URL>";
        }
        return url.replaceAll("//[^/@]*@", "//***@")
                .replaceAll("(?i)((password|pwd|user|username)=)[^&;]*", "$1***");
    }
}
