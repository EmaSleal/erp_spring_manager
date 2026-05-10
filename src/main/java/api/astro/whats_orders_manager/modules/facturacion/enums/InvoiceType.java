package api.astro.whats_orders_manager.modules.facturacion.enums;

import lombok.Getter;

@Getter
public enum InvoiceType {
    INSTITUCIONAL("Institucional"),
    MAYORISTA("Mayorista");

    private final String displayName;

    InvoiceType(String displayName) {
        this.displayName = displayName;
    }

}
