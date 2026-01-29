package api.astro.whats_orders_manager.shared.service;

import api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio para manejo de símbolos de moneda.
 * Centraliza la lógica de obtención del símbolo de moneda
 * desde la configuración de facturación.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonedaService {
    
    private final ConfiguracionFacturacionService configuracionFacturacionService;
    
    /**
     * Obtiene el símbolo de moneda desde la configuración activa.
     * 
     * @return Símbolo de moneda (₡, $, €, etc.)
     */
    public String obtenerSimboloMoneda() {
        try {
            var config = configuracionFacturacionService.getConfiguracionActiva();
            if (config.isPresent()) {
                String moneda = config.get().getMoneda();
                if (moneda != null) {
                    return obtenerSimboloPorCodigo(moneda);
                }
                // Si tiene símbolo personalizado, usarlo
                String simboloPersonalizado = config.get().getSimboloMoneda();
                if (simboloPersonalizado != null && !simboloPersonalizado.trim().isEmpty()) {
                    return simboloPersonalizado;
                }
            }
        } catch (Exception e) {
            log.warn("Error al obtener símbolo de moneda, usando predeterminado: {}", e.getMessage());
        }
        return "₡"; // Colones por defecto
    }
    
    /**
     * Mapea el código de moneda ISO a su símbolo correspondiente.
     * 
     * @param codigoMoneda Código ISO de moneda (CRC, USD, EUR, etc.)
     * @return Símbolo correspondiente a la moneda
     */
    public String obtenerSimboloPorCodigo(String codigoMoneda) {
        if (codigoMoneda == null || codigoMoneda.trim().isEmpty()) {
            return "₡";
        }
        
        return switch (codigoMoneda.toUpperCase()) {
            case "CRC" -> "₡";      // Colón costarricense
            case "USD" -> "$";       // Dólar estadounidense
            case "MXN" -> "$";       // Peso mexicano
            case "EUR" -> "€";       // Euro
            case "GBP" -> "£";       // Libra esterlina
            case "JPY" -> "¥";       // Yen japonés
            case "CNY" -> "¥";       // Yuan chino
            case "PEN" -> "S/";      // Sol peruano
            case "ARS" -> "$";       // Peso argentino
            case "CLP" -> "$";       // Peso chileno
            case "COP" -> "$";       // Peso colombiano
            case "BRL" -> "R$";      // Real brasileño
            default -> codigoMoneda + " ";  // Código + espacio como fallback
        };
    }
    
    /**
     * Obtiene el código de moneda desde la configuración activa.
     * 
     * @return Código ISO de moneda (CRC, USD, etc.)
     */
    public String obtenerCodigoMoneda() {
        try {
            var config = configuracionFacturacionService.getConfiguracionActiva();
            if (config.isPresent() && config.get().getMoneda() != null) {
                return config.get().getMoneda();
            }
        } catch (Exception e) {
            log.warn("Error al obtener código de moneda: {}", e.getMessage());
        }
        return "CRC"; // CRC por defecto
    }
    
    /**
     * Formatea un valor numérico con el símbolo de moneda.
     * Ejemplo: formatearConMoneda(1250.50) → "$1,250.50"
     * 
     * @param valor Valor a formatear
     * @return Cadena formateada con símbolo de moneda
     */
    public String formatearConMoneda(Number valor) {
        String simbolo = obtenerSimboloMoneda();
        return String.format("%s%,.2f", simbolo, valor.doubleValue());
    }
}
