package api.astro.whats_orders_manager.models.dto;

import api.astro.whats_orders_manager.models.enums.CategoriaParametro;
import api.astro.whats_orders_manager.models.enums.TipoDatoParametro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * PARAMETRO SISTEMA DTO
 * WhatsApp Orders Manager
 * ============================================================================
 * Data Transfer Object para parámetros del sistema.
 * 
 * Representa parámetros configurables del sistema almacenados como clave-valor
 * con información de tipo de dato y categoría.
 * ============================================================================
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParametroSistemaDTO {

    /**
     * ID del parámetro
     */
    private Integer idParametro;

    /**
     * Clave única del parámetro
     * Ejemplo: "sistema.nombre", "factura.iva_predeterminado"
     */
    private String clave;

    /**
     * Valor del parámetro (almacenado como texto)
     */
    private String valor;

    /**
     * Tipo de dato del valor
     */
    private TipoDatoParametro tipoDato;

    /**
     * Nombre del tipo de dato (para display)
     */
    private String tipoDatoNombre;

    /**
     * Categoría del parámetro
     */
    private CategoriaParametro categoria;

    /**
     * Nombre de la categoría (para display)
     */
    private String categoriaNombre;

    /**
     * Descripción del parámetro
     */
    private String descripcion;

    /**
     * Indica si el parámetro puede ser editado por el usuario
     */
    private Boolean editable;

    /**
     * Indica si el valor es válido para el tipo de dato (solo lectura)
     */
    private Boolean valorValido;
}
