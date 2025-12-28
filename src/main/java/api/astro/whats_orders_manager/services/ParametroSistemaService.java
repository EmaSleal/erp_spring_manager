package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.ParametroSistema;
import api.astro.whats_orders_manager.models.enums.CategoriaParametro;
import api.astro.whats_orders_manager.models.enums.TipoDatoParametro;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar parámetros del sistema
 */
public interface ParametroSistemaService {
    
    /**
     * Obtiene un parámetro por su clave
     * @param clave Clave del parámetro
     * @return Optional con el parámetro si existe
     */
    Optional<ParametroSistema> obtenerPorClave(String clave);
    
    /**
     * Obtiene todos los parámetros de una categoría
     * @param categoria Categoría de parámetros
     * @return Lista de parámetros
     */
    List<ParametroSistema> obtenerPorCategoria(CategoriaParametro categoria);
    
    /**
     * Obtiene todos los parámetros editables
     * @return Lista de parámetros editables
     */
    List<ParametroSistema> obtenerEditables();
    
    /**
     * Obtiene todos los parámetros
     * @return Lista de todos los parámetros
     */
    List<ParametroSistema> obtenerTodos();
    
    /**
     * Guarda o actualiza un parámetro
     * @param parametro Parámetro a guardar
     * @return Parámetro guardado
     */
    ParametroSistema guardarParametro(ParametroSistema parametro);
    
    /**
     * Crea un nuevo parámetro
     * @param clave Clave única del parámetro
     * @param valor Valor del parámetro
     * @param tipoDato Tipo de dato
     * @param categoria Categoría
     * @param descripcion Descripción del parámetro
     * @param editable Si es editable por el usuario
     * @return Parámetro creado
     */
    ParametroSistema crearParametro(String clave, String valor, TipoDatoParametro tipoDato, 
                                    CategoriaParametro categoria, String descripcion, Boolean editable);
    
    /**
     * Actualiza el valor de un parámetro por su clave
     * @param clave Clave del parámetro
     * @param nuevoValor Nuevo valor
     * @return Parámetro actualizado
     */
    ParametroSistema actualizarValor(String clave, String nuevoValor);
    
    /**
     * Elimina un parámetro por su clave
     * @param clave Clave del parámetro
     */
    void eliminarParametro(String clave);
    
    /**
     * Obtiene el valor de un parámetro como String
     * @param clave Clave del parámetro
     * @param valorPorDefecto Valor por defecto si no existe
     * @return Valor del parámetro o valor por defecto
     */
    String obtenerValorString(String clave, String valorPorDefecto);
    
    /**
     * Obtiene el valor de un parámetro como Integer
     * @param clave Clave del parámetro
     * @param valorPorDefecto Valor por defecto si no existe
     * @return Valor del parámetro o valor por defecto
     */
    Integer obtenerValorInteger(String clave, Integer valorPorDefecto);
    
    /**
     * Obtiene el valor de un parámetro como Boolean
     * @param clave Clave del parámetro
     * @param valorPorDefecto Valor por defecto si no existe
     * @return Valor del parámetro o valor por defecto
     */
    Boolean obtenerValorBoolean(String clave, Boolean valorPorDefecto);
    
    /**
     * Obtiene el valor de un parámetro como BigDecimal
     * @param clave Clave del parámetro
     * @param valorPorDefecto Valor por defecto si no existe
     * @return Valor del parámetro o valor por defecto
     */
    BigDecimal obtenerValorDecimal(String clave, BigDecimal valorPorDefecto);
    
    /**
     * Verifica si existe un parámetro con la clave dada
     * @param clave Clave del parámetro
     * @return true si existe
     */
    boolean existeParametro(String clave);
    
    /**
     * Inicializa los parámetros por defecto del sistema
     */
    void inicializarParametrosPorDefecto();
}
