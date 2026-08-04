package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParametroContableService {

    private final ParametroContableRepository parametroRepo;
    private final CuentaContableRepository cuentaRepo;

    @Transactional(readOnly = true)
    public List<ParametroContable> listar() {
        return parametroRepo.findAllWithCuentas();
    }

    @Transactional
    public ParametroContable crear(String clave, String descripcion, Long cuentaContableId) {
        if (parametroRepo.findByClave(clave).isPresent()) {
            throw new IllegalArgumentException("Ya existe un parámetro con la clave: " + clave);
        }
        CuentaContable cuenta = cuentaRepo.findById(cuentaContableId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta contable no encontrada: " + cuentaContableId));

        ParametroContable parametro = ParametroContable.builder()
                .clave(clave.trim().toUpperCase())
                .descripcion(descripcion)
                .cuentaContable(cuenta)
                .build();

        ParametroContable saved = parametroRepo.save(parametro);
        log.info("Created ParametroContable id={} clave={}", saved.getId(), saved.getClave());
        return saved;
    }

    @Transactional
    public ParametroContable actualizar(Long id, String clave, String descripcion, Long cuentaContableId) {
        ParametroContable parametro = parametroRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parámetro contable no encontrado: " + id));

        String claveNormalizada = clave.trim().toUpperCase();
        parametroRepo.findByClave(claveNormalizada).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe un parámetro con la clave: " + claveNormalizada);
            }
        });

        CuentaContable cuenta = cuentaRepo.findById(cuentaContableId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta contable no encontrada: " + cuentaContableId));

        parametro.setClave(claveNormalizada);
        parametro.setDescripcion(descripcion);
        parametro.setCuentaContable(cuenta);

        ParametroContable saved = parametroRepo.save(parametro);
        log.info("Updated ParametroContable id={} clave={}", saved.getId(), saved.getClave());
        return saved;
    }

    @Transactional
    public void eliminar(Long id) {
        if (!parametroRepo.existsById(id)) {
            throw new IllegalArgumentException("Parámetro contable no encontrado: " + id);
        }
        parametroRepo.deleteById(id);
        log.info("Deleted ParametroContable id={}", id);
    }
}
