# Guía: Tablas Responsive con Vista de Tarjetas

## 📱 Descripción

Sistema que convierte automáticamente tablas HTML en tarjetas responsive en dispositivos móviles, sin necesidad de JavaScript adicional.

## 🎯 Características

- **Desktop**: Tabla tradicional completa
- **Móvil**: Tarjetas individuales con toda la información
- **Automático**: Cambio mediante CSS (sin JavaScript)
- **Reutilizable**: Estructura consistente para todas las listas
- **Accesible**: Mantiene toda la funcionalidad en ambos formatos

## 📐 Estructura

### 1. Incluir CSS

```html
<link rel="stylesheet" th:href="@{/shared/css/responsive-table.css}">
```

### 2. Estructura HTML

```html
<div class="responsive-table-container">
    
    <!-- DESKTOP: Tabla (se oculta automáticamente en móvil) -->
    <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
                <tr>
                    <th>#</th>
                    <th>Nombre</th>
                    <th>Email</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="item : ${items}">
                    <td th:text="${item.id}">1</td>
                    <td th:text="${item.nombre}">Nombre</td>
                    <td th:text="${item.email}">email@example.com</td>
                    <td>
                        <button class="btn btn-sm btn-primary">Editar</button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- MÓVIL: Tarjetas (se ocultan automáticamente en desktop) -->
    <div class="mobile-card-view">
        <div th:each="item : ${items}" class="mobile-card">
            
            <!-- Header -->
            <div class="mobile-card-header">
                <h3 class="mobile-card-title" th:text="${item.nombre}">Nombre</h3>
                <span class="mobile-card-badge badge bg-primary">#<span th:text="${item.id}">1</span></span>
            </div>

            <!-- Body -->
            <div class="mobile-card-body">
                <div class="mobile-card-row">
                    <span class="mobile-card-label">Email</span>
                    <span class="mobile-card-value" th:text="${item.email}">email</span>
                </div>
            </div>

            <!-- Footer -->
            <div class="mobile-card-footer">
                <button class="btn btn-sm btn-primary w-100">
                    <i class="bi bi-pencil me-1"></i> Editar
                </button>
            </div>
        </div>
    </div>

</div>
```

## 🎨 Componentes de Tarjetas

### Header (Título y Badge)

```html
<div class="mobile-card-header">
    <h3 class="mobile-card-title">Título Principal</h3>
    <span class="mobile-card-badge badge bg-success">Activo</span>
</div>
```

### Body (Filas de información)

```html
<div class="mobile-card-body">
    <div class="mobile-card-row">
        <span class="mobile-card-label">Etiqueta</span>
        <span class="mobile-card-value">Valor</span>
    </div>
</div>
```

### Footer (Acciones)

```html
<div class="mobile-card-footer">
    <button class="btn btn-sm btn-primary">Acción 1</button>
    <button class="btn btn-sm btn-secondary">Acción 2</button>
</div>
```

### Avatar (Para usuarios/perfiles)

```html
<div class="mobile-card-avatar">
    <div class="avatar-circle bg-primary">
        <span>A</span>
    </div>
    <div class="mobile-card-avatar-info">
        <div class="mobile-card-avatar-name">Nombre Usuario</div>
        <div class="mobile-card-avatar-subtitle">@username</div>
    </div>
</div>
```

## 🎯 Variantes de Tarjetas

### Bordes coloreados

```html
<div class="mobile-card card-success">   <!-- Verde -->
<div class="mobile-card card-warning">   <!-- Amarillo -->
<div class="mobile-card card-danger">    <!-- Rojo -->
<div class="mobile-card card-info">      <!-- Azul claro -->
<div class="mobile-card card-primary">   <!-- Azul -->
```

### Tarjeta compacta

```html
<div class="mobile-card mobile-card-compact">
    <!-- Menos padding y espaciado -->
</div>
```

### Tarjeta destacada

```html
<div class="mobile-card mobile-card-featured">
    <!-- Fondo degradado y borde doble -->
</div>
```

## 🔧 Estados y Badges

### Estados con iconos

```html
<span class="mobile-card-status status-active">
    <i class="bi bi-check-circle-fill"></i>
    <span>Activo</span>
</span>

<span class="mobile-card-status status-inactive">
    <i class="bi bi-x-circle-fill"></i>
    <span>Inactivo</span>
</span>

<span class="mobile-card-status status-pending">
    <i class="bi bi-clock-fill"></i>
    <span>Pendiente</span>
</span>

<span class="mobile-card-status status-blocked">
    <i class="bi bi-shield-fill-x"></i>
    <span>Bloqueado</span>
</span>
```

## 📊 Ejemplo Completo: Lista de Usuarios

