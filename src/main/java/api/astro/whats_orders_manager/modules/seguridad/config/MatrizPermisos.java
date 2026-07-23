package api.astro.whats_orders_manager.modules.seguridad.config;

import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;

import java.util.*;

/**
 * ============================================================================
 * MATRIZ DE PERMISOS
 * ERP Orders Manager
 * ============================================================================
 * Define qué permisos tiene cada rol del sistema.
 * Esta es la fuente de verdad para el control de acceso basado en roles (RBAC).
 * 
 * @version 1.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */
public class MatrizPermisos {
    
    private static final Map<String, Set<Permiso>> PERMISOS_POR_ROL = new HashMap<>();
    
    static {
        inicializarPermisos();
    }
    
    /**
     * Inicializa la matriz de permisos por rol
     */
    private static void inicializarPermisos() {
        
        // ==================== ROL: VENDEDOR ====================
        Set<Permiso> permisosVendedor = new HashSet<>();
        
        // Facturación (CRUD completo)
        permisosVendedor.add(Permiso.FACTURA_VER);
        permisosVendedor.add(Permiso.FACTURA_CREAR);
        permisosVendedor.add(Permiso.FACTURA_EDITAR);
        permisosVendedor.add(Permiso.FACTURA_EXPORTAR);
        permisosVendedor.add(Permiso.FACTURA_ENVIAR_EMAIL);
        
        // Clientes (CRUD completo)
        permisosVendedor.add(Permiso.CLIENTE_VER);
        permisosVendedor.add(Permiso.CLIENTE_CREAR);
        permisosVendedor.add(Permiso.CLIENTE_EDITAR);
        permisosVendedor.add(Permiso.CLIENTE_EXPORTAR);
        
        // Productos (solo lectura y búsqueda)
        permisosVendedor.add(Permiso.PRODUCTO_VER);
        
        // Reportes (básicos)
        permisosVendedor.add(Permiso.REPORTE_VENTAS);
        permisosVendedor.add(Permiso.REPORTE_DASHBOARD);
        
        // Notificaciones (propias)
        permisosVendedor.add(Permiso.NOTIFICACION_VER);
        permisosVendedor.add(Permiso.NOTIFICACION_MARCAR_LEIDA);
        permisosVendedor.add(Permiso.NOTIFICACION_CONFIGURAR);
        
        PERMISOS_POR_ROL.put("VENDEDOR", Collections.unmodifiableSet(permisosVendedor));
        
        // ==================== ROL: GERENTE ====================
        Set<Permiso> permisosGerente = new HashSet<>();
        
        // Hereda todos los permisos del vendedor
        permisosGerente.addAll(permisosVendedor);
        
        // Facturación (permisos adicionales)
        permisosGerente.add(Permiso.FACTURA_ELIMINAR);
        permisosGerente.add(Permiso.FACTURA_ANULAR);
        
        // Clientes (permisos adicionales)
        permisosGerente.add(Permiso.CLIENTE_ELIMINAR);
        
        // Productos (CRUD completo)
        permisosGerente.add(Permiso.PRODUCTO_CREAR);
        permisosGerente.add(Permiso.PRODUCTO_EDITAR);
        permisosGerente.add(Permiso.PRODUCTO_ELIMINAR);
        permisosGerente.add(Permiso.PRODUCTO_AJUSTAR_INVENTARIO);
        permisosGerente.add(Permiso.PRODUCTO_EXPORTAR);
        
        // Reportes (todos)
        permisosGerente.add(Permiso.REPORTE_PRODUCTOS);
        permisosGerente.add(Permiso.REPORTE_CLIENTES);
        permisosGerente.add(Permiso.REPORTE_EXPORTAR_PDF);
        permisosGerente.add(Permiso.REPORTE_EXPORTAR_EXCEL);
        permisosGerente.add(Permiso.REPORTE_EXPORTAR_CSV);
        
        // Configuración (solo lectura)
        permisosGerente.add(Permiso.CONFIG_VER);

        // Notificaciones (puede crear)
        permisosGerente.add(Permiso.NOTIFICACION_CREAR);

        // Inventario (solo lectura)
        permisosGerente.add(Permiso.INVENTARIO_VER);

        // Proveedores y compras (solo lectura)
        permisosGerente.add(Permiso.PROVEEDOR_VER);
        permisosGerente.add(Permiso.ORDEN_COMPRA_VER);
        permisosGerente.add(Permiso.CUENTA_PAGAR_VER);

        // Recursos Humanos (GERENTE obtiene 5; RRHH_GESTIONAR_CATALOGO_SALARIAL reservado a ADMIN)
        permisosGerente.add(Permiso.RRHH_VER);
        permisosGerente.add(Permiso.RRHH_GESTIONAR_EMPLEADOS);
        permisosGerente.add(Permiso.RRHH_GESTIONAR_CONTRATOS);
        permisosGerente.add(Permiso.RRHH_APROBAR_AUSENCIAS);
        permisosGerente.add(Permiso.RRHH_VER_CATALOGO_SALARIAL);

        PERMISOS_POR_ROL.put("GERENTE", Collections.unmodifiableSet(permisosGerente));
        
        // ==================== ROL: ADMIN ====================
        // ADMIN has all permissions — always kept in sync with the enum automatically
        Set<Permiso> permisosAdmin = EnumSet.allOf(Permiso.class);

        PERMISOS_POR_ROL.put("ADMIN", Collections.unmodifiableSet(permisosAdmin));
    }
    
