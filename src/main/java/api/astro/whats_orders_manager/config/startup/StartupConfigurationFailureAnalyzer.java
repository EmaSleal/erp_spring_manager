package api.astro.whats_orders_manager.config.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * T-021 — Presenta una {@link StartupConfigurationException} como descripción + acción, sin stacktrace.
 * Registrado en META-INF/spring.factories.
 */
public class StartupConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<StartupConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, StartupConfigurationException cause) {
        return new FailureAnalysis(cause.getDescription(), cause.getAction(), cause);
    }
}
