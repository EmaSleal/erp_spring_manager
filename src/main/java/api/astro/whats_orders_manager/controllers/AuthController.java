package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;
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
        return "auth/login"; // Redirige a la vista login.html en templates/auth/
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        RedirectAttributes redirectAttributes) {

        try {
            //print
            System.out.println("username: " + username);
            System.out.println("password: " + password);
            // Autenticamos manualmente al usuario con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            return "redirect:/"; // Redirigir a la página principal después del login

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "nombre o contraseña incorrectos");
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "auth/register"; // Redirige a la vista register.html en templates/auth/
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute Usuario usuario,
                               RedirectAttributes redirectAttributes,
                               @RequestParam String confirmPassword) {

        // Verificar si las contraseñas coinciden
        if (!usuario.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/auth/register";
        }

        // Verificar si el usuario ya existe
        if (usuarioRepository.findByNombre(usuario.getNombre()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "El número de teléfono ya está registrado");
            return "redirect:/auth/register";
        }

        // Encriptar contraseña y guardar el usuario
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol("USER"); // Rol por defecto
        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("success", "Registro exitoso, ahora puedes iniciar sesión.");
        return "redirect:/auth/login";
    }
}