    /**
     * Obtiene todos los permisos de un rol
     */
    public static Set<Permiso> getPermisos(String rol) {
        if (rol == null || rol.isEmpty()) {
            return Collections.emptySet();
        }
        return PERMISOS_POR_ROL.getOrDefault(rol.toUpperCase(), Collections.emptySet());
    }
    
    /**
     * Verifica si un rol tiene un permiso específico
     */
    public static boolean tienePermiso(String rol, Permiso permiso) {
        if (rol == null || permiso == null) {
            return false;
        }
        Set<Permiso> permisos = getPermisos(rol);
        return permisos.contains(permiso);
    }
    
    /**
     * Verifica si un rol tiene al menos uno de los permisos especificados
     */
    public static boolean tieneAlgunPermiso(String rol, Permiso... permisos) {
        if (rol == null || permisos == null || permisos.length == 0) {
            return false;
        }
        Set<Permiso> permisosRol = getPermisos(rol);
        for (Permiso permiso : permisos) {
            if (permisosRol.contains(permiso)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Verifica si un rol tiene todos los permisos especificados
     */
    public static boolean tieneTodosLosPermisos(String rol, Permiso... permisos) {
        if (rol == null || permisos == null || permisos.length == 0) {
            return false;
        }
        Set<Permiso> permisosRol = getPermisos(rol);
        for (Permiso permiso : permisos) {
            if (!permisosRol.contains(permiso)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Obtiene todos los roles disponibles
     */
    public static Set<String> getRoles() {
        return PERMISOS_POR_ROL.keySet();
    }
    
    /**
     * Obtiene un mapa con todos los permisos agrupados por categoría para un rol
     */
    public static Map<String, List<Permiso>> getPermisosPorCategoria(String rol) {
        Set<Permiso> permisos = getPermisos(rol);
        Map<String, List<Permiso>> permisosPorCategoria = new LinkedHashMap<>();
        
        for (Permiso permiso : permisos) {
            String categoria = permiso.getCategoria();
            permisosPorCategoria
                .computeIfAbsent(categoria, k -> new ArrayList<>())
                .add(permiso);
        }
        
        return permisosPorCategoria;
    }
    
    /**
     * Obtiene estadísticas de permisos por rol
     */
    public static Map<String, Integer> getEstadisticasPermisos() {
        Map<String, Integer> estadisticas = new LinkedHashMap<>();
        
        for (Map.Entry<String, Set<Permiso>> entry : PERMISOS_POR_ROL.entrySet()) {
            estadisticas.put(entry.getKey(), entry.getValue().size());
        }
        
        return estadisticas;
    }
    
    /**
     * Obtiene la diferencia de permisos entre dos roles
     */
    public static Set<Permiso> getDiferenciaPermisos(String rol1, String rol2) {
        Set<Permiso> permisos1 = new HashSet<>(getPermisos(rol1));
        Set<Permiso> permisos2 = getPermisos(rol2);
        
        permisos1.removeAll(permisos2);
        return permisos1;
    }
    
    /**
     * Imprime la matriz completa de permisos (útil para debugging)
     */
    public static String imprimirMatriz() {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(80)).append("\n");
        sb.append("MATRIZ DE PERMISOS DEL SISTEMA\n");
        sb.append("=".repeat(80)).append("\n\n");
        
        for (Map.Entry<String, Set<Permiso>> entry : PERMISOS_POR_ROL.entrySet()) {
            String rol = entry.getKey();
            Set<Permiso> permisos = entry.getValue();
            
            sb.append(String.format("ROL: %s (%d permisos)\n", rol, permisos.size()));
            sb.append("-".repeat(80)).append("\n");
            
            Map<String, List<Permiso>> porCategoria = new LinkedHashMap<>();
            for (Permiso permiso : permisos) {
                porCategoria
                    .computeIfAbsent(permiso.getCategoria(), k -> new ArrayList<>())
                    .add(permiso);
            }
            
            for (Map.Entry<String, List<Permiso>> catEntry : porCategoria.entrySet()) {
                sb.append(String.format("\n  %s:\n", catEntry.getKey()));
                for (Permiso permiso : catEntry.getValue()) {
                    sb.append(String.format("    - %s: %s\n", 
                        permiso.getNombre(), permiso.getDescripcion()));
                }
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
