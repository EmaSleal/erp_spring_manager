# 📋 SPRINT 2 - PLAN DE DESARROLLO

**Proyecto:** WhatsApp Orders Manager  
**Sprint:** Sprint 2  
**Fecha de inicio:** Por definir  
**Fecha estimada de finalización:** Por definir  
**Responsable:** Equipo de Desarrollo

---

## 🎯 Objetivos del Sprint 2

1. Implementar módulo de **Configuración del Sistema**
2. Implementar módulo de **Gestión de Usuarios**
3. Mejorar **integración entre módulos existentes**
4. Implementar **reportes básicos**
5. Optimizar **seguridad y permisos**

---

## 📊 Resumen de Fases

| Fase | Descripción | Prioridad | Estimación |
|------|-------------|-----------|------------|
| Fase 1 | Configuración de Empresa | 🔴 Alta | 3 días |
| Fase 2 | Configuración de Facturación | 🔴 Alta | 2 días |
| Fase 3 | Gestión de Usuarios | 🔴 Alta | 4 días |
| Fase 4 | Roles y Permisos | 🟡 Media | 3 días |
| Fase 5 | Notificaciones Básicas | 🟡 Media | 3 días |
| Fase 6 | Reportes y Estadísticas | 🟡 Media | 4 días |
| Fase 7 | Integración de Módulos | 🟢 Baja | 2 días |
| Fase 8 | Testing y Optimización | 🔴 Alta | 3 días |

**Total estimado:** 24 días

---

## 📦 FASE 1: CONFIGURACIÓN DE EMPRESA

### Objetivo
Permitir a los administradores configurar los datos de la empresa que aparecerán en facturas y documentos.

### Tareas

#### 1.1 Modelo de Datos
**Archivo:** `Empresa.java`

```java
@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEmpresa;
    
    @Column(name = "nombre_empresa", nullable = false, length = 200)
    private String nombreEmpresa;
    
    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;
    
    @Column(name = "ruc", length = 20)
    private String ruc;
    
    @Column(name = "direccion", length = 300)
    private String direccion;
    
    @Column(name = "telefono", length = 20)
    private String telefono;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "sitio_web", length = 200)
    private String sitioWeb;
    
    @Column(name = "logo", length = 255)
    private String logo;
    
    @Column(name = "favicon", length = 255)
    private String favicon;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    @Column(name = "fecha_creacion")
    private Timestamp fechaCreacion;
    
    @Column(name = "fecha_modificacion")
    private Timestamp fechaModificacion;
    
    @Column(name = "usuario_modificacion")
    private Integer usuarioModificacion;
}
```

**Script SQL:**
```sql
CREATE TABLE empresa (
    id_empresa INT AUTO_INCREMENT PRIMARY KEY,
    nombre_empresa VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    ruc VARCHAR(20),
    direccion VARCHAR(300),
    telefono VARCHAR(20),
    email VARCHAR(100),
    sitio_web VARCHAR(200),
    logo VARCHAR(255),
    favicon VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    usuario_modificacion INT
);

-- Insertar configuración por defecto
INSERT INTO empresa (nombre_empresa, nombre_comercial, activo) 
VALUES ('Mi Empresa', 'Mi Empresa', TRUE);
```

#### 1.2 Repository, Service y Controller
- `EmpresaRepository.java`
- `EmpresaService.java`
- `EmpresaServiceImpl.java`
- `ConfiguracionController.java`

#### 1.3 Vistas
- `configuracion/empresa.html` - Formulario de datos de empresa
- `configuracion/index.html` - Página principal con tabs

#### 1.4 Funcionalidades
- ✅ Ver datos de la empresa
- ✅ Editar datos de la empresa
- ✅ Upload de logo
- ✅ Upload de favicon
- ✅ Preview de logo y favicon
- ✅ Validaciones (RUC, email, teléfono)

---

## 📦 FASE 2: CONFIGURACIÓN DE FACTURACIÓN

### Objetivo
Configurar parámetros de facturación (serie, numeración, impuestos).

### Tareas

#### 2.1 Modelo de Datos
**Archivo:** `ConfiguracionFacturacion.java`

