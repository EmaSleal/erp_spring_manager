package api.astro.whats_orders_manager.modules.configuracion.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.configuracion.repository.PresentacionRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.PresentacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PresentacionServiceImpl implements PresentacionService {
    @Autowired
    private PresentacionRepository presentacionRepository;

    @Override
    public List<Presentacion> findAll() { return presentacionRepository.findAll(); }

    @Override
    public Optional<Presentacion> findById(Integer id) { return presentacionRepository.findById(id); }

    @Override
    public Presentacion save(Presentacion presentacion) { return presentacionRepository.save(presentacion); }

    @Override
    public void deleteById(Integer id) { presentacionRepository.deleteById(id); }
}