package api.astro.whats_orders_manager.modules.configuracion.dto;

import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfiguracionEmpresaDTO {

    /**
     * ID de la configuración
     */
    private Integer idConfiguracion;

    /**
     * Razón social de la empresa (obligatorio)
     */
    private String razonSocial;

    /**
     * Nombre comercial de la empresa
     */
    private String nombreComercial;

    /**
     * RFC de la empresa
     */
    private String rfc;

    /**
     * Régimen fiscal
     */
    private String regimenFiscal;

    /**
     * Calle de la dirección
     */
    private String direccionCalle;

    /**
     * Número exterior/interior
     */
    private String direccionNumero;

    /**
     * Colonia
     */
    private String direccionColonia;

    /**
     * Ciudad
     */
    private String direccionCiudad;

    /**
     * Estado
     */
    private String direccionEstado;

    /**
     * Código postal
     */
    private String direccionCodigoPostal;

    /**
     * País (default: México)
     */
    private String direccionPais;

    /**
     * Teléfono de contacto
     */
    private String telefono;

    /**
     * Email de contacto
     */
    private String email;

    /**
     * Sitio web
     */
    private String sitioWeb;

    /**
     * URL del logo de la empresa
     */
    private String logoUrl;

    /**
     * Color primario del tema (hexadecimal)
     */
    private String colorPrimario;

    /**
     * Color secundario del tema (hexadecimal)
     */
    private String colorSecundario;

    /**
     * Dirección completa formateada (solo lectura)
     */
    private String direccionCompleta;

    /**
     * Indica si tiene logo configurado (solo lectura)
     */
    private Boolean tieneLogoConfigurado;

    /**
     * Indica si tiene datos fiscales completos (solo lectura)
     */
    private Boolean datosFiscalesCompletos;

    // ==================== FACTURACIÓN ELECTRÓNICA COSTA RICA ====================

    private String numeroIdentificacion;
    private TipoIdentificacion tipoIdentificacion;
    private String codigoProvincia;
    private String canton;
    private String distrito;
    private String barrio;
    private String otrasSenas;
    private String codigoActividad;
    private String nombreComercialFe;
}
