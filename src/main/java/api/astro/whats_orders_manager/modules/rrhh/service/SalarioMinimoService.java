package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.SalarioMinimo;

import java.time.LocalDate;
import java.util.List;

public interface SalarioMinimoService {

    /**
     * Returns the vigente SalarioMinimo for the given categoria and date.
     * Throws NoSuchElementException if no vigente record exists for the categoria.
     */
    SalarioMinimo findVigenteByCategoria(String categoria, LocalDate fecha);

    List<SalarioMinimo> findAll();
}
