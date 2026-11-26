package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPagoFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para el detalle completo de una factura.
 * Incluye cliente, líneas de factura y pagos sin referencias circulares.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since 2026-01-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturaDetalleDTO {
    
    private Integer idFactura;
    private String numeroFactura;
    private String serie;
    
    // Información del cliente
    private ClienteSimpleDTO cliente;
    
    // Moneda
    private MonedaFE monedaFE;
    private BigDecimal tipoCambio;

    // Montos
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;
    
    // Tipo y fechas
    private InvoiceType tipoFactura;
    private Date fechaPago;
    private Date fechaEntrega;
    private Date fechaVencimiento;
    private LocalDate fechaEmision;
    
    /** T-057: régimen sellado al crear la factura; la UI oculta «Enviar a Hacienda» si es SIMPLIFICADO. */
    private Regimen regimenEmisor;

    // Estado y entregas
    private Boolean entregado;
    private EstadoPagoFactura estadoPago;
    
    // Descripción
    private String descripcion;
    
    // Líneas de la factura
    @Builder.Default
    private List<LineaFacturaDTO> lineas = new ArrayList<>();
    
    // Pagos aplicados
    @Builder.Default
    private List<PagoDTO> pagos = new ArrayList<>();
    
    // Cálculos
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    /** Notas de crédito parciales APLICADAS (T-047). */
    private BigDecimal totalAcreditado;
    /** La suma de las notas parciales alcanzó el total: saldo 0, factura sin anular (T-047). */
    private Boolean acreditadaTotalmente;
    
    // Facturación electrónica: comprobante vigente (ADR-031; null si no tiene). Solo estos datos, nunca la entidad
    private Long idComprobante;
    private EstadoComprobante estadoComprobante;
    /** Texto, color de Bootstrap e ícono del estado, como los define EstadoComprobante. */
    private String estadoComprobanteDescripcion;
    private String estadoComprobanteColor;
    private String estadoComprobanteIcono;
    private String claveComprobante;
    /** ERROR_HACIENDA tras el máximo de consultas seguidas con "error": revisar el comprobante en ATV. */
    private Boolean revisarEnAtv;

    // T-044 (P-099): ambiente del comprobante vigente (SANDBOX o PRODUCCION), si coincide con el de la configuración
    // activa y, si no, el motivo fijo que deja «Reenviar» y «Consultar estado» deshabilitados
    private String ambienteComprobante;
    private Boolean ambienteComprobanteCoincide;
    private String motivoAmbienteComprobante;

    // Factura sustituta (ADR-045): solo cuando procede la acción, y los vínculos con la otra factura
    private Boolean puedeEmitirSustituta;
    /** T-044: «La factura se emitió en el ambiente X; la configuración activa es Y» si solo lo impide el ambiente; si no, null. */
    private String motivoSustitutaNoDisponible;
    /** Factura anulada que esta sustituye, con el número de la nota que la anuló; null si no es sustituta. */
    private SustitucionRefDTO sustituye;
    /** Sustituta no anulada de esta factura; null si no tiene. */
    private SustitucionRefDTO sustituidaPor;
    /** T-046 (ADR-050): fecha del asiento de venta de la sustituta; null si la factura no es sustituta. */
    private LocalDate fechaContable;
    /** T-046 (ADR-050): mes (AAAA-MM) al que se atribuye el IVA de la sustituta; null si no es sustituta. */
    private String periodoFiscalAtribucion;

    // Nota de crédito (ADR-047 §2, T-042): la regla la decide el servidor. puedeEmitirNotaCredito habilita el botón;
    // motivoNotaCreditoNoDisponible lo deja deshabilitado con ese texto; con ambos vacíos no hay botón.
    private Boolean puedeEmitirNotaCredito;
    private String motivoNotaCreditoNoDisponible;
    /**
     * T-047 (ADR-051): modalidades que admite el modal. puedeEmitirNotaTotal: 01, 06 total o 13; puedeEmitirNotaParcial: 02, 06 parcial o 14.
     * motivoNotaTotalNoDisponible explica por qué no procede la total cuando la parcial sí (notas parciales previas).
     */
    private Boolean puedeEmitirNotaTotal;
    private String motivoNotaTotalNoDisponible;
    private Boolean puedeEmitirNotaParcial;
    /** TotalComprobante del comprobante ACEPTADO; null si no se calcula (la factura no admite nota parcial). */
    private BigDecimal notaCreditoTotalComprobante;
    /** Suma de las notas APLICADAS y EN_TRAMITE de la factura. */
    private BigDecimal notaCreditoTotalAcreditado;
    /** TotalComprobante menos lo acreditado: tope de la nota parcial nueva. */
    private BigDecimal notaCreditoDisponible;
    /** Por línea de factura: total, ya acreditado y disponible (con IVA incluido, moneda de la factura). */
    private List<LineaNotaDisponibleDTO> notaCreditoLineas;
    /** T-046: mes (AAAA-MM) del comprobante ACEPTADO vigente, al que la nota 13 atribuye el IVA; null si no lo hay. */
    private String periodoFacturaOriginal;

    /**
     * T-055 (ADR-070): reenvío del comprobante por correo: id y estado del comprobante vigente, correo por defecto del
     * receptor y si el usuario puede reenviar. Null si la factura no tiene comprobante vigente.
     */
    private api.astro.whats_orders_manager.modules.facturacion.correo.dto.CorreoDetalleDTO correo;

    // Auditoría
    private Integer createBy;
    private Integer updateBy;
    private Date updateDate;
}
