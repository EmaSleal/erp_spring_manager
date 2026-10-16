package api.astro.whats_orders_manager.config.testguard;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import java.util.List;

/**
 * T-021 — Registra {@link TestDataSourceGuard} en TODOS los contextos de prueba de Spring (@SpringBootTest,
 * @DataJpaTest, @WebMvcTest...). Registrado en src/test/resources/META-INF/spring.factories.
 */
public class TestDataSourceGuardContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        return new GuardCustomizer();
    }

    /** Sin estado: todas las instancias son iguales para no romper la caché de contextos. */
    static final class GuardCustomizer implements ContextCustomizer {

        @Override
        public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration config) {
            context.getBeanFactory().addBeanPostProcessor(new TestDataSourceGuard());
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof GuardCustomizer;
        }

        @Override
        public int hashCode() {
            return GuardCustomizer.class.hashCode();
        }
    }
}
