package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.ParametroSistema;
import api.astro.whats_orders_manager.models.enums.CategoriaParametro;
import api.astro.whats_orders_manager.models.enums.TipoDatoParametro;
import api.astro.whats_orders_manager.repositories.ParametroSistemaRepository;
import api.astro.whats_orders_manager.services.ParametroSistemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de parámetros del sistema
 */
@Slf4j
@Service
@Transactional
public class ParametroSistemaServiceImpl implements ParametroSistemaService {

    @Autowired
    private ParametroSistemaRepository parametroRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ParametroSistema> obtenerPorClave(String clave) {
        log.debug("Obteniendo parámetro por clave: {}", clave);
        return parametroRepository.findByClave(clave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametroSistema> obtenerPorCategoria(CategoriaParametro categoria) {
        log.debug("Obteniendo parámetros de categoría: {}", categoria);
        return parametroRepository.findByCategoria(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametroSistema> obtenerEditables() {
        log.debug("Obteniendo parámetros editables");
        return parametroRepository.findByEditable(true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParametroSistema> obtenerTodos() {
        log.debug("Obteniendo todos los parámetros");
        return parametroRepository.findAll();
    }

    @Override
    public ParametroSistema guardarParametro(ParametroSistema parametro) {
        log.debug("Guardando parámetro: {}", parametro.getClave());
        
        if (parametro == null) {
            throw new IllegalArgumentException("El parámetro no puede ser null");
        }
        
        if (parametro.getClave() == null || parametro.getClave().trim().isEmpty()) {
            throw new IllegalArgumentException("La clave del parámetro es obligatoria");
        }
        
        // Validar que el valor sea válido para el tipo de dato
        if (!parametro.isValido()) {
            throw new IllegalArgumentException("El valor del parámetro no es válido para el tipo de dato especificado");
        }
        
        ParametroSistema guardado = parametroRepository.save(parametro);
        log.info("Parámetro guardado correctamente: {}", guardado.getClave());
        
        return guardado;
    }

    @Override
    public ParametroSistema crearParametro(String clave, String valor, TipoDatoParametro tipoDato,
                                           CategoriaParametro categoria, String descripcion, Boolean editable) {
        log.debug("Creando nuevo parámetro: {}", clave);
        
        // Verificar si ya existe
        if (existeParametro(clave)) {
            throw new IllegalArgumentException("Ya existe un parámetro con la clave: " + clave);
        }
        
        ParametroSistema nuevoParametro = ParametroSistema.builder()
                .clave(clave)
                .valor(valor)
                .tipoDato(tipoDato)
                .categoria(categoria)
                .descripcion(descripcion)
                .editable(editable != null ? editable : true)
                .build();
        
        return guardarParametro(nuevoParametro);
    }

    @Override
    public ParametroSistema actualizarValor(String clave, String nuevoValor) {
        log.debug("Actualizando valor del parámetro: {}", clave);
        
        Optional<ParametroSistema> parametroOpt = obtenerPorClave(clave);
        
        if (parametroOpt.isEmpty()) {
            throw new IllegalArgumentException("No existe el parámetro con clave: " + clave);
        }
        
        ParametroSistema parametro = parametroOpt.get();
        
        // Verificar si es editable
        if (!parametro.getEditable()) {
            throw new IllegalArgumentException("El parámetro " + clave + " no es editable");
        }
        
        parametro.setValor(nuevoValor);
        
        // Validar que el nuevo valor sea válido
        if (!parametro.isValido()) {
            throw new IllegalArgumentException("El nuevo valor no es válido para el tipo de dato del parámetro");
        }
        
        ParametroSistema actualizado = parametroRepository.save(parametro);
        log.info("Valor del parámetro {} actualizado correctamente", clave);
        
        return actualizado;
    }

    @Override
    public void eliminarParametro(String clave) {
        log.debug("Eliminando parámetro: {}", clave);
        
        Optional<ParametroSistema> parametroOpt = obtenerPorClave(clave);
        
        if (parametroOpt.isEmpty()) {
            throw new IllegalArgumentException("No existe el parámetro con clave: " + clave);
        }
        
        ParametroSistema parametro = parametroOpt.get();
        
        // Verificar si es editable (solo se pueden eliminar parámetros editables)
        if (!parametro.getEditable()) {
            throw new IllegalArgumentException("El parámetro " + clave + " no puede ser eliminado");
        }
        
        parametroRepository.delete(parametro);
        log.info("Parámetro {} eliminado correctamente", clave);
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerValorString(String clave, String valorPorDefecto) {
        Optional<ParametroSistema> parametroOpt = obtenerPorClave(clave);
        
        if (parametroOpt.isEmpty()) {
            log.debug("Parámetro {} no encontrado, retornando valor por defecto: {}", clave, valorPorDefecto);
            return valorPorDefecto;
        }
        
        return parametroOpt.get().getValorAsString();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer obtenerValorInteger(String clave, Integer valorPorDefecto) {
        Optional<ParametroSistema> parametroOpt = obtenerPorClave(clave);
        
        if (parametroOpt.isEmpty()) {
            log.debug("Parámetro {} no encontrado, retornando valor por defecto: {}", clave, valorPorDefecto);
            return valorPorDefecto;
        }
        
        try {
            return parametroOpt.get().getValorAsInteger();
        } catch (Exception e) {
            log.warn("Error al convertir parámetro {} a Integer, retornando valor por defecto", clave);
            return valorPorDefecto;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean obtenerValorBoolean(String clave, Boolean valorPorDefecto) {
        Optional<ParametroSistema> parametroOpt = obtenerPorClave(clave);
        
        if (parametroOpt.isEmpty()) {
            log.debug("Parámetro {} no encontrado, retornando valor por defecto: {}", clave, valorPorDefecto);
            return valorPorDefecto;
        }
        
        try {
            return parametroOpt.get().getValorAsBoolean();
        } catch (Exception e) {
            log.warn("Error al convertir parámetro {} a Boolean, retornando valor por defecto", clave);
            return valorPorDefecto;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerValorDecimal(String clave, BigDecimal valorPorDefecto) {
        Optional<ParametroSistema> parametroOpt = obtenerPorClave(clave);
        
        if (parametroOpt.isEmpty()) {
            log.debug("Parámetro {} no encontrado, retornando valor por defecto: {}", clave, valorPorDefecto);
            return valorPorDefecto;
        }
        
        try {
            return parametroOpt.get().getValorAsBigDecimal();
        } catch (Exception e) {
            log.warn("Error al convertir parámetro {} a BigDecimal, retornando valor por defecto", clave);
            return valorPorDefecto;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeParametro(String clave) {
        boolean existe = parametroRepository.existsByClave(clave);
        log.debug("¿Existe parámetro {}?: {}", clave, existe);
        return existe;
    }

    @Override
    public void inicializarParametrosPorDefecto() {
        log.info("Inicializando parámetros por defecto del sistema");
        
        // CATEGORÍA: GENERAL
        crearParametroSiNoExiste("sistema.nombre", "Sistema de Facturación", 
                TipoDatoParametro.STRING, CategoriaParametro.GENERAL, 
                "Nombre del sistema", false);
        
        crearParametroSiNoExiste("sistema.version", "1.0.0", 
                TipoDatoParametro.STRING, CategoriaParametro.GENERAL, 
                "Versión del sistema", false);
        
        crearParametroSiNoExiste("sistema.modo_mantenimiento", "false", 
                TipoDatoParametro.BOOLEAN, CategoriaParametro.GENERAL, 
                "Indica si el sistema está en modo mantenimiento", true);
        
        // CATEGORÍA: FACTURACIÓN
        crearParametroSiNoExiste("factura.serie_predeterminada", "A", 
                TipoDatoParametro.STRING, CategoriaParametro.FACTURACION, 
                "Serie predeterminada para nuevas facturas", true);
        
        crearParametroSiNoExiste("factura.folio_inicial", "1", 
                TipoDatoParametro.INTEGER, CategoriaParametro.FACTURACION, 
                "Folio inicial para facturas", true);
        
        crearParametroSiNoExiste("factura.dias_vencimiento_predeterminado", "30", 
                TipoDatoParametro.INTEGER, CategoriaParametro.FACTURACION, 
                "Días de vencimiento predeterminado para facturas", true);
        
        crearParametroSiNoExiste("factura.dias_antes_vencimiento_alerta", "7", 
                TipoDatoParametro.INTEGER, CategoriaParametro.FACTURACION, 
                "Días antes del vencimiento para mostrar alerta", true);
        
        crearParametroSiNoExiste("factura.iva_predeterminado", "16", 
                TipoDatoParametro.DECIMAL, CategoriaParametro.FACTURACION, 
                "Porcentaje de IVA predeterminado", true);
        
        // CATEGORÍA: WHATSAPP
        crearParametroSiNoExiste("whatsapp.mensajes_automaticos_activo", "true", 
                TipoDatoParametro.BOOLEAN, CategoriaParametro.WHATSAPP, 
                "Activar mensajes automáticos de WhatsApp", true);
        
        crearParametroSiNoExiste("whatsapp.timeout_respuesta_segundos", "300", 
                TipoDatoParametro.INTEGER, CategoriaParametro.WHATSAPP, 
                "Timeout en segundos para esperar respuesta", true);
        
        // CATEGORÍA: NOTIFICACIONES
        crearParametroSiNoExiste("notificaciones.email_activo", "true", 
                TipoDatoParametro.BOOLEAN, CategoriaParametro.NOTIFICACIONES, 
                "Activar notificaciones por email", true);
        
        crearParametroSiNoExiste("notificaciones.whatsapp_activo", "true", 
                TipoDatoParametro.BOOLEAN, CategoriaParametro.NOTIFICACIONES, 
                "Activar notificaciones por WhatsApp", true);
        
        // CATEGORÍA: REPORTES
        crearParametroSiNoExiste("reportes.registros_por_pagina", "20", 
                TipoDatoParametro.INTEGER, CategoriaParametro.REPORTES, 
                "Número de registros por página en reportes", true);
        
        crearParametroSiNoExiste("reportes.formato_exportacion_predeterminado", "PDF", 
                TipoDatoParametro.STRING, CategoriaParametro.REPORTES, 
                "Formato predeterminado para exportar reportes (PDF, EXCEL, CSV)", true);
        
        // CATEGORÍA: SEGURIDAD
        crearParametroSiNoExiste("seguridad.sesion_timeout_minutos", "30", 
                TipoDatoParametro.INTEGER, CategoriaParametro.SEGURIDAD, 
                "Tiempo de expiración de sesión en minutos", true);
        
        crearParametroSiNoExiste("seguridad.intentos_login_maximos", "5", 
                TipoDatoParametro.INTEGER, CategoriaParametro.SEGURIDAD, 
                "Número máximo de intentos de login antes de bloqueo", true);
        
        crearParametroSiNoExiste("seguridad.bloqueo_cuenta_minutos", "15", 
                TipoDatoParametro.INTEGER, CategoriaParametro.SEGURIDAD, 
                "Tiempo de bloqueo de cuenta en minutos", true);
        
        log.info("Parámetros por defecto inicializados correctamente");
    }
    
    /**
     * Método auxiliar para crear un parámetro solo si no existe
     */
    private void crearParametroSiNoExiste(String clave, String valor, TipoDatoParametro tipoDato,
                                          CategoriaParametro categoria, String descripcion, Boolean editable) {
        if (!existeParametro(clave)) {
            crearParametro(clave, valor, tipoDato, categoria, descripcion, editable);
            log.debug("Parámetro creado: {}", clave);
        } else {
            log.debug("Parámetro ya existe: {}", clave);
        }
    }
}
