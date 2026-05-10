package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generador de claves numéricas para comprobantes electrónicos.
 * Formato según especificación de Hacienda de Costa Rica.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Component
@Slf4j
public class ClaveNumericaGenerator {
    
    private static final String PAIS_COSTA_RICA = "506";
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Genera clave numérica de 50 dígitos.
     * 
     * Formato: [País(3)][Día(2)][Mes(2)][Año(2)][Cédula(12)][Consecutivo(20)][Situación(1)][Código(8)]
     * 
     * @param cedula Cédula jurídica del emisor (sin guiones)
     * @param consecutivo Consecutivo del comprobante
     * @param tipo Tipo de comprobante
     * @param fechaEmision Fecha de emisión
     * @return Clave numérica de 50 dígitos
     */
    public String generar(String cedula, Long consecutivo, TipoComprobanteElectronico tipo, LocalDateTime fechaEmision) {
        StringBuilder clave = new StringBuilder();
        
        // 1. País (3 dígitos) - Costa Rica = 506
        clave.append(PAIS_COSTA_RICA);
        
        // 2. Día (2 dígitos)
        clave.append(String.format("%02d", fechaEmision.getDayOfMonth()));
        
        // 3. Mes (2 dígitos)
        clave.append(String.format("%02d", fechaEmision.getMonthValue()));
        
        // 4. Año (2 dígitos) - Últimos 2 dígitos
        clave.append(String.format("%02d", fechaEmision.getYear() % 100));
        
        // 5. Cédula del emisor (12 dígitos) - rellenar con ceros a la izquierda
        String cedulaFormateada = cedula.replaceAll("[^0-9]", ""); // Eliminar guiones
        clave.append(String.format("%012d", Long.parseLong(cedulaFormateada)));
        
        // 6. Consecutivo (20 dígitos)
        // Formato: [Código sucursal(3)][Terminal(5)][Tipo(2)][Número(10)]
        String consecutivoStr = generarConsecutivo(consecutivo, tipo);
        clave.append(consecutivoStr);
        
        // 7. Situación del comprobante (1 dígito)
        // 1=Normal, 2=Contingencia, 3=Sin Internet
        clave.append("1");
        
        // 8. Código de seguridad (8 dígitos aleatorios)
        clave.append(generarCodigoSeguridad());
        
        String claveGenerada = clave.toString();
        
        if (claveGenerada.length() != 50) {
            log.error("Clave numérica generada con longitud incorrecta: {} caracteres", claveGenerada.length());
            throw new IllegalStateException("Error generando clave numérica: longitud incorrecta");
        }
        
        log.debug("Clave numérica generada: {}", claveGenerada);
        return claveGenerada;
    }
    
    /**
     * Genera el consecutivo de 20 dígitos.
     */
    private String generarConsecutivo(Long numero, TipoComprobanteElectronico tipo) {
        // Sucursal: 001 (por defecto)
        String sucursal = "001";
        
        // Terminal: 00001 (por defecto)
        String terminal = "00001";
        
        // Tipo de comprobante: 01-09
        String tipoStr = tipo.getCodigo();
        
        // Número consecutivo: 10 dígitos
        String numeroStr = String.format("%010d", numero);
        
        return sucursal + terminal + tipoStr + numeroStr;
    }
    
    /**
     * Genera código de seguridad aleatorio de 8 dígitos.
     */
    private String generarCodigoSeguridad() {
        int codigo = 10000000 + random.nextInt(90000000);
        return String.valueOf(codigo);
    }
    
    /**
     * Valida formato de clave numérica.
     */
    public boolean validar(String claveNumerica) {
        if (claveNumerica == null || claveNumerica.length() != 50) {
            return false;
        }
        
        // Verificar que todos sean dígitos
        if (!claveNumerica.matches("\\d{50}")) {
            return false;
        }
        
        // Verificar país
        String pais = claveNumerica.substring(0, 3);
        if (!pais.equals(PAIS_COSTA_RICA)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Extrae información de una clave numérica.
     */
    public ClaveNumericaInfo extraerInfo(String claveNumerica) {
        if (!validar(claveNumerica)) {
            throw new IllegalArgumentException("Clave numérica inválida");
        }
        
        return ClaveNumericaInfo.builder()
            .pais(claveNumerica.substring(0, 3))
            .dia(Integer.parseInt(claveNumerica.substring(3, 5)))
            .mes(Integer.parseInt(claveNumerica.substring(5, 7)))
            .anio(Integer.parseInt(claveNumerica.substring(7, 9)))
            .cedula(claveNumerica.substring(9, 21))
            .consecutivo(claveNumerica.substring(21, 41))
            .situacion(claveNumerica.substring(41, 42))
            .codigoSeguridad(claveNumerica.substring(42, 50))
            .build();
    }
    
    /**
     * Información extraída de una clave numérica.
     */
    @lombok.Data
    @lombok.Builder
    public static class ClaveNumericaInfo {
        private String pais;
        private Integer dia;
        private Integer mes;
        private Integer anio;
        private String cedula;
        private String consecutivo;
        private String situacion;
        private String codigoSeguridad;
    }
}