```java
@Entity
@Table(name = "configuracion_facturacion")
public class ConfiguracionFacturacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "serie_factura", length = 10)
    private String serieFactura = "F001";
    
    @Column(name = "numero_inicial")
    private Integer numeroInicial = 1;
    
    @Column(name = "numero_actual")
    private Integer numeroActual = 1;
    
    @Column(name = "prefijo_factura", length = 10)
    private String prefijoFactura;
    
    @Column(name = "igv", precision = 5, scale = 2)
    private BigDecimal igv = new BigDecimal("18.00");
    
    @Column(name = "moneda", length = 3)
    private String moneda = "PEN";
    
    @Column(name = "terminos_condiciones", columnDefinition = "TEXT")
    private String terminosCondiciones;
    
    @Column(name = "nota_pie_pagina", length = 500)
    private String notaPiePagina;
    
    @Column(name = "incluir_igv_en_precio")
    private Boolean incluirIgvEnPrecio = true;
    
    @Column(name = "activo")
    private Boolean activo = true;
}
```

**Script SQL:**
```sql
CREATE TABLE configuracion_facturacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    serie_factura VARCHAR(10) DEFAULT 'F001',
    numero_inicial INT DEFAULT 1,
    numero_actual INT DEFAULT 1,
    prefijo_factura VARCHAR(10),
    igv DECIMAL(5,2) DEFAULT 18.00,
    moneda VARCHAR(3) DEFAULT 'PEN',
    terminos_condiciones TEXT,
    nota_pie_pagina VARCHAR(500),
    incluir_igv_en_precio BOOLEAN DEFAULT TRUE,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Configuración por defecto
INSERT INTO configuracion_facturacion (serie_factura, numero_inicial, numero_actual, igv, moneda) 
VALUES ('F001', 1, 1, 18.00, 'PEN');
```

#### 2.2 Funcionalidades
- ✅ Configurar serie de factura
- ✅ Configurar número inicial y actual
- ✅ Configurar prefijo
- ✅ Configurar IGV/IVA (%)
- ✅ Configurar moneda
- ✅ Configurar términos y condiciones
- ✅ Configurar nota de pie de página
- ✅ Toggle: incluir IGV en precio

#### 2.3 Integración
- Modificar `FacturaService` para usar configuración
- Auto-incrementar número de factura al crear una nueva
- Aplicar IGV según configuración
- Mostrar términos y condiciones en PDF de factura

---

## 📦 FASE 3: GESTIÓN DE USUARIOS

### Objetivo
Permitir a los administradores gestionar usuarios del sistema.

### Tareas

#### 3.1 Controller y Vistas
**Archivo:** `UsuarioController.java` (extender existente)

**Endpoints nuevos:**
```java
GET  /usuarios                     → Lista de usuarios
GET  /usuarios/form                → Formulario crear usuario
GET  /usuarios/form/{id}           → Formulario editar usuario
POST /usuarios/save                → Guardar usuario
POST /usuarios/delete/{id}         → Eliminar usuario
POST /usuarios/toggle-active/{id}  → Activar/desactivar
POST /usuarios/reset-password/{id} → Resetear contraseña
```

#### 3.2 Vista Principal
**Archivo:** `usuarios/usuarios.html`

**Características:**
- Tabla con todos los usuarios
- Columnas: ID, Nombre, Teléfono, Email, Rol, Estado, Acciones
- Filtros: por rol, por estado (activo/inactivo)
- Búsqueda por nombre o teléfono
- Paginación
- Botones: Nuevo Usuario, Editar, Eliminar, Activar/Desactivar

#### 3.3 Vista Formulario
**Archivo:** `usuarios/form.html`

**Campos:**
- Nombre (required)
- Teléfono (required, unique)
- Email (optional, unique)
- Contraseña (required en creación)
- Confirmar contraseña
- Rol (select: ADMIN, USER, VENDEDOR)
- Estado activo (checkbox)

#### 3.4 Funcionalidades
- ✅ CRUD completo de usuarios
- ✅ Validación de teléfono único
- ✅ Validación de email único
- ✅ Generar contraseña aleatoria
- ✅ Enviar contraseña por email/WhatsApp
- ✅ Activar/desactivar usuarios (soft delete)
- ✅ Resetear contraseña
- ✅ Ver última actividad del usuario
- ✅ Restricción: solo ADMIN puede gestionar usuarios

