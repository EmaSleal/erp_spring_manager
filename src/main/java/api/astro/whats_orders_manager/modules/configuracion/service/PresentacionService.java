package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface PresentacionService {
    List<Presentacion> findAll();
    Optional<Presentacion> findById(Integer id);
    Presentacion save(Presentacion presentacion);
    void deleteById(Integer id);

}