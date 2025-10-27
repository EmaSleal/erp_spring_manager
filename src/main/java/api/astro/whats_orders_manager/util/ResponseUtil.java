package api.astro.whats_orders_manager.util;

import api.astro.whats_orders_manager.dto.ResponseDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Utilidad para crear respuestas HTTP estandarizadas
 * Proporciona métodos estáticos para construir ResponseEntity de forma consistente
 * 
 * @version 1.1 - Agregados métodos para archivos (PDF, Excel, CSV)
 * @since 26/10/2025
 */
public class ResponseUtil {
    
    private ResponseUtil() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Crea una respuesta de error con status 400 Bad Request
     * 
     * @param mensaje Mensaje descriptivo del error
     * @return ResponseEntity con el error
     */
    public static ResponseEntity<Map<String, Object>> error(String mensaje) {
        return ResponseEntity
                .badRequest()
                .body(ResponseDTO.error(mensaje).toMap());
    }
    
    /**
     * Crea una respuesta de error con datos adicionales
     * 
     * @param mensaje Mensaje descriptivo del error
     * @param data Datos adicionales del error
     * @return ResponseEntity con el error
     */
    public static ResponseEntity<Map<String, Object>> error(String mensaje, Object data) {
        return ResponseEntity
                .badRequest()
                .body(ResponseDTO.error(mensaje, data).toMap());
    }
    
    /**
     * Crea una respuesta de éxito con status 200 OK
     * 
     * @param mensaje Mensaje descriptivo del éxito
     * @return ResponseEntity con el éxito
     */
    public static ResponseEntity<Map<String, Object>> success(String mensaje) {
        return ResponseEntity
                .ok()
                .body(ResponseDTO.success(mensaje).toMap());
    }
    
    /**
     * Crea una respuesta de éxito con datos adicionales
     * 
     * @param mensaje Mensaje descriptivo del éxito
     * @param data Datos adicionales
     * @return ResponseEntity con el éxito
     */
    public static ResponseEntity<Map<String, Object>> success(String mensaje, Object data) {
        return ResponseEntity
                .ok()
                .body(ResponseDTO.success(mensaje, data).toMap());
    }
    
    /**
     * Crea una respuesta de éxito solo con datos (sin mensaje)
     * 
     * @param data Datos a devolver
     * @return ResponseEntity con los datos
     */
    public static ResponseEntity<Map<String, Object>> successData(Object data) {
        return ResponseEntity
                .ok()
                .body(ResponseDTO.success("Operación exitosa", data).toMap());
    }
    
    // ========================================================================
    // MÉTODOS PARA EXPORTACIÓN DE ARCHIVOS
    // ========================================================================
    
    /**
     * Crea una respuesta para descargar archivo PDF
     * 
     * @param contenido Bytes del archivo PDF
     * @param nombreArchivo Nombre del archivo a descargar
     * @return ResponseEntity con el PDF y headers apropiados
     */
    public static ResponseEntity<byte[]> pdf(byte[] contenido, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", nombreArchivo);
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(contenido);
    }
    
    /**
     * Crea una respuesta para descargar archivo Excel (.xlsx)
     * 
     * @param contenido Bytes del archivo Excel
     * @param nombreArchivo Nombre del archivo a descargar
     * @return ResponseEntity con el Excel y headers apropiados
     */
    public static ResponseEntity<byte[]> excel(byte[] contenido, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", nombreArchivo);
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(contenido);
    }
    
    /**
     * Crea una respuesta para descargar archivo CSV
     * 
     * @param contenido Bytes del archivo CSV
     * @param nombreArchivo Nombre del archivo a descargar
     * @return ResponseEntity con el CSV y headers apropiados
     */
    public static ResponseEntity<byte[]> csv(byte[] contenido, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", nombreArchivo);
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(contenido);
    }
    
    /**
     * Crea una respuesta para descargar un archivo genérico
     * 
     * @param contenido Bytes del archivo
     * @param nombreArchivo Nombre del archivo a descargar
     * @param mediaType Tipo de contenido (MIME type)
     * @return ResponseEntity con el archivo y headers apropiados
     */
    public static ResponseEntity<byte[]> file(byte[] contenido, String nombreArchivo, MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", nombreArchivo);
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(contenido);
    }
}
