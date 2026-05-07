package api.astro.whats_orders_manager.modules.configuracion.converter;

import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Convertidor de String (código de provincia) a ProvinciaCostaRica
 * Permite que Spring haga binding automático en formularios
 * 
 * @author Sistema
 * @since Sprint 5 - Fase 9
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StringToProvinciaCostaRicaConverter implements Converter<String, ProvinciaCostaRica> {

    private final ProvinciaCostaRicaRepository provinciaRepository;

    @Override
    public ProvinciaCostaRica convert(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        
        log.debug("Convertiendo código de provincia: {}", codigo);
        return provinciaRepository.findById(codigo)
                .orElseGet(() -> {
                    log.warn("Provincia no encontrada con código: {}", codigo);
                    return null;
                });
    }
}
