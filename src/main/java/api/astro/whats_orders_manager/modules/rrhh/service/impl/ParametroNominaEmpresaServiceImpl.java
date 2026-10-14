package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroNominaEmpresa;
import api.astro.whats_orders_manager.modules.rrhh.repository.ParametroNominaEmpresaRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.ParametroNominaEmpresaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParametroNominaEmpresaServiceImpl implements ParametroNominaEmpresaService {

    private final ParametroNominaEmpresaRepository parametroNominaEmpresaRepository;

    @Override
    public ParametroNominaEmpresa findVigenteByFecha(LocalDate fecha) {
        return parametroNominaEmpresaRepository.findVigenteByFecha(fecha)
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró un registro de ParametroNominaEmpresa vigente para la fecha: " + fecha));
    }

    @Override
    public List<ParametroNominaEmpresa> findAll() {
        return parametroNominaEmpresaRepository.findAll();
    }
}
