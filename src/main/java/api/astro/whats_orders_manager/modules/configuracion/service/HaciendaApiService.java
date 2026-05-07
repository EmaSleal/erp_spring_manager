package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.dto.CabysBusquedaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;

/**
 * Servicio para consultar la API pública de Hacienda Costa Rica.
 * Permite validar identificaciones, obtener datos tributarios y buscar códigos CABYS.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 4
 */
public interface HaciendaApiService {
    
    /**
     * Consulta los datos de un contribuyente en la API de Hacienda.
     * Endpoint: https://api.hacienda.go.cr/fe/ae?identificacion={numero}
     * 
     * @param numeroIdentificacion Número de identificación sin guiones ni espacios
     * @return DTO con los datos del contribuyente o error
     */
    HaciendaConsultaDTO consultarContribuyente(String numeroIdentificacion);
    
    /**
     * Valida si un número de identificación existe en Hacienda.
     * 
     * @param numeroIdentificacion Número de identificación
     * @return true si existe y está inscrito, false en caso contrario
     */
    boolean validarIdentificacion(String numeroIdentificacion);
    
    /**
     * Busca códigos CABYS por descripción o palabra clave.
     * Endpoint: https://api.hacienda.go.cr/fe/cabys?q={busqueda}&top={cantidad}
     * 
     * @param busqueda Término de búsqueda (ej: "jugo de tomate")
     * @param top Cantidad máxima de resultados (por defecto 10)
     * @return DTO con lista de códigos CABYS encontrados
     */
    CabysBusquedaDTO buscarCabys(String busqueda, Integer top);
}

