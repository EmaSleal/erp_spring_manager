## 📝 INSTRUCCIONES PARA TESTING MANUAL

### Paso 1: Iniciar la aplicación
```bash
mvn spring-boot:run
```

### Paso 2: Probar acceso con diferentes roles

**Como ADMIN o USER:**
1. Iniciar sesión
2. Verificar que aparece "Reportes" en el sidebar (con icono de gráfico)
3. Click en "Reportes"
4. Debe cargar el dashboard de reportes (/reportes)
5. Verificar que el navbar se carga correctamente
6. Navegar a los 3 tipos de reportes (Ventas, Clientes, Productos)

**Como VENDEDOR o VISUALIZADOR:**
1. Iniciar sesión
2. Verificar que NO aparece "Reportes" en el sidebar
3. Intentar acceder directamente a /reportes
4. Debe redirigir a /error/403 (Acceso Denegado)

### Paso 3: Verificar elementos visuales

**En /reportes (index):**
- ✅ Navbar carga correctamente
- ✅ Breadcrumbs: "Inicio > Reportes"
- ✅ 4 cards de estadísticas (Facturas, Clientes, Productos, Usuarios)
- ✅ 3 cards de tipos de reportes (Ventas, Clientes, Productos)
- ✅ Secciones informativas

**En /reportes/ventas:**
- ✅ Navbar carga correctamente
- ✅ Breadcrumbs: "Inicio > Reportes > Ventas"
- ✅ Filtros funcionan (fechas, cliente)
- ✅ Estadísticas se calculan correctamente
- ✅ Tabla muestra facturas
- ✅ Botones de exportación (aunque aún no funcionales)

**En /reportes/clientes:**
- ✅ Navbar carga correctamente
- ✅ Breadcrumbs: "Inicio > Reportes > Clientes"
- ✅ Filtros funcionan (estado, deuda)
- ✅ Estadísticas se calculan correctamente
- ✅ Tabla muestra clientes

**En /reportes/productos:**
- ✅ Navbar carga correctamente
- ✅ Breadcrumbs: "Inicio > Reportes > Productos"
- ✅ Filtros funcionan (stock bajo, sin ventas)
- ✅ Estadísticas se calculan correctamente
- ✅ Tabla muestra productos

---