---

## 📦 FASE 4: ROLES Y PERMISOS

### Objetivo
Implementar sistema de permisos basado en roles.

### Tareas

#### 4.1 Definición de Roles

**Roles disponibles:**
1. **ADMIN** - Acceso total
2. **USER** - Acceso a módulos operativos
3. **VENDEDOR** - Solo crear facturas y ver clientes
4. **VISUALIZADOR** - Solo lectura

#### 4.2 Tabla de Permisos

| Módulo | ADMIN | USER | VENDEDOR | VISUALIZADOR |
|--------|-------|------|----------|--------------|
| Dashboard | ✅ | ✅ | ✅ | ✅ |
| Clientes (Ver) | ✅ | ✅ | ✅ | ✅ |
| Clientes (Crear/Editar) | ✅ | ✅ | ❌ | ❌ |
| Productos (Ver) | ✅ | ✅ | ✅ | ✅ |
| Productos (Crear/Editar) | ✅ | ✅ | ❌ | ❌ |
| Facturas (Ver) | ✅ | ✅ | ✅ | ✅ |
| Facturas (Crear) | ✅ | ✅ | ✅ | ❌ |
| Facturas (Editar/Eliminar) | ✅ | ✅ | ❌ | ❌ |
| Reportes | ✅ | ✅ | ❌ | ❌ |
| Configuración | ✅ | ❌ | ❌ | ❌ |
| Usuarios | ✅ | ❌ | ❌ | ❌ |

#### 4.3 Implementación
**Archivo:** `SecurityConfig.java` (actualizar)

```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http
        .authorizeRequests()
            // Públicas
            .antMatchers("/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
            
            // Dashboard y perfil (todos autenticados)
            .antMatchers("/dashboard", "/perfil/**").authenticated()
            
            // Módulos operativos (ADMIN, USER, VENDEDOR)
            .antMatchers("/clientes", "/productos", "/facturas").hasAnyRole("ADMIN", "USER", "VENDEDOR")
            
            // Crear/Editar (ADMIN, USER)
            .antMatchers("/clientes/save", "/productos/save").hasAnyRole("ADMIN", "USER")
            .antMatchers("/facturas/save").hasAnyRole("ADMIN", "USER", "VENDEDOR")
            
            // Eliminar (solo ADMIN, USER)
            .antMatchers("/*/delete/**").hasAnyRole("ADMIN", "USER")
            
            // Reportes (ADMIN, USER)
            .antMatchers("/reportes/**").hasAnyRole("ADMIN", "USER")
            
            // Configuración y Usuarios (solo ADMIN)
            .antMatchers("/configuracion/**", "/usuarios/**").hasRole("ADMIN")
            
            .anyRequest().authenticated()
        .and()
        .formLogin()
            .loginPage("/auth/login")
            .defaultSuccessUrl("/dashboard")
        .and()
        .logout()
            .logoutUrl("/auth/logout")
            .logoutSuccessUrl("/auth/login");
}
```

#### 4.4 Directivas Thymeleaf
```html
<!-- Mostrar solo para ADMIN -->
<div sec:authorize="hasRole('ADMIN')">
    <a href="/configuracion">Configuración</a>
</div>

<!-- Mostrar para ADMIN o USER -->
<button sec:authorize="hasAnyRole('ADMIN', 'USER')">Editar</button>

<!-- Ocultar para VISUALIZADOR -->
<form sec:authorize="!hasRole('VISUALIZADOR')">
    <!-- Formulario de edición -->
</form>
```

---

## 📦 FASE 5: NOTIFICACIONES BÁSICAS

### Objetivo
Implementar sistema de notificaciones por email.

### Tareas

#### 5.1 Configuración de Email
**Archivo:** `application.yml`

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 5.2 Servicio de Email
**Archivo:** `EmailService.java`

```java
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void enviarFacturaPorEmail(Factura factura, String emailDestino) {
        // Implementación
    }
    
    public void enviarCredencialesUsuario(Usuario usuario, String password) {
        // Implementación
    }
    
    public void enviarRecordatorioPago(Factura factura) {
        // Implementación
    }
}
```

