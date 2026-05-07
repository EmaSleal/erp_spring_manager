## 💻 CÓDIGO IMPLEMENTADO

```java
/**
 * Carga los módulos disponibles según el rol del usuario
 * 
 * Permisos por rol:
 * - ADMIN: Acceso total (todos los módulos)
 * - USER: Módulos operativos + reportes (sin configuración/usuarios)
 * - VENDEDOR: Solo crear facturas + consultar catálogos
 * - VISUALIZADOR: Solo lectura de información
 * 
 * @param rol Rol del usuario (ADMIN, USER, VENDEDOR, VISUALIZADOR)
 * @return Lista de módulos con permisos y estado
 */
private List<ModuloDTO> cargarModulosSegunRol(String rol) {
    List<ModuloDTO> modulos = new ArrayList<>();
    
    boolean esAdmin = "ADMIN".equals(rol);
    boolean esUser = "USER".equals(rol);
    boolean esVendedor = "VENDEDOR".equals(rol);
    boolean esVisualizador = "VISUALIZADOR".equals(rol);

    // Clientes (ADMIN, USER, VENDEDOR, VISUALIZADOR pueden ver)
    modulos.add(new ModuloDTO(
            "Clientes",
            "Gestión de clientes",
            "fas fa-users",
            "#4CAF50",
            "/clientes",
            true,
            esAdmin || esUser || esVendedor || esVisualizador
    ));

    // Productos (ADMIN, USER, VENDEDOR, VISUALIZADOR pueden ver)
    modulos.add(new ModuloDTO(
            "Productos",
            "Catálogo de productos",
            "fas fa-box",
            "#FF9800",
            "/productos",
            true,
            esAdmin || esUser || esVendedor || esVisualizador
    ));

    // Facturas (ADMIN, USER, VENDEDOR, VISUALIZADOR pueden ver)
    modulos.add(new ModuloDTO(
            "Facturas",
            "Gestión de facturas",
            "fas fa-file-invoice-dollar",
            "#9C27B0",
            "/facturas",
            true,
            esAdmin || esUser || esVendedor || esVisualizador
    ));

    // Usuarios (solo ADMIN)
    modulos.add(new ModuloDTO(
            "Usuarios",
            "Gestión de usuarios",
            "fas fa-user-cog",
            "#3F51B5",
            "/usuarios",
            true,
            esAdmin
    ));

    // Pedidos (próximamente - ADMIN, USER, VENDEDOR)
    modulos.add(new ModuloDTO(
            "Pedidos",
            "Gestión de pedidos",
            "fas fa-shopping-cart",
            "#F44336",
            "/pedidos",
            false,  // No implementado aún
            esAdmin || esUser || esVendedor
    ));

    // Reportes (ADMIN y USER pueden ver)
    modulos.add(new ModuloDTO(
            "Reportes",
            "Informes y estadísticas",
            "fas fa-chart-bar",
            "#00BCD4",
            "/reportes",
            false,  // No implementado aún
            esAdmin || esUser
    ));

    // Configuración (solo ADMIN)
    modulos.add(new ModuloDTO(
            "Configuración",
            "Ajustes del sistema",
            "fas fa-cog",
            "#607D8B",
            "/configuracion",
            true,
            esAdmin
    ));

    return modulos;
}
```

---

