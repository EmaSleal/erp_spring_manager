package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

/**
 * Entidad ConfiguracionFacturacion
 * Almacena la configuración para el módulo de facturación.
 * Solo debe existir un registro activo en la base de datos.
 * 
 * @author Astro Dev Team
 * @version 1.0
 * @since Sprint 2 - Fase 2
 */
@Entity
@Table(name = "configuracion_facturacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ConfiguracionFacturacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * Serie de la factura (ej: F001, FV01, etc.)
     */
    @Size(max = 10, message = "La serie no puede exceder 10 caracteres")
    @Column(name = "serie_factura", length = 10)
    private String serieFactura = "F001";

    /**
     * Número inicial desde donde comenzar la numeración
     */
    @Column(name = "numero_inicial")
    private Integer numeroInicial = 1;

    /**
     * Número actual de factura (auto-incrementable)
     */
    @NotNull(message = "El número actual es obligatorio")
    @Column(name = "numero_actual", nullable = false)
    private Integer numeroActual = 1;

    /**
     * Prefijo opcional para el número de factura (ej: FAC-, INV-)
     */
    @Size(max = 10, message = "El prefijo no puede exceder 10 caracteres")
    @Column(name = "prefijo_factura", length = 10)
    private String prefijoFactura;

    /**
     * Porcentaje de IGV/IVA aplicable
     */
    @NotNull(message = "El IGV es obligatorio")
    @DecimalMin(value = "0.0", message = "El IGV no puede ser negativo")
    @Column(name = "igv", precision = 5, scale = 2, nullable = false)
    private BigDecimal igv = new BigDecimal("18.00");

    /**
     * Código de moneda (ISO 4217: PEN, USD, EUR, etc.)
     */
    @NotNull(message = "La moneda es obligatoria")
    @Size(min = 3, max = 3, message = "La moneda debe ser un código de 3 letras")
    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda = "PEN";

    /**
     * Símbolo de la moneda (S/, $, €, etc.)
     */
    @Size(max = 5, message = "El símbolo no puede exceder 5 caracteres")
    @Column(name = "simbolo_moneda", length = 5)
    private String simboloMoneda = "S/";

    /**
     * Términos y condiciones que aparecerán en la factura
     */
    @Column(name = "terminos_condiciones", columnDefinition = "TEXT")
    private String terminosCondiciones;

    /**
     * Nota de pie de página en la factura
     */
    @Size(max = 500, message = "La nota no puede exceder 500 caracteres")
    @Column(name = "nota_pie_pagina", length = 500)
    private String notaPiePagina;

    /**
     * Indica si el IGV está incluido en el precio del producto
     * true = Precio incluye IGV
     * false = Precio + IGV
     */
    @Column(name = "incluir_igv_en_precio")
    private Boolean incluirIgvEnPrecio = true;

    /**
     * Formato del número de factura
     * Opciones: SERIE-NUMERO, PREFIJO-SERIE-NUMERO, etc.
     */
    @Size(max = 50, message = "El formato no puede exceder 50 caracteres")
    @Column(name = "formato_numero", length = 50)
    private String formatoNumero = "{serie}-{numero}";

    /**
     * Cantidad de decimales para los montos
     */
    @Column(name = "decimales")
    private Integer decimales = 2;

    /**
     * Indica si esta configuración está activa
     * Solo debe haber una configuración activa
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Campos de auditoría - Spring Data JPA
     */
    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private Timestamp createDate;

    @LastModifiedDate
    @Column(name = "updateDate")
    private Timestamp updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @LastModifiedBy
    @Column(name = "updateBy")
    private Integer updateBy;

    /**
     * Callback antes de persistir
     */
    @PrePersist
    protected void onCreate() {
        if (this.activo == null) {
            this.activo = true;
        }
        if (this.numeroActual == null) {
            this.numeroActual = this.numeroInicial != null ? this.numeroInicial : 1;
        }
    }

    /**
     * Genera el próximo número de factura
     * 
     * @return Número de factura formateado (ej: F001-00001)
     */
    public String generarNumeroFactura() {
        String numero = String.format("%05d", numeroActual);
        String resultado = formatoNumero
                .replace("{serie}", serieFactura != null ? serieFactura : "")
                .replace("{prefijo}", prefijoFactura != null ? prefijoFactura : "")
                .replace("{numero}", numero);
        
        return resultado;
    }

    /**
     * Incrementa el número actual de factura
     */
    public void incrementarNumero() {
        if (this.numeroActual != null) {
            this.numeroActual++;
        }
    }

    /**
     * Calcula el IGV de un monto
     * 
     * @param subtotal Monto base
     * @return Monto de IGV
     */
    public BigDecimal calcularIgv(BigDecimal subtotal) {
        if (subtotal == null || igv == null) {
            return BigDecimal.ZERO;
        }
        
        if (incluirIgvEnPrecio) {
            // Si el precio incluye IGV, extraer el IGV del total
            BigDecimal divisor = BigDecimal.ONE.add(igv.divide(new BigDecimal("100"), decimales, RoundingMode.HALF_UP));
            BigDecimal base = subtotal.divide(divisor, decimales, RoundingMode.HALF_UP);
            return subtotal.subtract(base);
        } else {
            // Si el precio no incluye IGV, calcular el IGV sobre el subtotal
            return subtotal.multiply(igv).divide(new BigDecimal("100"), decimales, RoundingMode.HALF_UP);
        }
    }

    /**
     * Calcula el total con IGV
     * 
     * @param subtotal Monto base
     * @return Total con IGV
     */
    public BigDecimal calcularTotal(BigDecimal subtotal) {
        if (subtotal == null) {
            return BigDecimal.ZERO;
        }
        
        if (incluirIgvEnPrecio) {
            // El precio ya incluye IGV
            return subtotal;
        } else {
            // Sumar IGV al subtotal
            BigDecimal montoIgv = calcularIgv(subtotal);
            return subtotal.add(montoIgv);
        }
    }

    /**
     * Método helper para verificar si tiene términos y condiciones
     */
    public boolean tieneTerminos() {
        return terminosCondiciones != null && !terminosCondiciones.trim().isEmpty();
    }

    /**
     * Método helper para verificar si tiene nota de pie de página
     */
    public boolean tieneNotaPie() {
        return notaPiePagina != null && !notaPiePagina.trim().isEmpty();
    }

    /**
     * Override toString para logging y debugging
     */
    @Override
    public String toString() {
        return "ConfiguracionFacturacion{" +
                "id=" + id +
                ", serieFactura='" + serieFactura + '\'' +
                ", numeroActual=" + numeroActual +
                ", igv=" + igv +
                ", moneda='" + moneda + '\'' +
                ", activo=" + activo +
                '}';
    }
}
