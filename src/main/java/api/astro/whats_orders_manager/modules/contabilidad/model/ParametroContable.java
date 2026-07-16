package api.astro.whats_orders_manager.modules.contabilidad.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps a named accounting-event key to a chart-of-accounts entry.
 * Decouples journal-entry generation from hardcoded account IDs.
 *
 * Required keys (must be configured before CPP/pago hooks fire):
 *   CPP_CUENTA_COMPRAS, CPP_CUENTA_POR_PAGAR, CPP_IVA_CREDITO_FISCAL,
 *   CPP_BANCO_TRANSFERENCIA, CPP_BANCO_CHEQUE, CPP_BANCO_EFECTIVO,
 *   CPP_BANCO_SINPE, CPP_BANCO_DEPOSITO
 */
@Entity
@Table(name = "parametros_contables", indexes = {
    @Index(name = "idx_param_clave", columnList = "clave", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametroContable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String clave;

    @Column(length = 300)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_contable_id", nullable = false)
    private CuentaContable cuentaContable;
}
