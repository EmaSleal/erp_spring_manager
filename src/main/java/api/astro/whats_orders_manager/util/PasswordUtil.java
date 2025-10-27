package api.astro.whats_orders_manager.util;

import java.security.SecureRandom;

/**
 * Utilidad para operaciones relacionadas con contraseñas
 * Proporciona métodos para generación y validación de contraseñas
 * 
 * @version 1.0
 * @since 26/10/2025
 */
public class PasswordUtil {
    
    private static final String CARACTERES_PASSWORD = 
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    
    private static final int LONGITUD_DEFAULT = 10;
    private static final int LONGITUD_MINIMA = 6;
    
    private PasswordUtil() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Genera una contraseña aleatoria segura con longitud por defecto (10 caracteres)
     * 
     * @return Contraseña aleatoria
     */
    public static String generarPasswordAleatoria() {
        return generarPasswordAleatoria(LONGITUD_DEFAULT);
    }
    
    /**
     * Genera una contraseña aleatoria segura con longitud específica
     * 
     * @param longitud Longitud de la contraseña (mínimo 6)
     * @return Contraseña aleatoria
     * @throws IllegalArgumentException si la longitud es menor a 6
     */
    public static String generarPasswordAleatoria(int longitud) {
        if (longitud < LONGITUD_MINIMA) {
            throw new IllegalArgumentException(
                    "La longitud mínima de la contraseña es " + LONGITUD_MINIMA);
        }
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(longitud);
        
        for (int i = 0; i < longitud; i++) {
            int index = random.nextInt(CARACTERES_PASSWORD.length());
            password.append(CARACTERES_PASSWORD.charAt(index));
        }
        
        return password.toString();
    }
    
    /**
     * Valida que una contraseña cumpla con los requisitos mínimos
     * 
     * @param password Contraseña a validar
     * @return true si cumple los requisitos, false en caso contrario
     */
    public static boolean esPasswordValida(String password) {
        return password != null 
                && password.length() >= LONGITUD_MINIMA 
                && !password.isBlank();
    }
    
    /**
     * Obtiene la longitud mínima requerida para contraseñas
     * 
     * @return Longitud mínima
     */
    public static int getLongitudMinima() {
        return LONGITUD_MINIMA;
    }
}
