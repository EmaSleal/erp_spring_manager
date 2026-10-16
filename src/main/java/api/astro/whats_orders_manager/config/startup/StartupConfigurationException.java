package api.astro.whats_orders_manager.config.startup;

/**
 * T-021 — Configuración de arranque ausente o inválida. Lleva una descripción y una acción legibles que
 * {@link StartupConfigurationFailureAnalyzer} muestra en lugar de un stacktrace. Los mensajes nombran
 * variables de entorno, nunca sus valores.
 */
public class StartupConfigurationException extends RuntimeException {

    private final String action;

    public StartupConfigurationException(String description, String action) {
        super(description);
        this.action = action;
    }

    public String getDescription() {
        return getMessage();
    }

    public String getAction() {
        return action;
    }
}
