package api.astro.whats_orders_manager.config.testguard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-021 — La fábrica de META-INF/spring.factories (test) instala la guardia en cualquier contexto de prueba,
 * incluso uno sin Spring Boot.
 */
@SpringJUnitConfig(TestDataSourceGuardRegisteredTest.Empty.class)
@DisplayName("T-021 — la guardia de DataSource se registra en todo contexto de prueba")
class TestDataSourceGuardRegisteredTest {

    @Configuration
    static class Empty {
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void guardiaRegistrada() {
        AbstractBeanFactory beanFactory = (AbstractBeanFactory) context.getBeanFactory();
        assertThat(beanFactory.getBeanPostProcessors()).anyMatch(TestDataSourceGuard.class::isInstance);
    }
}
