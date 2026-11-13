package api.astro.whats_orders_manager.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fechas y horas de MySQL sin conversión de zona (T-033, Review 1).
 *
 * La URL de producción lleva serverTimezone=UTC y, desde T-033, la JVM corre en America/Costa_Rica. Con eso, el
 * conector convierte cada DATETIME/TIMESTAMP entre la zona de la JVM y UTC: una factura creada a las 00:20 (hora
 * guardada) se leía como las 18:20 del día anterior, y su fecha de emisión y la del asiento cambiaban de día
 * (verificado sobre el dump de producción). Las columnas DATE no se desplazaban.
 *
 * Con preserveInstants=false el conector lee y escribe la hora tal como está en la columna, en todas las rutas
 * (entidades, consultas nativas, procedimientos y JdbcTemplate). Se aplica en código, a cualquier pool MySQL, para no
 * depender de la DB_URL de cada despliegue.
 */
@Slf4j
@Configuration
public class MysqlFechasSinConversionConfig {

    static final String PROPIEDAD = "preserveInstants";

    @Bean
    public static BeanPostProcessor mysqlFechasSinConversion() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof HikariDataSource ds && ds.getJdbcUrl() != null
                        && ds.getJdbcUrl().startsWith("jdbc:mysql:")) {
                    ds.addDataSourceProperty(PROPIEDAD, "false");
                    log.info("Conexión MySQL sin conversión de zona en fechas ({}=false)", PROPIEDAD);
                }
                return bean;
            }
        };
    }
}
