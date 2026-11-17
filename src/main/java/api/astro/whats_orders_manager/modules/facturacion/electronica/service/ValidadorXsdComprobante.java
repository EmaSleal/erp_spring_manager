package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Único punto de validación del XML firmado contra el XSD v4.4 de Hacienda, antes de cualquier llamada HTTP (T-036).
 * Los Schema se compilan una vez por tipo (ver {@link XmlValidator}).
 */
@Component
public class ValidadorXsdComprobante {

    /**
     * @return errores "línea N, columna M: mensaje" (lista vacía si el XML es válido)
     * @throws IllegalStateException si el XSD del tipo no está disponible
     */
    public List<String> validar(String xml, TipoComprobanteElectronico tipo) {
        return XmlValidator.erroresContraXsd(xml, tipo);
    }
}
