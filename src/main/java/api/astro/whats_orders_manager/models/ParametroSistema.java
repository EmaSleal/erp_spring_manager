package api.astro.whats_orders_manager.models;

import api.astro.whats_orders_manager.models.enums.CategoriaParametro;
import api.astro.whats_orders_manager.models.enums.TipoDatoParametro;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entidad que almacena parámetros configurables del sistema en formato clave-valor.
 * Permite configurar valores del sistema sin cambiar código.
 * 
 * Ejemplos:
 * - sistema.nombre = "WhatsApp Orders Manager"
 * - factura.dias_antes_vencimiento_alerta = "3"
 * - whatsapp.mensajes_automaticos = "true"
 */
@Entity
@Table(name = "parametro_sistema",
       uniqueConstraints = @UniqueConstraint(columnNames = "clave"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ParametroSistema implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Integer idParametro;

    // ==================== IDENTIFICACIÓN ====================
    
    @Column(name = "clave", nullable = false, unique = true, length = 100)
    private String clave;

    @Column(name = "valor", columnDefinition = "TEXT")
    private String valor;

    // ==================== METADATOS ====================
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dato", length = 20)
    private TipoDatoParametro tipoDato = TipoDatoParametro.STRING;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 50)
    private CategoriaParametro categoria;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Builder.Default
    @Column(name = "editable")
    private Boolean editable = true;

    // ==================== AUDITORÍA ====================
    
    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @LastModifiedBy
    @Column(name = "update_by")
    private Integer updateBy;

    @LastModifiedDate
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    // ==================== MÉTODOS DE CONVERSIÓN ====================
    
    /**
     * Obtiene el valor como String (valor original).
     * 
     * @return Valor como String
     */
    public String getValorAsString() {
        return valor != null ? valor : "";
    }

    /**
     * Obtiene el valor como Integer.
     * 
     * @return Valor como Integer, null si no es válido
     */
    public Integer getValorAsInteger() {
        try {
            return valor != null ? Integer.parseInt(valor.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Obtiene el valor como Long.
     * 
     * @return Valor como Long, null si no es válido
     */
    public Long getValorAsLong() {
        try {
            return valor != null ? Long.parseLong(valor.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Obtiene el valor como Boolean.
     * Acepta: "true", "false", "1", "0", "sí", "no", "yes", "no"
     * 
     * @return Valor como Boolean, null si no es válido
     */
    public Boolean getValorAsBoolean() {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        String v = valor.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "sí".equals(v) || "yes".equals(v);
    }

    /**
     * Obtiene el valor como BigDecimal.
     * 
     * @return Valor como BigDecimal, null si no es válido
     */
    public BigDecimal getValorAsBigDecimal() {
        try {
            return valor != null ? new BigDecimal(valor.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Obtiene el valor como LocalDate.
     * Formato esperado: yyyy-MM-dd
     * 
     * @return Valor como LocalDate, null si no es válido
     */
    public LocalDate getValorAsDate() {
        try {
            return valor != null ? LocalDate.parse(valor.trim(), DateTimeFormatter.ISO_LOCAL_DATE) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== MÉTODOS DE ASIGNACIÓN ====================
    
    /**
     * Establece el valor desde un Integer.
     * 
     * @param valorInt Valor Integer
     */
    public void setValorFromInteger(Integer valorInt) {
        this.valor = valorInt != null ? valorInt.toString() : null;
        this.tipoDato = TipoDatoParametro.INTEGER;
    }

    /**
     * Establece el valor desde un Boolean.
     * 
     * @param valorBool Valor Boolean
     */
    public void setValorFromBoolean(Boolean valorBool) {
        this.valor = valorBool != null ? valorBool.toString() : null;
        this.tipoDato = TipoDatoParametro.BOOLEAN;
    }

    /**
     * Establece el valor desde un BigDecimal.
     * 
     * @param valorDecimal Valor BigDecimal
     */
    public void setValorFromBigDecimal(BigDecimal valorDecimal) {
        this.valor = valorDecimal != null ? valorDecimal.toString() : null;
        this.tipoDato = TipoDatoParametro.DECIMAL;
    }

    /**
     * Establece el valor desde un LocalDate.
     * 
     * @param fecha Valor LocalDate
     */
    public void setValorFromDate(LocalDate fecha) {
        this.valor = fecha != null ? fecha.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
        this.tipoDato = TipoDatoParametro.DATE;
    }

    // ==================== MÉTODOS DE VALIDACIÓN ====================
    
    /**
     * Verifica si el parámetro es válido (tiene clave y valor).
     * 
     * @return true si es válido
     */
    public boolean isValido() {
        return clave != null && !clave.trim().isEmpty() &&
               valor != null && !valor.trim().isEmpty();
    }

    /**
     * Verifica si el valor puede ser convertido al tipo especificado.
     * 
     * @return true si la conversión es válida
     */
    public boolean isValorValidoParaTipo() {
        if (valor == null || tipoDato == null) {
            return false;
        }

        return switch (tipoDato) {
            case INTEGER -> getValorAsInteger() != null;
            case LONG -> getValorAsLong() != null;
            case BOOLEAN -> getValorAsBoolean() != null;
            case DECIMAL -> getValorAsBigDecimal() != null;
            case DATE -> getValorAsDate() != null;
            case STRING -> true; // Siempre válido
        };
    }

    /**
     * Obtiene el nombre amigable de la categoría.
     * 
     * @return Nombre de la categoría
     */
    public String getNombreCategoria() {
        return categoria != null ? categoria.getNombre() : "Sin categoría";
    }

    @Override
    public String toString() {
        return "ParametroSistema{" +
                "clave='" + clave + '\'' +
                ", valor='" + valor + '\'' +
                ", tipoDato=" + tipoDato +
                ", categoria=" + categoria +
                ", editable=" + editable +
                '}';
    }
}