#### 5.3 Tipos de Notificaciones
1. **Nueva factura creada** → Enviar PDF por email al cliente
2. **Usuario creado** → Enviar credenciales por email
3. **Recordatorio de pago** → Email automático para facturas pendientes
4. **Cambio de contraseña** → Notificar al usuario

#### 5.4 Configuración de Notificaciones
**Vista:** `configuracion/notificaciones.html`

**Opciones:**
- ☑️ Activar notificaciones por email
- ☑️ Enviar factura automáticamente por email al crearla
- ☑️ Enviar recordatorio de pago (cada X días)
- ☑️ Notificar cuando se registra un nuevo cliente
- ☑️ Notificar cuando un usuario cambia su contraseña

---

## 📦 FASE 6: REPORTES Y ESTADÍSTICAS

### Objetivo
Generar reportes básicos de ventas y estadísticas.

### Tareas

#### 6.1 Controller y Vistas
**Archivo:** `ReporteController.java`

**Endpoints:**
```java
GET /reportes                      → Página principal
GET /reportes/ventas               → Reporte de ventas
GET /reportes/clientes             → Reporte de clientes
GET /reportes/productos            → Reporte de productos
GET /reportes/export/pdf           → Exportar a PDF
GET /reportes/export/excel         → Exportar a Excel
```

#### 6.2 Tipos de Reportes

**1. Reporte de Ventas**
- Total de ventas por período (día, semana, mes, año)
- Facturas pagadas vs pendientes
- Gráfico de ventas en el tiempo
- Top 10 clientes
- Métodos de pago más usados

**2. Reporte de Clientes**
- Total de clientes registrados
- Clientes nuevos por período
- Clientes con deuda pendiente
- Clientes más frecuentes

**3. Reporte de Productos**
- Productos más vendidos
- Stock bajo (alertas)
- Productos sin ventas
- Valor total del inventario

#### 6.3 Filtros
- Rango de fechas (desde - hasta)
- Cliente específico
- Producto específico
- Estado de factura (pagado, pendiente)
- Método de pago

#### 6.4 Exportación
- **PDF**: Usando iText o similar
- **Excel**: Usando Apache POI
- **CSV**: Export simple

---

## 📦 FASE 7: INTEGRACIÓN DE MÓDULOS

### Objetivo
Mejorar la integración entre módulos existentes.

### Tareas

#### 7.1 Breadcrumbs en todas las vistas
- Implementar navegación de migas de pan
- Formato: Dashboard > Módulo > Acción
- Actualizar dinámicamente con JavaScript

#### 7.2 Links desde Dashboard
- Asegurar que todos los módulos activos naveguen correctamente
- Verificar que módulos inactivos muestren alerta

#### 7.3 Navbar con Avatar
- Mostrar avatar del usuario si existe
- Mostrar iniciales si no hay avatar
- Sincronizar con datos del perfil

#### 7.4 Actualización de último acceso
- Actualizar campo `ultimo_acceso` en login exitoso
- Modificar `AuthController` o `UserDetailsService`

#### 7.5 Unificación de Diseño
- Verificar que todas las vistas usen el mismo layout
- Estandarizar botones y formularios
- Estandarizar tablas y tarjetas

---

## 📦 FASE 8: TESTING Y OPTIMIZACIÓN

### Objetivo
Probar todas las funcionalidades y optimizar rendimiento.

### Tareas

#### 8.1 Testing Funcional
- [ ] Login y autenticación
- [ ] Gestión de usuarios (CRUD)
- [ ] Configuración de empresa
- [ ] Configuración de facturación
- [ ] Roles y permisos
- [ ] Notificaciones por email
- [ ] Generación de reportes
- [ ] Exportación de datos

#### 8.2 Testing de Seguridad
- [ ] Verificar que roles restringen acceso correctamente
- [ ] Verificar CSRF tokens en formularios
- [ ] Verificar que solo ADMIN accede a configuración
- [ ] Verificar que usuarios inactivos no pueden login

