package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroCCSS;

import java.time.LocalDate;
import java.util.List;

public interface ParametroCCSSService {

    /**
     * Returns the ParametroCCSS record vigente for the given date.
     * Throws NoSuchElementException if no record covers the date.
     */
    ParametroCCSS findVigenteByFecha(LocalDate fecha);

    List<ParametroCCSS> findAll();
}
