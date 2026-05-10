## 📁 ARCHIVOS MODIFICADOS

### 1. **clientes/clientes.html** (Modificado)
**Ubicación:** `src/main/resources/templates/clientes/clientes.html`

#### Cambios realizados:

#### a) Badge de "Solo lectura" en el header:
```html
<p class="text-muted mb-0">
    Administra tu base de clientes
    <span sec:authorize="hasAnyRole('VENDEDOR', 'VISUALIZADOR')" class="badge bg-secondary ms-2">
        <i class="bi bi-eye-fill me-1"></i>Solo lectura
    </span>
</p>
```

#### b) Botón "Agregar Cliente" solo para ADMIN y USER:
```html
<button id="open-modal" class="btn btn-primary" sec:authorize="hasAnyRole('ADMIN', 'USER')">
    <i class="fas fa-plus me-2"></i>Agregar Cliente
</button>
```

#### c) Botones de acción en la tabla:
```html
<td class="text-center">
    <div class="btn-group" role="group">
        <!-- Botón Editar: solo ADMIN y USER -->
        <button th:attr="onclick=|openEditModal(...)|"
                class="btn btn-sm btn-warning"
                sec:authorize="hasAnyRole('ADMIN', 'USER')">
            <i class="fas fa-edit"></i> Editar
        </button>
        
        <!-- Botón Eliminar: solo ADMIN y USER -->
        <a th:href="@{/clientes/eliminar/{id}(id=${cliente.idCliente})}"
           class="btn btn-sm btn-danger"
           sec:authorize="hasAnyRole('ADMIN', 'USER')">
            <i class="fas fa-trash"></i> Eliminar
        </a>
    </div>
    
    <!-- Badge para usuarios de solo lectura -->
    <span sec:authorize="hasAnyRole('VENDEDOR', 'VISUALIZADOR')" 
          class="badge bg-secondary">
        <i class="bi bi-eye-fill"></i> Solo lectura
    </span>
</td>
```

**Resultado:**
- ✅ ADMIN y USER: Ven todos los botones de acción
- ✅ VENDEDOR y VISUALIZADOR: Solo ven badge "Solo lectura"
- ✅ Botón "Agregar Cliente" oculto para roles sin permisos

---

### 2. **productos/productos.html** (Modificado)
**Ubicación:** `src/main/resources/templates/productos/productos.html`

#### Cambios realizados:

#### a) Badge de "Solo lectura" en el header:
```html
<p class="text-muted mb-0">
    Administra tu catálogo de productos
    <span sec:authorize="hasAnyRole('VENDEDOR', 'VISUALIZADOR')" class="badge bg-secondary ms-2">
        <i class="bi bi-eye-fill me-1"></i>Solo lectura
    </span>
</p>
```

#### b) Botón "Agregar Producto" solo para ADMIN y USER:
```html
<button type="button" class="btn btn-success" onclick="openAddModal()" 
        sec:authorize="hasAnyRole('ADMIN', 'USER')">
    <i class="fas fa-plus me-2"></i>Agregar Producto
</button>
```

#### c) Pasar rol del usuario a JavaScript:
```html
<script th:inline="javascript">
    const productos = /*[[${productos}]]*/ [];
    const userRole = /*[[${#authentication.principal.authorities[0].authority}]]*/ 'ROLE_USER';
</script>
```

**Nota:** Los botones de la tabla se manejan en JavaScript (ver siguiente sección)

---

### 3. **productos.js** (Modificado)
**Ubicación:** `src/main/resources/static/js/productos.js`

#### Cambios realizados:

#### Lógica de renderizado de botones según rol:
```javascript
<td class="text-center">
    <!-- Botón Editar: oculto si es VENDEDOR o VISUALIZADOR -->
    <button class="btn btn-sm btn-warning me-1" 
            onclick="openEditModal(...)"
            title="Editar"
            style="${(typeof userRole !== 'undefined' && 
                    (userRole === 'ROLE_VENDEDOR' || userRole === 'ROLE_VISUALIZADOR')) 
                    ? 'display:none;' : ''}">
        <i class="fas fa-edit"></i>
    </button>
    
    <!-- Botón Eliminar: oculto si es VENDEDOR o VISUALIZADOR -->
    <button class="btn btn-sm btn-danger" 
            onclick="eliminarProducto(...)"
            title="Eliminar"
            style="${(typeof userRole !== 'undefined' && 
                    (userRole === 'ROLE_VENDEDOR' || userRole === 'ROLE_VISUALIZADOR')) 
                    ? 'display:none;' : ''}">
        <i class="fas fa-trash"></i>
    </button>
    
    <!-- Badge Solo Lectura: visible para VENDEDOR y VISUALIZADOR -->
    ${(typeof userRole !== 'undefined' && 
       (userRole === 'ROLE_VENDEDOR' || userRole === 'ROLE_VISUALIZADOR')) ? 
        '<span class="badge bg-secondary"><i class="bi bi-eye-fill"></i> Solo lectura</span>' : ''}
</td>
```

**Ventaja:** Como la tabla se renderiza dinámicamente con JavaScript, necesitamos esta lógica para aplicar restricciones en el lado del cliente.