#### 8.3 Testing de Integración
- [ ] Crear factura con configuración personalizada
- [ ] Enviar factura por email
- [ ] Generar reporte de ventas
- [ ] Exportar datos a PDF/Excel

#### 8.4 Optimización
- [ ] Indexar tablas de base de datos
- [ ] Optimizar consultas N+1
- [ ] Implementar paginación en listas grandes
- [ ] Cachear datos de configuración
- [ ] Minificar CSS y JS

---

## 📋 Checklist General Sprint 2

### Configuración
- [ ] Modelo Empresa
- [ ] CRUD Empresa
- [ ] Upload de logo y favicon
- [ ] Modelo ConfiguracionFacturacion
- [ ] Configurar serie y numeración
- [ ] Configurar IGV/IVA

### Usuarios
- [ ] Lista de usuarios
- [ ] Crear usuario
- [ ] Editar usuario
- [ ] Eliminar usuario
- [ ] Activar/desactivar usuario
- [ ] Resetear contraseña

### Roles y Permisos
- [ ] Definir roles
- [ ] Configurar SecurityConfig
- [ ] Aplicar permisos en controladores
- [ ] Aplicar permisos en vistas (sec:authorize)

### Notificaciones
- [ ] Configurar JavaMailSender
- [ ] Servicio de email
- [ ] Enviar factura por email
- [ ] Enviar credenciales de usuario
- [ ] Recordatorio de pago

### Reportes
- [ ] Reporte de ventas
- [ ] Reporte de clientes
- [ ] Reporte de productos
- [ ] Exportar a PDF
- [ ] Exportar a Excel

### Integración
- [ ] Breadcrumbs en todas las vistas
- [ ] Links activos desde dashboard
- [ ] Avatar en navbar
- [ ] Actualizar último acceso
- [ ] Diseño unificado

### Testing
- [ ] Testing funcional completo
- [ ] Testing de seguridad
- [ ] Testing de integración
- [ ] Optimización de rendimiento

---

## 📊 Criterios de Aceptación

### Para Configuración
- ✅ Se puede editar y guardar datos de la empresa
- ✅ El logo se muestra en el navbar y en facturas
- ✅ La serie de factura se aplica automáticamente
- ✅ El IGV se calcula según configuración

### Para Usuarios
- ✅ Solo ADMIN puede gestionar usuarios
- ✅ Los usuarios inactivos no pueden hacer login
- ✅ Se envían credenciales por email al crear usuario

### Para Roles
- ✅ Cada rol tiene acceso solo a sus módulos permitidos
- ✅ Las opciones de editar/eliminar se ocultan para roles sin permiso
- ✅ Se muestra mensaje de "Acceso denegado" si se intenta acceso no autorizado

### Para Reportes
- ✅ Los reportes muestran datos correctos
- ✅ Los filtros funcionan correctamente
- ✅ La exportación genera archivos válidos

---

## 🚀 Orden de Implementación Recomendado

1. **Semana 1:**
   - Fase 1: Configuración de Empresa (3 días)
   - Fase 2: Configuración de Facturación (2 días)

2. **Semana 2:**
   - Fase 3: Gestión de Usuarios (4 días)
   - Inicio Fase 4: Roles y Permisos (1 día)

3. **Semana 3:**
   - Continuar Fase 4: Roles y Permisos (2 días)
   - Fase 5: Notificaciones Básicas (3 días)

4. **Semana 4:**
   - Fase 6: Reportes y Estadísticas (4 días)

5. **Semana 5:**
   - Fase 7: Integración de Módulos (2 días)
   - Fase 8: Testing y Optimización (3 días)

---

## 📝 Notas Importantes

1. **Priorizar Fase 1, 2 y 3** - Son fundamentales para un sistema de facturación profesional
2. **Testing continuo** - No dejar testing solo para el final
3. **Documentar cambios** - Actualizar documentación con cada fase completada
4. **Commits frecuentes** - Hacer commits por cada funcionalidad completada
5. **Revisión de código** - Revisar código antes de pasar a siguiente fase

---

**Fecha de creación:** 12 de octubre de 2025  
**Última actualización:** 12 de octubre de 2025  
**Estado:** Planificación completada ✅
