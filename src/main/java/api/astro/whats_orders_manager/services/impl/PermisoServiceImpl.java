package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.config.MatrizPermisos;
import api.astro.whats_orders_manager.enums.Permiso;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;
import api.astro.whats_orders_manager.repositories.PermisoRepository;
import api.astro.whats_orders_manager.services.PermisoService;
import api.astro.whats_orders_manager.services.UsuarioPermisoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * IMPLEMENTACIÓN DEL SERVICIO DE PERMISOS
 * WhatsApp Orders Manager
 * ============================================================================
 * Implementa la lógica de verificación de permisos basada en roles.
 * Valida estado del usuario (activo, no bloqueado) antes de verificar permisos.
 * Ahora incluye métodos para trabajar con la base de datos.
 * 
 * @version 2.0 - Sprint 4 (Migrado a BD)
 * @since 23/12/2025
 * ============================================================================
 */
@Slf4j
@Service("permisoService")
public class PermisoServiceImpl implements PermisoService {
    
    private final UsuarioRepository usuarioRepository;
    private final PermisoRepository permisoRepository;
    
    // Inyección lazy para evitar dependencia circular
    private final UsuarioPermisoService usuarioPermisoService;
    
    @Autowired
    public PermisoServiceImpl(
            UsuarioRepository usuarioRepository, 
            PermisoRepository permisoRepository,
            @Lazy UsuarioPermisoService usuarioPermisoService) {
        this.usuarioRepository = usuarioRepository;
        this.permisoRepository = permisoRepository;
        this.usuarioPermisoService = usuarioPermisoService;
    }
    
    @Override
    public boolean tienePermiso(Usuario usuario, Permiso permiso) {
        if (!esUsuarioValido(usuario) || permiso == null) {
            log.warn("Verificación de permiso rechazada - Usuario inválido o permiso nulo");
            return false;
        }
        
        boolean tiene = MatrizPermisos.tienePermiso(usuario.getRol(), permiso);
        
        if (!tiene) {
            log.debug("Usuario {} (rol: {}) no tiene permiso: {}", 
                usuario.getNombre(), usuario.getRol(), permiso.getNombre());
        }
        
        return tiene;
    }
    
    @Override
    public boolean tienePermiso(Integer idUsuario, Permiso permiso) {
        if (idUsuario == null || permiso == null) {
            return false;
        }
        
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        return usuarioOpt.map(usuario -> tienePermiso(usuario, permiso)).orElse(false);
    }
    
