package api.astro.whats_orders_manager.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador para manejar páginas de error personalizadas
 */
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Maneja todos los errores y redirige a páginas personalizadas
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            // Error 403 - Acceso Denegado
            if (statusCode == 403) {
                return "error/403";
            }
            
            // Error 404 - No Encontrado
            if (statusCode == 404) {
                return "error/404";
            }
            
            // Error 500 - Error Interno del Servidor
            if (statusCode == 500) {
                return "error/500";
            }
        }
        
        // Error genérico
        return "error/error";
    }

    /**
     * Página específica para error 403
     */
    @RequestMapping("/error/403")
    public String error403() {
        return "error/403";
    }

    /**
     * Página específica para error 404
     */
    @RequestMapping("/error/404")
    public String error404() {
        return "error/404";
    }

    /**
     * Página específica para error 500
     */
    @RequestMapping("/error/500")
    public String error500() {
        return "error/500";
    }
}
