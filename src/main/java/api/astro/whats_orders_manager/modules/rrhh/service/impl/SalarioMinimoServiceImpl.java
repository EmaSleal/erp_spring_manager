package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.model.SalarioMinimo;
import api.astro.whats_orders_manager.modules.rrhh.repository.SalarioMinimoRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.SalarioMinimoService;
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
public class SalarioMinimoServiceImpl implements SalarioMinimoService {

    private final SalarioMinimoRepository salarioMinimoRepository;

    @Override
    public SalarioMinimo findVigenteByCategoria(String categoria, LocalDate fecha) {
        return salarioMinimoRepository.findVigenteByCategoria(categoria, fecha)
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró salario mínimo vigente para la categoría '" +
                        categoria + "' en la fecha " + fecha));
    }

    @Override
    public List<SalarioMinimo> findAll() {
        return salarioMinimoRepository.findAll();
    }
}
