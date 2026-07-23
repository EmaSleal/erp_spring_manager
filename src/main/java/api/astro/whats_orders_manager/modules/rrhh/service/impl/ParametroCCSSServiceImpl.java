package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.model.ParametroCCSS;
import api.astro.whats_orders_manager.modules.rrhh.repository.ParametroCCSSRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.ParametroCCSSService;
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
public class ParametroCCSSServiceImpl implements ParametroCCSSService {

    private final ParametroCCSSRepository parametroCCSSRepository;

    @Override
    public ParametroCCSS findVigenteByFecha(LocalDate fecha) {
        return parametroCCSSRepository.findVigenteByFecha(fecha)
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró un registro de ParametroCCSS vigente para la fecha: " + fecha));
    }

    @Override
    public List<ParametroCCSS> findAll() {
        return parametroCCSSRepository.findAll();
    }
}