```html
<div class="responsive-table-container">
    
    <!-- DESKTOP: Tabla -->
    <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
                <tr>
                    <th>#</th>
                    <th>Usuario</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="user, iter : ${usuarios}">
                    <td th:text="${iter.index + 1}">1</td>
                    <td>
                        <div class="d-flex align-items-center">
                            <div class="avatar-circle bg-primary me-2">
                                <span th:text="${#strings.substring(user.nombre, 0, 1)}">A</span>
                            </div>
                            <div>
                                <strong th:text="${user.nombre}">Nombre</strong><br>
                                <small th:text="${user.email}">email</small>
                            </div>
                        </div>
                    </td>
                    <td><span class="badge bg-info" th:text="${user.rol}">ROL</span></td>
                    <td>
                        <span class="badge" 
                              th:classappend="${user.activo ? 'bg-success' : 'bg-secondary'}"
                              th:text="${user.activo ? 'Activo' : 'Inactivo'}">Estado</span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-primary">Editar</button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- MÓVIL: Tarjetas -->
    <div class="mobile-card-view">
        <div th:each="user : ${usuarios}" 
             class="mobile-card"
             th:classappend="${user.activo ? 'card-success' : 'card-secondary'}">
            
            <div class="mobile-card-header">
                <div class="mobile-card-avatar">
                    <div class="avatar-circle bg-primary">
                        <span th:text="${#strings.substring(user.nombre, 0, 1)}">A</span>
                    </div>
                    <div class="mobile-card-avatar-info">
                        <div class="mobile-card-avatar-name" th:text="${user.nombre}">Nombre</div>
                        <div class="mobile-card-avatar-subtitle" th:text="${user.email}">email</div>
                    </div>
                </div>
                <span class="mobile-card-status"
                      th:classappend="${user.activo ? 'status-active' : 'status-inactive'}">
                    <i class="bi" th:classappend="${user.activo ? 'bi-check-circle-fill' : 'bi-x-circle-fill'}"></i>
                    <span th:text="${user.activo ? 'Activo' : 'Inactivo'}">Estado</span>
                </span>
            </div>

            <div class="mobile-card-body">
                <div class="mobile-card-row">
                    <span class="mobile-card-label">Rol</span>
                    <span class="mobile-card-value">
                        <span class="badge bg-info" th:text="${user.rol}">ROL</span>
                    </span>
                </div>
                <div class="mobile-card-row">
                    <span class="mobile-card-label">Teléfono</span>
                    <span class="mobile-card-value" th:text="${user.telefono}">999999999</span>
                </div>
            </div>

            <div class="mobile-card-footer mobile-card-actions">
                <button class="btn btn-sm btn-primary">
                    <i class="bi bi-pencil me-1"></i> Editar
                </button>
                <button class="btn btn-sm btn-danger">
                    <i class="bi bi-trash me-1"></i> Eliminar
                </button>
            </div>
        </div>
    </div>

</div>
```

## 🔄 Migración de Tablas Existentes

### Paso 1: Agregar contenedor

```html
<!-- ANTES -->
<div class="table-responsive">
    <table class="table">...</table>
</div>

<!-- DESPUÉS -->
<div class="responsive-table-container">
    <div class="table-responsive">
        <table class="table">...</table>
    </div>
    <div class="mobile-card-view">
        <!-- Agregar tarjetas aquí -->
    </div>
</div>
```

### Paso 2: Duplicar datos en formato tarjeta

Para cada fila `<tr th:each="item : ${items}">`, crear una tarjeta correspondiente:

```html
<div th:each="item : ${items}" class="mobile-card">
    <!-- Mapear columnas a mobile-card-row -->
</div>
```

### Paso 3: Incluir CSS

```html
<link rel="stylesheet" th:href="@{/shared/css/responsive-table.css}">
```

## 🎨 Personalización

### Cambiar breakpoint responsive

Por defecto: 768px. Para modificar, editar en `responsive-table.css`:

```css
@media (max-width: 767px) {  /* Cambiar valor aquí */
    /* Reglas móvil */
}
```

### Colores de estados

```css
.mobile-card-status.status-active {
    background-color: rgba(40, 167, 69, 0.1);
    color: #28a745;
}
```

### Espaciado de tarjetas

```css
.mobile-card {
    margin-bottom: 1rem;  /* Ajustar aquí */
    padding: 1.5rem;      /* Ajustar aquí */
}
```

## 🧪 Testing

### Chrome DevTools

1. F12 → Toggle device toolbar (Ctrl+Shift+M)
2. Seleccionar dispositivo móvil
3. Verificar que se muestren tarjetas

### Breakpoints a probar

- **iPhone SE**: 375px
- **iPhone 12**: 390px
- **Samsung Galaxy**: 360px
- **iPad**: 768px (límite)
- **Desktop**: 1024px+

## ✅ Checklist de Implementación

- [ ] Incluir `responsive-table.css` en el `<head>`
- [ ] Envolver tabla en `.responsive-table-container`
- [ ] Crear vista `.mobile-card-view` con tarjetas
- [ ] Duplicar datos de tabla en tarjetas
- [ ] Mapear acciones de botones en ambas vistas
- [ ] Probar en diferentes tamaños de pantalla
- [ ] Verificar que eventos JavaScript funcionen en ambas vistas

## 🚀 Ventajas

✅ **Sin JavaScript**: Todo mediante CSS  
✅ **Reutilizable**: Misma estructura para todas las listas  
✅ **Performante**: Sin overhead de renderizado  
✅ **Accesible**: Mantiene semántica HTML  
✅ **SEO Friendly**: Contenido duplicado pero oculto por CSS  

## ⚠️ Consideraciones

- Los datos se duplican en HTML (tabla + tarjetas)
- Incrementa ligeramente el tamaño del HTML
- Eventos JS deben aplicarse a ambas vistas
- No usar para tablas con > 100 filas (performance)

## 📚 Recursos

- Demo completo: `docs/snippets/ejemplo-tabla-responsive.html`
- CSS fuente: `static/shared/css/responsive-table.css`
- Bootstrap Icons: https://icons.getbootstrap.com/

---

**Actualizado**: 20 de enero de 2026  
**Versión**: 1.0.0
