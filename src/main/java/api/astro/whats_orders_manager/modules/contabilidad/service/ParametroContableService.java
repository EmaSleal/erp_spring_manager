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
        String claveNormalizada = normalizarClave(clave);
        if (parametroRepo.findByClave(claveNormalizada).isPresent()) {
            throw new IllegalArgumentException("Ya existe un parámetro con la clave: " + claveNormalizada);
        }
        CuentaContable cuenta = cuentaHoja(claveNormalizada, cuentaContableId);

        ParametroContable parametro = ParametroContable.builder()
                .clave(claveNormalizada)
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

        String claveNormalizada = normalizarClave(clave);
        parametroRepo.findByClave(claveNormalizada).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe un parámetro con la clave: " + claveNormalizada);
            }
        });

        CuentaContable cuenta = cuentaHoja(claveNormalizada, cuentaContableId);

        parametro.setClave(claveNormalizada);
        parametro.setDescripcion(descripcion);
        parametro.setCuentaContable(cuenta);

        ParametroContable saved = parametroRepo.save(parametro);
        log.info("Updated ParametroContable id={} clave={}", saved.getId(), saved.getClave());
        return saved;
    }

    /** Clave en mayúsculas y sin espacios en los extremos; se usa tanto para buscar duplicados como para guardar. */
    private static String normalizarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave del parámetro contable es obligatoria");
        }
        return clave.trim().toUpperCase();
    }

    /**
     * Cuenta destino de un parámetro contable: debe existir y ser una hoja activa que acepta
     * movimientos y no tiene subcuentas (ADR-017 R1).
     *
     * @throws IllegalArgumentException si no existe o no es una hoja elegible
     */
    private CuentaContable cuentaHoja(String clave, Long cuentaContableId) {
        if (cuentaContableId == null) {
            throw new IllegalArgumentException("La cuenta contable es obligatoria");
        }
        CuentaContable cuenta = cuentaRepo.findById(cuentaContableId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta contable no encontrada: " + cuentaContableId));
        String motivo = null;
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            motivo = "está inactiva";
        } else if (!Boolean.TRUE.equals(cuenta.getAceptaMovimientos())) {
            motivo = "no acepta movimientos";
        } else {
            Long subcuentas = cuentaRepo.countSubcuentas(cuenta.getIdCuenta());
            if (subcuentas != null && subcuentas > 0) {
                motivo = "tiene subcuentas (es agrupadora)";
            }
        }
        if (motivo != null) {
            throw new IllegalArgumentException("La cuenta " + cuenta.getCodigo() + " - " + cuenta.getNombre()
                    + " " + motivo + "; el parámetro " + clave
                    + " solo puede apuntar a una cuenta hoja activa que acepta movimientos");
        }
        return cuenta;
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
