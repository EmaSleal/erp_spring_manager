package api.astro.whats_orders_manager.config.testguard;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-021 — La guardia rechaza un DataSource remoto SIN abrir conexión (los tipos conocidos se leen por getter) y
 * acepta H2 y MySQL en localhost. 10.255.255.1 es una IP no enrutable: si se intentara conectar, la prueba
 * quedaría colgada en lugar de fallar rápido.
 */
@DisplayName("T-021 — guardia de DataSource en pruebas")
class TestDataSourceGuardTest {

    private static final String REMOTE = "jdbc:mysql://10.255.255.1:3306/x";

    private static HikariDataSource hikari(String url) {
        HikariDataSource ds = new HikariDataSource(); // sin pool hasta el primer getConnection()
        ds.setJdbcUrl(url);
        return ds;
    }

    @Test
    @DisplayName("Hikari a 10.255.255.1 → rechazado con mensaje explícito, sin conectar")
    void hikariRemoto_rechazado() {
        try (HikariDataSource ds = hikari(REMOTE)) {
            assertThatThrownBy(() -> TestDataSourceGuard.verify(ds, "dataSource"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Test datasource guard")
                    .hasMessageContaining("URL no permitida: " + REMOTE);
            assertThat(ds.isRunning()).as("no se abrió el pool").isFalse();
        }
    }

    @Test
    @DisplayName("DriverManagerDataSource / SimpleDriverDataSource remotos → rechazados")
    void driverBasedRemoto_rechazado() {
        assertThatThrownBy(() -> TestDataSourceGuard.verify(new DriverManagerDataSource(REMOTE), "ds"))
                .hasMessageContaining("URL no permitida");
        SimpleDriverDataSource simple = new SimpleDriverDataSource();
        simple.setUrl("jdbc:mysql://192.168.100.93:3306/facturas");
        assertThatThrownBy(() -> TestDataSourceGuard.verify(simple, "ds")).hasMessageContaining("URL no permitida");
    }

    @Test
    @DisplayName("Un proxy delegante se desenvuelve antes de leer la URL")
    void proxyDelegante_seDesenvuelve() {
        DataSource proxy = new LazyConnectionDataSourceProxy(new DriverManagerDataSource(REMOTE));
        assertThatThrownBy(() -> TestDataSourceGuard.verify(proxy, "ds")).hasMessageContaining("URL no permitida");
    }

    @Test
    @DisplayName("H2, MySQL en 127.0.0.1/localhost y EmbeddedDatabase → aceptados")
    void locales_aceptados() {
        assertThatCode(() -> TestDataSourceGuard.verify(new DriverManagerDataSource("jdbc:h2:mem:x"), "ds"))
                .doesNotThrowAnyException();
        assertThatCode(() -> TestDataSourceGuard.verify(
                new DriverManagerDataSource("jdbc:mysql://127.0.0.1:3306/t021"), "ds")).doesNotThrowAnyException();
        try (HikariDataSource ds = hikari("jdbc:mysql://localhost/t021?useSSL=false")) {
            assertThatCode(() -> TestDataSourceGuard.verify(ds, "ds")).doesNotThrowAnyException();
        }
        var embedded = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        try {
            assertThatCode(() -> TestDataSourceGuard.verify(embedded, "ds")).doesNotThrowAnyException();
        } finally {
            embedded.shutdown();
        }
    }

    @Test
    @DisplayName("Hosts parecidos a localhost no pasan")
    void hostsParecidos_rechazados() {
        assertThat(TestDataSourceGuard.isAllowed("jdbc:mysql://localhost.example.com/x")).isFalse();
        assertThat(TestDataSourceGuard.isAllowed("jdbc:mysql://127.0.0.10/x")).isFalse();
        assertThat(TestDataSourceGuard.isAllowed("jdbc:mysql://127.0.0.1:3307/x")).isTrue();
        assertThat(TestDataSourceGuard.isAllowed(null)).isFalse();
    }

    @Test
    @DisplayName("Tipo desconocido → URL por metadatos de conexión")
    void tipoDesconocido_usaMetadatos() throws Exception {
        DataSource remoto = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(remoto.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getURL()).thenReturn("jdbc:mysql://user:secreto@10.255.255.1/x");

        assertThatThrownBy(() -> TestDataSourceGuard.verify(remoto, "ds"))
                .hasMessageContaining("jdbc:mysql://***@10.255.255.1/x")
                .hasMessageNotContaining("secreto");
    }

    @Test
    @DisplayName("Enmascara credenciales en la URL")
    void mask() {
        assertThat(TestDataSourceGuard.mask("jdbc:mysql://10.0.0.1/x?user=root&password=abc"))
                .isEqualTo("jdbc:mysql://10.0.0.1/x?user=***&password=***");
    }

    @Configuration
    static class RemoteDataSourceConfig {
        @Bean
        HikariDataSource dataSource() {
            return hikari(REMOTE);
        }
    }

    @Configuration
    static class LocalDataSourceConfig {
        @Bean
        DriverManagerDataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:t021guard");
        }
    }

    @Test
    @DisplayName("Como BeanPostProcessor: el contexto con DataSource remoto no arranca; el local sí")
    void comoBeanPostProcessor() {
        new ApplicationContextRunner()
                .withInitializer(ctx -> ctx.getBeanFactory().addBeanPostProcessor(new TestDataSourceGuard()))
                .withUserConfiguration(RemoteDataSourceConfig.class)
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("URL no permitida: " + REMOTE));

        new ApplicationContextRunner()
                .withInitializer(ctx -> ctx.getBeanFactory().addBeanPostProcessor(new TestDataSourceGuard()))
                .withUserConfiguration(LocalDataSourceConfig.class)
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(DataSource.class));
    }

    @Test
    @DisplayName("La fábrica registrada da customizers iguales (no rompe la caché de contextos)")
    void customizersIguales() {
        var factory = new TestDataSourceGuardContextCustomizerFactory();
        assertThat(factory.createContextCustomizer(Object.class, java.util.List.of()))
                .isEqualTo(factory.createContextCustomizer(String.class, java.util.List.of()));
    }
}
