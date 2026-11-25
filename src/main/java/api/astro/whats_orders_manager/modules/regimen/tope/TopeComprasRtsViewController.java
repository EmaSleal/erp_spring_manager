package api.astro.whats_orders_manager.modules.regimen.tope;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Clock;
import java.time.LocalDate;

/** Pantalla de parámetros por año del tope de compras del RTS, dentro de Configuración (T-056, ADR-072). */
@Controller
@RequestMapping("/configuracion/tope-compras-rts")
@RequiredArgsConstructor
public class TopeComprasRtsViewController {

    private final Clock clock;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'REGIMEN_TOPE_COMPRAS_GESTIONAR')")
    public String topeComprasRts(Model model, Authentication authentication) {
        model.addAttribute("title", "Tope de compras del RTS");
        model.addAttribute("currentPage", "config");
        model.addAttribute("anio", LocalDate.now(clock).getYear());
        return "modules/configuracion/tope-compras-rts";
    }
}
