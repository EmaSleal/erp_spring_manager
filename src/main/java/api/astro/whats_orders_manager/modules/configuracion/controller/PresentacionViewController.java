package api.astro.whats_orders_manager.modules.configuracion.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Vista de mantenimiento de presentaciones (nombre, empacado, unidad FE). Los datos se cargan por
 * JS desde /api/configuracion/presentaciones. Solo ADMIN, igual que el resto de /configuracion/**
 * (SecurityConfig) y que /api/configuracion/**.
 */
@Slf4j
@Controller
@RequestMapping("/configuracion/presentaciones")
public class PresentacionViewController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listar() {
        log.info("GET /configuracion/presentaciones");
        return "modules/configuracion/presentaciones";
    }
}
