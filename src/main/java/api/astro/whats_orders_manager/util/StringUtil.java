package api.astro.whats_orders_manager.util;

import lombok.extern.slf4j.Slf4j;

/**
 * ============================================================================
 * STRING UTIL
 * WhatsApp Orders Manager
 * ============================================================================
 * Utilidad para operaciones comunes con cadenas de texto.
 * 
 * Funcionalidades:
 * - Generación de iniciales para avatares
 * - Normalización de nombres
 * - Validación de cadenas
 * - Formateo de texto
 * 
 * @version 1.0
 * @since 26/10/2025
 * ============================================================================
 */
@Slf4j
public final class StringUtil {

    private StringUtil() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no puede ser instanciada");
    }

    /**
     * Genera iniciales a partir de un nombre completo para usar en avatares.
     * 
     * Reglas:
     * - Si el nombre tiene 2+ palabras: Primera letra de las primeras 2 palabras
     * - Si el nombre tiene 1 palabra con 2+ caracteres: Primeras 2 letras
     * - Si el nombre tiene 1 palabra con 1 carácter: Esa letra + "U"
     * - Si el nombre es null/vacío: "??"
     * 
     * Ejemplos:
     * - "Juan Pérez" → "JP"
     * - "María" → "MA"
     * - "J" → "JU"
     * - "" → "??"
     * 
     * @param nombre Nombre completo del usuario
     * @return Iniciales en mayúsculas (siempre 2 caracteres)
     */
    public static String generarIniciales(String nombre) {
        // Validar entrada
        if (nombre == null || nombre.trim().isEmpty()) {
            log.debug("Nombre vacío o null, retornando iniciales por defecto");
            return "??";
        }

        // Limpiar y separar palabras
        String[] partes = nombre.trim().split("\\s+");
        
        // Si tiene dos o más palabras
        if (partes.length >= 2) {
            String iniciales = String.valueOf(partes[0].charAt(0)) + partes[1].charAt(0);
            String resultado = iniciales.toUpperCase();
            log.debug("Iniciales generadas para '{}': {}", nombre, resultado);
            return resultado;
        }
        
        // Si tiene una sola palabra
        String palabra = partes[0];
        if (palabra.length() >= 2) {
            String resultado = palabra.substring(0, 2).toUpperCase();
            log.debug("Iniciales generadas para '{}': {}", nombre, resultado);
            return resultado;
        } else {
            String resultado = (palabra + "U").toUpperCase();
            log.debug("Iniciales generadas para '{}': {} (complementado)", nombre, resultado);
            return resultado;
        }
    }

    /**
     * Normaliza un nombre eliminando espacios extra y poniendo en formato adecuado.
     * 
     * Ejemplo:
     * - "  juan   perez  " → "Juan Perez"
     * 
     * @param nombre Nombre a normalizar
     * @return Nombre normalizado con capitalización
     */
    public static String normalizarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "";
        }

        String[] palabras = nombre.trim().toLowerCase().split("\\s+");
        StringBuilder resultado = new StringBuilder();
        
        for (int i = 0; i < palabras.length; i++) {
            if (i > 0) {
                resultado.append(" ");
            }
            
            String palabra = palabras[i];
            if (!palabra.isEmpty()) {
                resultado.append(Character.toUpperCase(palabra.charAt(0)));
                if (palabra.length() > 1) {
                    resultado.append(palabra.substring(1));
                }
            }
        }
        
        return resultado.toString();
    }

    /**
     * Verifica si una cadena está vacía o es null.
     * 
     * @param str Cadena a verificar
     * @return true si es null, vacía o solo espacios
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Verifica si una cadena NO está vacía.
     * 
     * @param str Cadena a verificar
     * @return true si tiene contenido
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Trunca una cadena al largo especificado agregando "..." si es necesario.
     * 
     * @param str Cadena a truncar
     * @param maxLength Longitud máxima
     * @return Cadena truncada
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        
        if (str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Capitaliza la primera letra de una cadena.
     * 
     * @param str Cadena a capitalizar
     * @return Cadena con primera letra mayúscula
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return "";
        }
        
        if (str.length() == 1) {
            return str.toUpperCase();
        }
        
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

    /**
     * Limpia caracteres especiales de una cadena dejando solo letras, números y espacios.
     * 
     * @param str Cadena a limpiar
     * @return Cadena limpia
     */
    public static String limpiarCaracteresEspeciales(String str) {
        if (isEmpty(str)) {
            return "";
        }
        
        return str.replaceAll("[^a-zA-Z0-9\\s]", "");
    }
}
