package api.astro.whats_orders_manager.modules.configuracion.service.impl;

import api.astro.whats_orders_manager.shared.exception.ValidacionException;
import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.configuracion.repository.PresentacionRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.PresentacionService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.UnidadMedidaFe;
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
    public Presentacion save(Presentacion presentacion) {
        validar(presentacion);
        return presentacionRepository.save(presentacion);
    }

    /**
     * ADR-041 / T-034 D5: empacado nulo cuenta como true. No empacada: codigoUnidadFE obligatorio y del enum
     * UnidadMedidaType del XSD. Empacada: opcional, pero si viene debe estar en el enum.
     *
     * @throws ValidacionException con un mensaje fijo si no cumple
     */
    static void validar(Presentacion presentacion) {
        if (presentacion.getEmpacado() == null) {
            presentacion.setEmpacado(true);
        }
        String codigo = presentacion.getCodigoUnidadFE();
        boolean hayCodigo = codigo != null && !codigo.isBlank();
        if (!presentacion.getEmpacado() && !hayCodigo) {
            throw new ValidacionException(
                "Una presentación no empacada requiere el código de unidad de medida de Hacienda");
        }
        if (hayCodigo && !UnidadMedidaFe.esValida(codigo.trim())) {
            throw new ValidacionException(
                "El código de unidad de medida '" + codigo + "' no es válido para Hacienda");
        }
        presentacion.setCodigoUnidadFE(hayCodigo ? codigo.trim() : null);
    }

    @Override
    public void deleteById(Integer id) { presentacionRepository.deleteById(id); }
}