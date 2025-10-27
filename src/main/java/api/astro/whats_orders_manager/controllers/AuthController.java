package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ============================================================================
 * AUTH CONTROLLER
 * WhatsApp Orders Manager
 * ============================================================================
 * Controlador de autenticación (Login y Registro)
 * 
 * @version 1.1 - Mejorado logging profesional
 * @since 26/10/2025
 * ============================================================================
 */
@Slf4j
@Controller
@RequestMapping("/auth")
public class AuthController {

    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        log.debug("Acceso a página de login");
        return "auth/login"; // Redirige a la vista login.html en templates/auth/
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        RedirectAttributes redirectAttributes) {

        log.info("Intento de login para usuario: {}", username);
        
        try {
            // Autenticamos manualmente al usuario con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("✅ Login exitoso para usuario: {}", username);
            return "redirect:/"; // Redirigir a la página principal después del login

        } catch (Exception e) {
            log.warn("❌ Login fallido para usuario: {} - Razón: {}", username, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "nombre o contraseña incorrectos");
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        log.debug("Acceso a formulario de registro");
        model.addAttribute("usuario", new Usuario());
        return "auth/register"; // Redirige a la vista register.html en templates/auth/
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute Usuario usuario,
                               RedirectAttributes redirectAttributes,
                               @RequestParam String confirmPassword) {

        log.info("Intento de registro para usuario: {}", usuario.getNombre());
        
        // Verificar si las contraseñas coinciden
        if (!usuario.getPassword().equals(confirmPassword)) {
            log.warn("Registro fallido - Las contraseñas no coinciden para usuario: {}", usuario.getNombre());
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/auth/register";
        }

        // Verificar si el usuario ya existe
        if (usuarioRepository.findByNombre(usuario.getNombre()).isPresent()) {
            log.warn("Registro fallido - Usuario ya existe: {}", usuario.getNombre());
            redirectAttributes.addFlashAttribute("error", "El número de teléfono ya está registrado");
            return "redirect:/auth/register";
        }

        // Encriptar contraseña y guardar el usuario
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol("USER"); // Rol por defecto
        usuarioRepository.save(usuario);

        log.info("✅ Usuario registrado exitosamente: {} con rol USER", usuario.getNombre());
        redirectAttributes.addFlashAttribute("success", "Registro exitoso, ahora puedes iniciar sesión.");
        return "redirect:/auth/login";
    }
}
