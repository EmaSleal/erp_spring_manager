package api.astro.whats_orders_manager.modules.rrhh.service;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroNominaEmpresa;

import java.time.LocalDate;
import java.util.List;

public interface ParametroNominaEmpresaService {

    /**
     * Devuelve el registro de ParametroNominaEmpresa vigente para la fecha dada.
     * Lanza NoSuchElementException si ningún registro cubre la fecha.
     */
    ParametroNominaEmpresa findVigenteByFecha(LocalDate fecha);

    List<ParametroNominaEmpresa> findAll();
}
