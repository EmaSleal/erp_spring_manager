package api.astro.whats_orders_manager.modules.regimen.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Clock;
import java.time.LocalDate;

/** Pantalla de régimen tributario con historial de tramos, dentro de Configuración (T-056, ADR-072). */
@Controller
@RequestMapping("/configuracion/regimen-tributario")
@RequiredArgsConstructor
public class RegimenTributarioViewController {

    private final Clock clock;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'REGIMEN_TRIBUTARIO_GESTIONAR') "
        + "or @permisoService.tienePermisoPorCodigo(#authentication.name, 'CONFIG_VER')")
    public String regimenTributario(Model model, Authentication authentication) {
        model.addAttribute("title", "Régimen tributario");
        model.addAttribute("currentPage", "config");
        model.addAttribute("hoy", LocalDate.now(clock).toString());
        return "modules/configuracion/regimen-tributario";
    }
}
