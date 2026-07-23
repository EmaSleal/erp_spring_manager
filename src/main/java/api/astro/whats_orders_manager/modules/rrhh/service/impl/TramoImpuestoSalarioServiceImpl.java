package api.astro.whats_orders_manager.modules.rrhh.service.impl;

import api.astro.whats_orders_manager.modules.rrhh.model.TramoImpuestoSalario;
import api.astro.whats_orders_manager.modules.rrhh.repository.TramoImpuestoSalarioRepository;
import api.astro.whats_orders_manager.modules.rrhh.service.TramoImpuestoSalarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TramoImpuestoSalarioServiceImpl implements TramoImpuestoSalarioService {

    private final TramoImpuestoSalarioRepository tramoRepository;

    @Override
    public List<TramoImpuestoSalario> findByAnioVigencia(int anioVigencia) {
        List<TramoImpuestoSalario> result =
                tramoRepository.findByAnioVigenciaOrderByLimiteInferiorAsc(anioVigencia);
        log.debug("TramoImpuestoSalario for year {}: {} brackets found", anioVigencia, result.size());
        return result;
    }

    @Override
    public List<TramoImpuestoSalario> findAll() {
        return tramoRepository.findAll();
    }
}