    @Override
    public boolean tienePermisoByUsername(String username, Permiso permiso) {
        if (username == null || username.isEmpty() || permiso == null) {
            log.warn("Username o permiso nulo en verificación");
            return false;
        }
        
        try {
            // Buscar usuario por nombre o teléfono (login flexible)
            Optional<Usuario> usuarioOpt = usuarioRepository.findByNombre(username);
            if (usuarioOpt.isEmpty()) {
                usuarioOpt = usuarioRepository.findByTelefono(username);
            }
            
            if (usuarioOpt.isEmpty()) {
                log.warn("Usuario no encontrado para username: {}", username);
                return false;
            }
            
            boolean resultado = tienePermiso(usuarioOpt.get(), permiso);
            log.debug("Verificación de permiso - Usuario: {}, Permiso: {}, Resultado: {}", 
                    username, permiso.name(), resultado);
            return resultado;
            
        } catch (Exception e) {
            log.error("Error al verificar permiso por username: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean tieneAlgunPermiso(Usuario usuario, Permiso... permisos) {
        if (!esUsuarioValido(usuario) || permisos == null || permisos.length == 0) {
            return false;
        }
        
        return MatrizPermisos.tieneAlgunPermiso(usuario.getRol(), permisos);
    }
    
    @Override
    public boolean tieneAlgunPermisoByUsername(String username, Permiso... permisos) {
        if (username == null || username.isEmpty() || permisos == null || permisos.length == 0) {
            log.warn("Username o permisos nulos en verificación");
            return false;
        }
        
        try {
            // Buscar usuario por nombre o teléfono
            Optional<Usuario> usuarioOpt = usuarioRepository.findByNombre(username);
            if (usuarioOpt.isEmpty()) {
                usuarioOpt = usuarioRepository.findByTelefono(username);
            }
            
            if (usuarioOpt.isEmpty()) {
                log.warn("Usuario no encontrado para username: {}", username);
                return false;
            }
            
            return tieneAlgunPermiso(usuarioOpt.get(), permisos);
            
        } catch (Exception e) {
            log.error("Error al verificar permisos por username: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean tieneTodosLosPermisos(Usuario usuario, Permiso... permisos) {
        if (!esUsuarioValido(usuario) || permisos == null || permisos.length == 0) {
            return false;
        }
        
        return MatrizPermisos.tieneTodosLosPermisos(usuario.getRol(), permisos);
    }
    
    @Override
    public Set<Permiso> obtenerPermisos(Usuario usuario) {
        if (!esUsuarioValido(usuario)) {
            return Collections.emptySet();
        }
        
        return MatrizPermisos.getPermisos(usuario.getRol());
    }
    
    @Override
    public Map<String, List<Permiso>> obtenerPermisosPorCategoria(Usuario usuario) {
        if (!esUsuarioValido(usuario)) {
            return Collections.emptyMap();
        }
        
        return MatrizPermisos.getPermisosPorCategoria(usuario.getRol());
    }
    
    @Override
    public boolean puedeAcceder(Usuario usuario, String recurso) {
        if (!esUsuarioValido(usuario) || recurso == null || recurso.isEmpty()) {
            return false;
        }
        
        // Mapeo de recursos a permisos
        Map<String, Permiso> mapaRecursos = obtenerMapaRecursos();
        Permiso permisoRequerido = mapaRecursos.get(recurso.toLowerCase());
        
        if (permisoRequerido == null) {
            log.warn("Recurso desconocido: {}", recurso);
            return false;
        }
        
        return tienePermiso(usuario, permisoRequerido);
    }
    
    @Override
    public boolean esUsuarioValido(Usuario usuario) {
        if (usuario == null) {
            return false;
        }
        
        if (!usuario.getActivo()) {
            log.debug("Usuario {} está desactivado", usuario.getNombre());
            return false;
        }
        
        // Verificar bloqueo - tratar null como false (no bloqueado)
        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            log.debug("Usuario {} está bloqueado", usuario.getNombre());
            return false;
        }
        
        return true;
    }
    
    @Override
    public Set<Permiso> obtenerPermisosCriticos(Usuario usuario) {
        Set<Permiso> permisos = obtenerPermisos(usuario);
        
        return permisos.stream()
            .filter(Permiso::esCritico)
            .collect(Collectors.toSet());
    }
    
    @Override
    public Map<String, Object> compararPermisos(Usuario usuario1, Usuario usuario2) {
        Map<String, Object> comparacion = new LinkedHashMap<>();
        
        Set<Permiso> permisos1 = obtenerPermisos(usuario1);
        Set<Permiso> permisos2 = obtenerPermisos(usuario2);
        
        // Permisos en común
        Set<Permiso> comunes = new HashSet<>(permisos1);
        comunes.retainAll(permisos2);
        
        // Permisos solo en usuario1
        Set<Permiso> soloUsuario1 = new HashSet<>(permisos1);
        soloUsuario1.removeAll(permisos2);
        
        // Permisos solo en usuario2
        Set<Permiso> soloUsuario2 = new HashSet<>(permisos2);
        soloUsuario2.removeAll(permisos1);
        
        comparacion.put("usuario1", Map.of(
            "id", usuario1.getIdUsuario(),
            "nombre", usuario1.getNombre(),
            "rol", usuario1.getRol(),
            "totalPermisos", permisos1.size()
        ));
        
        comparacion.put("usuario2", Map.of(
            "id", usuario2.getIdUsuario(),
            "nombre", usuario2.getNombre(),
            "rol", usuario2.getRol(),
            "totalPermisos", permisos2.size()
        ));
        
        comparacion.put("enComun", comunes.size());
        comparacion.put("permisosEnComun", comunes.stream()
            .map(Permiso::getNombre)
            .collect(Collectors.toList()));
        
        comparacion.put("soloEnUsuario1", soloUsuario1.size());
        comparacion.put("permisosSoloUsuario1", soloUsuario1.stream()
            .map(Permiso::getNombre)
            .collect(Collectors.toList()));
        
        comparacion.put("soloEnUsuario2", soloUsuario2.size());
        comparacion.put("permisosSoloUsuario2", soloUsuario2.stream()
            .map(Permiso::getNombre)
            .collect(Collectors.toList()));
        
        return comparacion;
    }
    
    @Override
    public Map<String, Object> obtenerEstadisticasPermisos() {
        Map<String, Object> estadisticas = new LinkedHashMap<>();
        
        // Estadísticas por rol
        Map<String, Integer> permisosPorRol = MatrizPermisos.getEstadisticasPermisos();
        estadisticas.put("permisosPorRol", permisosPorRol);
        
        // Total de permisos en el sistema
        estadisticas.put("totalPermisosEnSistema", Permiso.values().length);
        
        // Categorías de permisos
        Map<String, Long> permisosPorCategoria = Arrays.stream(Permiso.values())
            .collect(Collectors.groupingBy(Permiso::getCategoria, Collectors.counting()));
        estadisticas.put("permisosPorCategoria", permisosPorCategoria);
        
        // Permisos críticos
        long permisosCriticos = Arrays.stream(Permiso.values())
            .filter(Permiso::esCritico)
            .count();
        estadisticas.put("permisosCriticos", permisosCriticos);
        
        // Usuarios por rol
        Map<String, Long> usuariosPorRol = usuarioRepository.findAll().stream()
            .filter(Usuario::getActivo)
            .collect(Collectors.groupingBy(Usuario::getRol, Collectors.counting()));
        estadisticas.put("usuariosActivosPorRol", usuariosPorRol);
        
        // Total de usuarios activos
        long usuariosActivos = usuarioRepository.countByActivo(true);
        estadisticas.put("totalUsuariosActivos", usuariosActivos);
        
        // Total de usuarios bloqueados
        long usuariosBloqueados = usuarioRepository.countByBloqueado(true);
        estadisticas.put("totalUsuariosBloqueados", usuariosBloqueados);
        
        return estadisticas;
    }
    
    /**
     * Obtiene el mapa de recursos a permisos para validación rápida
     */
    private Map<String, Permiso> obtenerMapaRecursos() {
        Map<String, Permiso> mapa = new HashMap<>();
        
        // Facturación
        mapa.put("/facturas", Permiso.FACTURA_VER);
        mapa.put("/facturas/crear", Permiso.FACTURA_CREAR);
        mapa.put("/facturas/editar", Permiso.FACTURA_EDITAR);
        mapa.put("/facturas/eliminar", Permiso.FACTURA_ELIMINAR);
        mapa.put("/facturas/anular", Permiso.FACTURA_ANULAR);
        mapa.put("/facturas/exportar", Permiso.FACTURA_EXPORTAR);
        mapa.put("/facturas/enviar", Permiso.FACTURA_ENVIAR_EMAIL);
        
        // Clientes
        mapa.put("/clientes", Permiso.CLIENTE_VER);
        mapa.put("/clientes/crear", Permiso.CLIENTE_CREAR);
        mapa.put("/clientes/editar", Permiso.CLIENTE_EDITAR);
        mapa.put("/clientes/eliminar", Permiso.CLIENTE_ELIMINAR);
        mapa.put("/clientes/exportar", Permiso.CLIENTE_EXPORTAR);
        
        // Productos
        mapa.put("/productos", Permiso.PRODUCTO_VER);
        mapa.put("/productos/crear", Permiso.PRODUCTO_CREAR);
        mapa.put("/productos/editar", Permiso.PRODUCTO_EDITAR);
        mapa.put("/productos/eliminar", Permiso.PRODUCTO_ELIMINAR);
        mapa.put("/productos/inventario", Permiso.PRODUCTO_AJUSTAR_INVENTARIO);
        mapa.put("/productos/exportar", Permiso.PRODUCTO_EXPORTAR);
        
        // Reportes
        mapa.put("/reportes/ventas", Permiso.REPORTE_VENTAS);
        mapa.put("/reportes/productos", Permiso.REPORTE_PRODUCTOS);
        mapa.put("/reportes/clientes", Permiso.REPORTE_CLIENTES);
        mapa.put("/reportes/dashboard", Permiso.REPORTE_DASHBOARD);
        
        // Configuración
        mapa.put("/configuracion", Permiso.CONFIG_VER);
        mapa.put("/configuracion/empresa", Permiso.CONFIG_EDITAR_EMPRESA);
        mapa.put("/configuracion/facturacion", Permiso.CONFIG_EDITAR_FACTURACION);
        mapa.put("/configuracion/email", Permiso.CONFIG_EDITAR_EMAIL);
        mapa.put("/configuracion/whatsapp", Permiso.CONFIG_EDITAR_WHATSAPP);
        
        // Notificaciones
        mapa.put("/notificaciones", Permiso.NOTIFICACION_VER);
        mapa.put("/notificaciones/crear", Permiso.NOTIFICACION_CREAR);
        
        // Usuarios
        mapa.put("/admin/usuarios", Permiso.USUARIO_VER);
        mapa.put("/admin/usuarios/crear", Permiso.USUARIO_CREAR);
        mapa.put("/admin/usuarios/editar", Permiso.USUARIO_EDITAR);
        mapa.put("/admin/usuarios/eliminar", Permiso.USUARIO_ELIMINAR);
        
        // Auditoría
        mapa.put("/auditoria", Permiso.AUDITORIA_VER);
        
        // Sistema
        mapa.put("/sistema/logs", Permiso.SISTEMA_VER_LOGS);
        mapa.put("/sistema/backup", Permiso.SISTEMA_BACKUP);
        mapa.put("/sistema/mantenimiento", Permiso.SISTEMA_MANTENIMIENTO);
        
        return mapa;
    }
    
    // ===== IMPLEMENTACIÓN DE MÉTODOS PARA LA BASE DE DATOS =====
    
    @Override
    public List<api.astro.whats_orders_manager.models.Permiso> obtenerTodosActivos() {
        log.debug("Obteniendo todos los permisos activos desde BD");
        return permisoRepository.findByActivoTrue();
    }
    
    @Override
    public List<api.astro.whats_orders_manager.models.Permiso> obtenerTodos() {
        log.debug("Obteniendo todos los permisos desde BD");
        return permisoRepository.findAll();
    }
    
    @Override
    public Optional<api.astro.whats_orders_manager.models.Permiso> buscarPorCodigo(String codigo) {
        log.debug("Buscando permiso por código: {}", codigo);
        return permisoRepository.findByCodigo(codigo);
    }
    
    @Override
    public Optional<api.astro.whats_orders_manager.models.Permiso> buscarPorId(Long id) {
        log.debug("Buscando permiso por ID: {}", id);
        return permisoRepository.findById(id);
    }
    
    @Override
    public api.astro.whats_orders_manager.models.Permiso guardar(api.astro.whats_orders_manager.models.Permiso permiso) {
        log.info("Guardando permiso: {} (ID: {})", permiso.getCodigo(), permiso.getIdPermiso());
        return permisoRepository.save(permiso);
    }
    
    // ===== MÉTODOS PARA MIGRACIÓN A BD (basados en código String) =====
    
    @Override
    public boolean tienePermisoPorCodigo(String username, String codigoPermiso) {
        log.debug("Verificando permiso por código: {} para usuario: {}", codigoPermiso, username);
        
        if (username == null || username.isEmpty() || codigoPermiso == null || codigoPermiso.isEmpty()) {
            log.warn("Username o código de permiso vacío en verificación");
            return false;
        }
        
        try {
            // Buscar usuario por nombre o teléfono (login flexible)
            Optional<Usuario> usuarioOpt = usuarioRepository.findByNombre(username);
            if (usuarioOpt.isEmpty()) {
                usuarioOpt = usuarioRepository.findByTelefono(username);
            }
            
            if (usuarioOpt.isEmpty()) {
                log.warn("Usuario no encontrado para username: {}", username);
                return false;
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Validar estado del usuario
            if (!esUsuarioValido(usuario)) {
                log.debug("Usuario {} no es válido (inactivo o bloqueado)", username);
                return false;
            }
            
            // OPCIÓN 1: Usar UsuarioPermisoService para obtener permisos efectivos (incluye personalizados)
            // Esta es la forma correcta que respeta la lógica: DENEGADOS > CONCEDIDOS > ROL
            try {
                // Inyectar UsuarioPermisoService si está disponible
                if (usuarioPermisoService != null) {
                    boolean tienePermiso = usuarioPermisoService.tienePermiso(usuario.getIdUsuario(), codigoPermiso);
                    log.debug("Verificación con permisos personalizados - Usuario: {}, Permiso: {}, Resultado: {}", 
                            username, codigoPermiso, tienePermiso);
                    return tienePermiso;
                }
            } catch (Exception e) {
                log.warn("Error al verificar permisos personalizados, usando solo permisos del rol: {}", e.getMessage());
            }
            
            // FALLBACK: Si no hay UsuarioPermisoService, verificar solo permisos del rol
            Set<api.astro.whats_orders_manager.models.Permiso> permisosRol = new HashSet<>();
            
            // Verificar si el usuario tiene rolEntity asignado
            if (usuario.getRolEntity() != null) {
                permisosRol = usuario.getRolEntity().getPermisos();
            } else if (usuario.getRol() != null) {
                // Fallback: buscar rol por código (String legacy)
                log.warn("Usuario {} tiene rol '{}' como String pero no tiene rolEntity", 
                         username, usuario.getRol());
                // Por ahora devolver false, idealmente debería buscar el rol en BD
                log.warn("No se puede verificar permiso sin rolEntity asignado");
                return false;
            } else {
                log.warn("Usuario {} no tiene rol asignado", username);
                return false;
            }
            
            // Verificar si el rol tiene el permiso activo
            boolean tieneEnRol = permisosRol.stream()
                .anyMatch(p -> p.getCodigo().equals(codigoPermiso) && Boolean.TRUE.equals(p.getActivo()));
            
            log.debug("Verificación de permiso del rol - Usuario: {}, Permiso: {}, Resultado: {}", 
                    username, codigoPermiso, tieneEnRol);
            
            return tieneEnRol;
            
        } catch (Exception e) {
            log.error("Error al verificar permiso por código para usuario {}: {}", 
                    username, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean tieneAlgunPermisoPorCodigo(String username, String... codigosPermiso) {
        if (username == null || username.isEmpty() || codigosPermiso == null || codigosPermiso.length == 0) {
            log.warn("Username o códigos de permisos vacíos en verificación");
            return false;
        }
        
        log.debug("Verificando si usuario {} tiene alguno de {} permisos", username, codigosPermiso.length);
        
        // Verificar si tiene al menos uno de los permisos
        for (String codigo : codigosPermiso) {
            if (tienePermisoPorCodigo(username, codigo)) {
                log.debug("Usuario {} tiene permiso: {}", username, codigo);
                return true;
            }
        }
        
        log.debug("Usuario {} no tiene ninguno de los permisos solicitados", username);
        return false;
    }
}
