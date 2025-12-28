package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.Permiso;
import api.astro.whats_orders_manager.models.Rol;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.models.UsuarioPermiso;
import api.astro.whats_orders_manager.repositories.PermisoRepository;
import api.astro.whats_orders_manager.repositories.RolRepository;
import api.astro.whats_orders_manager.repositories.UsuarioPermisoRepository;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;
import api.astro.whats_orders_manager.services.UsuarioPermisoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de permisos personalizados por usuario
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UsuarioPermisoServiceImpl implements UsuarioPermisoService {

    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;

    @Override
    public UsuarioPermiso concederPermiso(Integer idUsuario, Long idPermiso, Integer concedidoPorId) {
        log.info("Concediendo permiso {} a usuario {}", idPermiso, idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));
        
        Permiso permiso = permisoRepository.findById(idPermiso)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado: " + idPermiso));
        
        Usuario concedidoPor = usuarioRepository.findById(concedidoPorId)
                .orElseThrow(() -> new RuntimeException("Usuario que concede no encontrado: " + concedidoPorId));
        
        // Verificar si ya existe este permiso personalizado
        Optional<UsuarioPermiso> existente = buscarPermisoPersonalizado(idUsuario, permiso.getCodigo());
        
        if (existente.isPresent()) {
            UsuarioPermiso up = existente.get();
            // Si ya existe pero estaba denegado, cambiarlo a concedido
            if (up.getTipo() == UsuarioPermiso.TipoPermisoUsuario.DENEGADO) {
                log.info("Cambiando permiso {} de DENEGADO a CONCEDIDO para usuario {}", 
                        permiso.getCodigo(), idUsuario);
                up.setTipo(UsuarioPermiso.TipoPermisoUsuario.CONCEDIDO);
                up.setConcedidoPor(concedidoPor);
                return usuarioPermisoRepository.save(up);
            } else {
                log.warn("El permiso {} ya está concedido al usuario {}", permiso.getCodigo(), idUsuario);
                return up;
            }
        }
        
        // Crear nuevo permiso personalizado
        UsuarioPermiso nuevoPermiso = new UsuarioPermiso();
        nuevoPermiso.setUsuario(usuario);
        nuevoPermiso.setPermiso(permiso);
        nuevoPermiso.setTipo(UsuarioPermiso.TipoPermisoUsuario.CONCEDIDO);
        nuevoPermiso.setConcedidoPor(concedidoPor);
        
        UsuarioPermiso guardado = usuarioPermisoRepository.save(nuevoPermiso);
        log.info("Permiso {} concedido exitosamente a usuario {}", permiso.getCodigo(), idUsuario);
        
        return guardado;
    }

    @Override
    public UsuarioPermiso denegarPermiso(Integer idUsuario, Long idPermiso, Integer concedidoPorId) {
        log.info("Denegando permiso {} a usuario {}", idPermiso, idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));
        
        Permiso permiso = permisoRepository.findById(idPermiso)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado: " + idPermiso));
        
        Usuario concedidoPor = usuarioRepository.findById(concedidoPorId)
                .orElseThrow(() -> new RuntimeException("Usuario que deniega no encontrado: " + concedidoPorId));
        
        // Verificar si ya existe este permiso personalizado
        Optional<UsuarioPermiso> existente = buscarPermisoPersonalizado(idUsuario, permiso.getCodigo());
        
        if (existente.isPresent()) {
            UsuarioPermiso up = existente.get();
            // Si ya existe pero estaba concedido, cambiarlo a denegado
            if (up.getTipo() == UsuarioPermiso.TipoPermisoUsuario.CONCEDIDO) {
                log.info("Cambiando permiso {} de CONCEDIDO a DENEGADO para usuario {}", 
                        permiso.getCodigo(), idUsuario);
                up.setTipo(UsuarioPermiso.TipoPermisoUsuario.DENEGADO);
                up.setConcedidoPor(concedidoPor);
                return usuarioPermisoRepository.save(up);
            } else {
                log.warn("El permiso {} ya está denegado al usuario {}", permiso.getCodigo(), idUsuario);
                return up;
            }
        }
        
        // Crear nuevo permiso personalizado denegado
        UsuarioPermiso nuevoPermiso = new UsuarioPermiso();
        nuevoPermiso.setUsuario(usuario);
        nuevoPermiso.setPermiso(permiso);
        nuevoPermiso.setTipo(UsuarioPermiso.TipoPermisoUsuario.DENEGADO);
        nuevoPermiso.setConcedidoPor(concedidoPor);
        
        UsuarioPermiso guardado = usuarioPermisoRepository.save(nuevoPermiso);
        log.info("Permiso {} denegado exitosamente a usuario {}", permiso.getCodigo(), idUsuario);
        
        return guardado;
    }

    @Override
    @Transactional
    public void removerPermisoPersonalizado(Integer idUsuario, String codigoPermiso) {
        log.info("Removiendo permiso personalizado {} del usuario {}", codigoPermiso, idUsuario);
        
        Optional<UsuarioPermiso> permiso = buscarPermisoPersonalizado(idUsuario, codigoPermiso);
        
        if (permiso.isPresent()) {
            usuarioPermisoRepository.delete(permiso.get());
            log.info("Permiso personalizado {} removido del usuario {}", codigoPermiso, idUsuario);
        } else {
            log.warn("No se encontró permiso personalizado {} para usuario {}", codigoPermiso, idUsuario);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioPermiso> obtenerPermisosPersonalizados(Integer idUsuario) {
        log.debug("Obteniendo permisos personalizados del usuario {}", idUsuario);
        return usuarioPermisoRepository.findByUsuario_IdUsuario(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Boolean> obtenerPermisosEfectivos(Integer idUsuario) {
        log.debug("Calculando permisos efectivos para usuario {}", idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));
        
        Map<String, Boolean> permisosEfectivos = new HashMap<>();
        
        // 1. Agregar permisos del rol (base)
        Set<Permiso> permisosRol = new HashSet<>();
        
        if (usuario.getRolEntity() != null) {
            permisosRol = usuario.getRolEntity().getPermisos();
        } else if (usuario.getRol() != null) {
            // Fallback: buscar rol por código
            Optional<Rol> rolOpt = rolRepository.findByCodigo(usuario.getRol());
            if (rolOpt.isPresent()) {
                permisosRol = rolOpt.get().getPermisos();
            }
        }
        
        for (Permiso permiso : permisosRol) {
            if (permiso.getActivo()) {
                permisosEfectivos.put(permiso.getCodigo(), true);
            }
        }
        
        // 2. Aplicar permisos personalizados (sobrescriben los del rol)
        List<UsuarioPermiso> permisosPersonalizados = obtenerPermisosPersonalizados(idUsuario);
        
        for (UsuarioPermiso up : permisosPersonalizados) {
            String codigo = up.getPermiso().getCodigo();
            
            if (up.getTipo() == UsuarioPermiso.TipoPermisoUsuario.DENEGADO) {
                // DENEGADO tiene máxima prioridad - elimina el permiso
                permisosEfectivos.put(codigo, false);
                log.debug("Permiso {} DENEGADO para usuario {}", codigo, idUsuario);
            } else if (up.getTipo() == UsuarioPermiso.TipoPermisoUsuario.CONCEDIDO) {
                // CONCEDIDO agrega el permiso aunque el rol no lo tenga
                permisosEfectivos.put(codigo, true);
                log.debug("Permiso {} CONCEDIDO para usuario {}", codigo, idUsuario);
            }
        }
        
        // 3. Filtrar solo los permisos activos (true)
        return permisosEfectivos.entrySet().stream()
                .filter(Map.Entry::getValue)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioPermiso> obtenerPermisosConcedidos(Integer idUsuario) {
        log.debug("Obteniendo permisos concedidos del usuario {}", idUsuario);
        return usuarioPermisoRepository.findPermisosConcedidos(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioPermiso> obtenerPermisosDenegados(Integer idUsuario) {
        log.debug("Obteniendo permisos denegados del usuario {}", idUsuario);
        return usuarioPermisoRepository.findPermisosDenegados(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioPermiso> buscarPermisoPersonalizado(Integer idUsuario, String codigoPermiso) {
        log.debug("Buscando permiso personalizado {} para usuario {}", codigoPermiso, idUsuario);
        return usuarioPermisoRepository.findByUsuarioAndPermisoCodigo(idUsuario, codigoPermiso);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tienePermiso(Integer idUsuario, String codigoPermiso) {
        log.debug("Verificando si usuario {} tiene permiso {}", idUsuario, codigoPermiso);
        
        Map<String, Boolean> permisosEfectivos = obtenerPermisosEfectivos(idUsuario);
        boolean tienePermiso = permisosEfectivos.getOrDefault(codigoPermiso, false);
        
        log.debug("Usuario {} {} permiso {}", 
                idUsuario, 
                tienePermiso ? "TIENE" : "NO TIENE", 
                codigoPermiso);
        
        return tienePermiso;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> contarPermisosPorUsuario() {
        log.debug("Contando permisos personalizados por usuario");
        
        List<Object[]> resultados = usuarioPermisoRepository.contarPermisosPorUsuario();
        
        return resultados.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],  // nombre usuario
                        row -> (Long) row[1]      // cantidad
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenPermisos(Integer idUsuario) {
        log.debug("Obteniendo resumen de permisos para usuario {}", idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));
        
        Map<String, Object> resumen = new HashMap<>();
        
        // Permisos del rol (base)
        Set<Permiso> permisosRol = new HashSet<>();
        
        // Verificar si el usuario tiene rolEntity asignado
        if (usuario.getRolEntity() != null) {
            permisosRol = usuario.getRolEntity().getPermisos();
        } else if (usuario.getRol() != null) {
            // Si solo tiene el código del rol (String), buscar la entidad
            log.warn("Usuario {} tiene rol '{}' como String pero no tiene rolEntity. Buscando rol en BD...", 
                     idUsuario, usuario.getRol());
            
            Optional<Rol> rolOpt = rolRepository.findByCodigo(usuario.getRol());
            if (rolOpt.isPresent()) {
                permisosRol = rolOpt.get().getPermisos();
                log.info("Rol '{}' encontrado con {} permisos", usuario.getRol(), permisosRol.size());
            } else {
                log.error("Rol '{}' no encontrado en BD para usuario {}", usuario.getRol(), idUsuario);
            }
        } else {
            log.warn("Usuario {} no tiene rol asignado (ni String ni Entity)", idUsuario);
        }
        
        List<Map<String, Object>> permisosRolList = permisosRol.stream()
                .filter(Permiso::getActivo)
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getIdPermiso());
                    map.put("codigo", p.getCodigo());
                    map.put("nombre", p.getNombre());
                    map.put("categoria", p.getCategoria());
                    return map;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("codigo")))
                .collect(Collectors.toList());
        
        // Permisos concedidos adicionales
        List<UsuarioPermiso> concedidos = obtenerPermisosConcedidos(idUsuario);
        List<Map<String, Object>> concedidosList = concedidos.stream()
                .map(up -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", up.getPermiso().getIdPermiso());
                    map.put("codigo", up.getPermiso().getCodigo());
                    map.put("nombre", up.getPermiso().getNombre());
                    map.put("categoria", up.getPermiso().getCategoria());
                    map.put("concedidoPor", up.getConcedidoPor() != null ? 
                            up.getConcedidoPor().getNombre() : "Sistema");
                    map.put("fecha", up.getCreateDate());
                    return map;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("codigo")))
                .collect(Collectors.toList());
        
        // Permisos denegados
        List<UsuarioPermiso> denegados = obtenerPermisosDenegados(idUsuario);
        List<Map<String, Object>> denegadosList = denegados.stream()
                .map(up -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", up.getPermiso().getIdPermiso());
                    map.put("codigo", up.getPermiso().getCodigo());
                    map.put("nombre", up.getPermiso().getNombre());
                    map.put("categoria", up.getPermiso().getCategoria());
                    map.put("concedidoPor", up.getConcedidoPor() != null ? 
                            up.getConcedidoPor().getNombre() : "Sistema");
                    map.put("fecha", up.getCreateDate());
                    return map;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("codigo")))
                .collect(Collectors.toList());
        
        // Obtener nombre del rol (con fallback)
        String nombreRol = "Sin Rol";
        if (usuario.getRolEntity() != null) {
            nombreRol = usuario.getRolEntity().getNombre();
        } else if (usuario.getRol() != null) {
            // Buscar el rol en BD por código
            Optional<Rol> rolOpt = rolRepository.findByCodigo(usuario.getRol());
            nombreRol = rolOpt.map(Rol::getNombre).orElse(usuario.getRol());
        }
        
        resumen.put("rol", nombreRol);
        resumen.put("permisosRol", permisosRolList);
        resumen.put("permisosConcedidos", concedidosList);
        resumen.put("permisosDenegados", denegadosList);
        resumen.put("totalRol", permisosRolList.size());
        resumen.put("totalConcedidos", concedidosList.size());
        resumen.put("totalDenegados", denegadosList.size());
        resumen.put("totalEfectivos", obtenerPermisosEfectivos(idUsuario).size());
        
        log.info("Resumen de permisos para usuario {}: Rol={}, Concedidos={}, Denegados={}, Efectivos={}", 
                idUsuario, 
                permisosRolList.size(),
                concedidosList.size(),
                denegadosList.size(),
                resumen.get("totalEfectivos"));
        
        return resumen;
    }
}