---

### 4. **facturas/facturas.html** (Modificado)
**Ubicación:** `src/main/resources/templates/facturas/facturas.html`

#### Cambios realizados:

#### a) Badge de "Solo lectura" en el header (solo VISUALIZADOR):
```html
<p class="text-muted mb-0">
    Administra las facturas y pedidos de tus clientes
    <span sec:authorize="hasRole('VISUALIZADOR')" class="badge bg-secondary ms-2">
        <i class="bi bi-eye-fill me-1"></i>Solo lectura
    </span>
</p>
```

#### b) Botón "Nueva Factura" para ADMIN, USER y VENDEDOR:
```html
<button type="button" class="btn btn-success" onclick="openNuevaFacturaModal()" 
        sec:authorize="hasAnyRole('ADMIN', 'USER', 'VENDEDOR')">
    <i class="fas fa-plus me-2"></i>Nueva Factura
</button>
```

**Nota:** VENDEDOR puede crear facturas (su función principal)

#### c) Botones de acción en la tabla:
```html
<td class="text-center">
    <!-- Botón Ver Detalle: visible para TODOS -->
    <button th:attr="data-id=${factura.idFactura}" 
            onclick="openModal(this)"
            class="btn btn-sm btn-primary me-1"
            title="Ver Detalle">
        <i class="fas fa-eye"></i>
    </button>
    
    <!-- Botón Eliminar: solo ADMIN y USER -->
    <a th:href="@{/facturas/eliminar/{id}(id=${factura.idFactura})}"
       onclick="return confirmarEliminacion(event)"
       class="btn btn-sm btn-danger"
       title="Eliminar"
       sec:authorize="hasAnyRole('ADMIN', 'USER')">
        <i class="fas fa-trash"></i>
    </a>
    
    <!-- Badge para usuarios de solo lectura o vendedores -->
    <span sec:authorize="hasAnyRole('VENDEDOR', 'VISUALIZADOR')" 
          class="badge bg-secondary">
        <i class="bi bi-eye-fill"></i> Solo lectura
    </span>
</td>
```

**Lógica:**
- ✅ Todos pueden ver detalles de factura
- ✅ ADMIN y USER pueden eliminar
- ✅ VENDEDOR puede crear pero NO eliminar
- ✅ VISUALIZADOR solo puede ver (no crear ni eliminar)

---

### 5. **components/sidebar.html** (Modificado)
**Ubicación:** `src/main/resources/templates/components/sidebar.html`

#### Cambios realizados:

#### a) Clientes - Badge de solo lectura:
```html
<li class="menu-item">
    <a th:href="@{/clientes}" class="menu-link" data-module="clientes">
        <div class="menu-icon">
            <i class="fas fa-users"></i>
        </div>
        <span class="menu-text">Clientes</span>
        <span class="menu-badge bg-secondary" 
              sec:authorize="hasAnyRole('VENDEDOR', 'VISUALIZADOR')" 
              style="font-size: 0.65rem; padding: 2px 6px;">
            <i class="bi bi-eye-fill"></i>
        </span>
    </a>
</li>
```

#### b) Productos - Badge de solo lectura:
```html
<li class="menu-item">
    <a th:href="@{/productos}" class="menu-link" data-module="productos">
        <div class="menu-icon">
            <i class="fas fa-box"></i>
        </div>
        <span class="menu-text">Productos</span>
        <span class="menu-badge bg-secondary" 
              sec:authorize="hasAnyRole('VENDEDOR', 'VISUALIZADOR')" 
              style="font-size: 0.65rem; padding: 2px 6px;">
            <i class="bi bi-eye-fill"></i>
        </span>
    </a>
</li>
```

#### c) Facturas - Badge dinámico según rol:
```html
<li class="menu-item">
    <a th:href="@{/facturas}" class="menu-link" data-module="facturas">
        <div class="menu-icon">
            <i class="fas fa-file-invoice"></i>
        </div>
        <span class="menu-text">Facturas</span>
        
        <!-- Badge verde para VENDEDOR (puede crear) -->
        <span class="menu-badge bg-success" 
              sec:authorize="hasRole('VENDEDOR')" 
              style="font-size: 0.65rem; padding: 2px 6px;">
            <i class="bi bi-plus-circle-fill"></i>
        </span>
        
        <!-- Badge gris para VISUALIZADOR (solo lectura) -->
        <span class="menu-badge bg-secondary" 
              sec:authorize="hasRole('VISUALIZADOR')" 
              style="font-size: 0.65rem; padding: 2px 6px;">
            <i class="bi bi-eye-fill"></i>
        </span>
    </a>
</li>
```

**Resultado:**
- ✅ ADMIN y USER: Sin badges (acceso completo implícito)
- ✅ VENDEDOR: Ícono verde en Facturas (puede crear), ícono gris en Clientes y Productos
- ✅ VISUALIZADOR: Ícono gris en todos los módulos (solo lectura)
- ✅ Todos los roles pueden acceder a los módulos principales
- ✅ Restricciones se aplican dentro de cada módulo

---

