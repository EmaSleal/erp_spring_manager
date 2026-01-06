# 🔄 GUÍA DE REFACTORIZACIÓN MODULAR

**Proyecto:** WhatsApp Orders Manager  
**Tipo:** Reorganización de Package by Layer a Package by Feature  
**Fecha:** 27 de diciembre de 2025  
**Estimado:** 3-5 días (trabajo gradual)  
**Estado:** 📋 PLANIFICADO

---

## 📋 ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Estructura Actual vs Propuesta](#estructura-actual-vs-propuesta)
3. [Preparación Previa](#preparación-previa)
4. [Fases de Migración](#fases-de-migración)
5. [Checklist por Módulo](#checklist-por-módulo)
6. [Resolución de Problemas](#resolución-de-problemas)
7. [Validación Final](#validación-final)

---

## 🎯 RESUMEN EJECUTIVO

### Objetivo
Reorganizar el código de **Package by Layer** (organización técnica) a **Package by Feature** (organización por módulos de negocio) para mejorar mantenibilidad, escalabilidad y cohesión del código.

### Beneficios Esperados
- ✅ **Cohesión alta:** Todo lo relacionado con un módulo está junto
- ✅ **Navegación fácil:** Encontrar código es más intuitivo
- ✅ **Escalabilidad:** Cada módulo crece independientemente
- ✅ **Testing:** Tests organizados por módulo
- ✅ **Trabajo en equipo:** Menor cantidad de conflictos en Git

### Duración Estimada
- **Total:** 3-5 días (20-30 horas)
- **Por módulo:** 2-4 horas (dependiendo del tamaño)

---

## 🔍 ESTRUCTURA ACTUAL VS PROPUESTA

### 📦 Estructura Actual (Package by Layer)

```
src/main/java/api/astro/whats_orders_manager/
├── controllers/              (30 archivos mezclados)
│   ├── FacturaController.java
│   ├── ClienteController.java
│   ├── WhatsAppViewController.java
│   ├── NotificacionRestController.java
│   └── ...
├── services/                 (28 archivos mezclados)
│   ├── FacturaService.java
│   ├── ClienteService.java
│   ├── WhatsAppService.java
│   └── ...
├── repositories/
├── models/                   (25+ archivos mezclados)
│   ├── Factura.java
│   ├── Cliente.java
│   ├── MensajeWhatsApp.java
│   └── ...
├── config/
├── util/
└── WhatsOrdersManagerApplication.java
```

**❌ Problemas:**
- Difícil encontrar todo lo relacionado con WhatsApp
- 30 controllers en una sola carpeta
- Cambios en facturación requieren navegar 4+ carpetas
- Difícil de escalar

---

### ✅ Estructura Propuesta (Package by Feature)

```
src/main/java/api/astro/whats_orders_manager/
│
├── WhatsOrdersManagerApplication.java
│
├── shared/                          # Código compartido entre módulos
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebConfig.java
│   │   ├── ThymeleafConfig.java
│   │   └── DatabaseConfig.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ResourceNotFoundException.java
│   │   └── BusinessException.java
│   ├── util/
│   │   ├── DateUtil.java
│   │   ├── FileUtil.java
│   │   └── ValidationUtil.java
│   └── dto/
│       └── ApiResponse.java
│
├── modules/                         # ⭐ MÓDULOS DE NEGOCIO
│   │
│   ├── facturacion/
│   │   ├── controller/
│   │   │   ├── FacturaController.java
│   │   │   └── LineaFacturaController.java
│   │   ├── service/
│   │   │   ├── FacturaService.java
│   │   │   └── LineaFacturaService.java
│   │   ├── repository/
│   │   │   ├── FacturaRepository.java
│   │   │   └── LineaFacturaRepository.java
│   │   ├── model/
│   │   │   ├── Factura.java
│   │   │   └── LineaFactura.java
│   │   ├── dto/
│   │   │   ├── FacturaDTO.java
│   │   │   └── LineaFacturaDTO.java
│   │   └── enums/
│   │       └── EstadoFactura.java
│   │
│   ├── cliente/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   └── dto/
│   │
│   ├── producto/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   └── dto/
│   │
│   ├── whatsapp/
│   │   ├── controller/
│   │   │   ├── WhatsAppViewController.java
│   │   │   ├── WhatsAppMensajeController.java
│   │   │   ├── WhatsAppPlantillaController.java
│   │   │   ├── WhatsAppFacturaController.java
│   │   │   └── WhatsAppWebhookController.java
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   └── enums/
│   │
│   ├── notificacion/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   ├── enums/
│   │   └── events/
│   │
│   ├── seguridad/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   └── enums/
│   │
│   ├── configuracion/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   └── dto/
│   │
│   └── reportes/
│       ├── controller/
│       ├── service/
│       └── dto/
│
└── core/                            # Infraestructura técnica
    ├── listeners/
    │   └── ApplicationStartupListener.java
    ├── schedulers/
    │   └── CleanupScheduler.java
    └── events/
        └── BaseEvent.java
```

---

## 🛠️ PREPARACIÓN PREVIA

### 1. Backup del Proyecto ⚠️ CRÍTICO

```bash
# Opción A: Crear branch de backup
git checkout -b backup/pre-refactoring
git add .
git commit -m "Backup antes de refactorización modular"
git push origin backup/pre-refactoring

# Opción B: Copiar carpeta completa
cd D:\programacion\java\spring-boot\
cp -r whats_orders_manager whats_orders_manager_backup_20251227
```

### 2. Asegurar que Todo Compila

```bash
# Limpiar y compilar
mvn clean compile

# Ejecutar todos los tests
mvn test

# Verificar que la aplicación arranca
mvn spring-boot:run
```

**✅ CRITERIO DE ÉXITO:** 
- Compilación exitosa
- Todos los tests pasan
- Aplicación arranca sin errores

### 3. Crear Branch de Trabajo

```bash
git checkout -b feature/modular-refactoring
```

### 4. Documentar Estado Actual

```bash
# Contar archivos actuales
echo "Controllers:" && ls -1 src/main/java/api/astro/whats_orders_manager/controllers/ | wc -l
echo "Services:" && ls -1 src/main/java/api/astro/whats_orders_manager/services/ | wc -l
echo "Models:" && ls -1 src/main/java/api/astro/whats_orders_manager/models/ | wc -l
```

---

## 🚀 FASES DE MIGRACIÓN

### 📊 ORDEN DE MIGRACIÓN (Del más simple al más complejo)

```
FASE 1: Estructura base           (30 min)
FASE 2: Módulo Producto           (2 horas)   ← Más simple
FASE 3: Módulo Cliente            (2 horas)
FASE 4: Módulo Reportes           (2 horas)
FASE 5: Módulo Configuración      (3 horas)
FASE 6: Módulo Facturación        (4 horas)
FASE 7: Módulo WhatsApp           (4 horas)
FASE 8: Módulo Notificación       (4 horas)
FASE 9: Módulo Seguridad          (5 horas)   ← Más complejo
FASE 10: Código Compartido (shared) (2 horas)
FASE 11: Core (listeners/schedulers) (1 hora)
FASE 12: Limpieza Final           (1 hora)
```

---

## 📝 FASE 1: CREAR ESTRUCTURA BASE

**Duración:** 30 minutos  
**Prioridad:** ⚡ CRÍTICA (Desbloquea todo)

### Paso 1.1: Crear Carpetas Principales

```bash
cd src/main/java/api/astro/whats_orders_manager/

# Crear carpetas principales
mkdir -p modules/producto/{controller,service,repository,model,dto}
mkdir -p modules/cliente/{controller,service,repository,model,dto}
mkdir -p modules/facturacion/{controller,service,repository,model,dto,enums}
mkdir -p modules/whatsapp/{controller,service,repository,model,dto,enums}
mkdir -p modules/notificacion/{controller,service,repository,model,dto,enums,events}
mkdir -p modules/seguridad/{controller,service,repository,model,dto,enums}
mkdir -p modules/configuracion/{controller,service,repository,model,dto}
mkdir -p modules/reportes/{controller,service,dto}
mkdir -p shared/{config,exception,util,dto}
mkdir -p core/{listeners,schedulers,events}
```

### Paso 1.2: Verificar Estructura

```bash
# Verificar que las carpetas se crearon
tree modules/ -L 2
tree shared/ -L 2
tree core/ -L 2
```

### Paso 1.3: Crear .gitkeep en Carpetas Vacías

```bash
# Para que Git trackee las carpetas vacías
find modules/ -type d -empty -exec touch {}/.gitkeep \;
find shared/ -type d -empty -exec touch {}/.gitkeep \;
find core/ -type d -empty -exec touch {}/.gitkeep \;
```

### Paso 1.4: Commit Estructura

```bash
git add .
git commit -m "feat: Crear estructura base para refactorización modular"
```

---

## 📦 FASE 2: MIGRAR MÓDULO PRODUCTO (EJEMPLO COMPLETO)

**Duración:** 2 horas  
**Complejidad:** ⭐ Baja (Empezamos con el más simple)

### Paso 2.1: Identificar Archivos a Migrar

**Lista de archivos:**
```
controllers/ProductoController.java
services/ProductoService.java
repositories/ProductoRepository.java
models/Producto.java
models/dto/ProductoDTO.java
```

### Paso 2.2: Mover ProductoController.java

#### A. Mover el archivo

```bash
# Desde la raíz del proyecto
cd src/main/java/api/astro/whats_orders_manager/

# Mover el controller
mv controllers/ProductoController.java modules/producto/controller/
```

#### B. Actualizar imports en ProductoController.java

**ANTES:**
```java
package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.Producto;
import api.astro.whats_orders_manager.models.dto.ProductoDTO;
import api.astro.whats_orders_manager.services.ProductoService;
```

**DESPUÉS:**
```java
package api.astro.whats_orders_manager.modules.producto.controller;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.dto.ProductoDTO;
import api.astro.whats_orders_manager.modules.producto.service.ProductoService;
```

#### C. Usar "Find and Replace" en IntelliJ

1. `Ctrl + Shift + R` (Replace in Files)
2. Buscar: `import api.astro.whats_orders_manager.controllers.ProductoController`
3. Reemplazar: `import api.astro.whats_orders_manager.modules.producto.controller.ProductoController`
4. Scope: `Whole Project`
5. Click "Replace All"

### Paso 2.3: Mover ProductoService.java

```bash
mv services/ProductoService.java modules/producto/service/
```

**Actualizar package:**
```java
package api.astro.whats_orders_manager.modules.producto.service;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
```

### Paso 2.4: Mover ProductoRepository.java

```bash
mv repositories/ProductoRepository.java modules/producto/repository/
```

**Actualizar package:**
```java
package api.astro.whats_orders_manager.modules.producto.repository;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
```

### Paso 2.5: Mover Producto.java (Modelo)

```bash
mv models/Producto.java modules/producto/model/
```

**Actualizar package:**
```java
package api.astro.whats_orders_manager.modules.producto.model;
```

### Paso 2.6: Mover ProductoDTO.java

```bash
mv models/dto/ProductoDTO.java modules/producto/dto/
```

**Actualizar package:**
```java
package api.astro.whats_orders_manager.modules.producto.dto;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
```

### Paso 2.7: Actualizar TODOS los imports en el proyecto

**Usar IntelliJ IDEA:**

1. `Ctrl + Shift + F` (Find in Files)
2. Buscar: `import api.astro.whats_orders_manager.models.Producto;`
3. Ver todos los archivos que lo usan
4. Para cada uno, usar `Alt + Enter` → "Optimize imports"

**O usar Replace All:**

| Buscar | Reemplazar con |
|--------|----------------|
| `import api.astro.whats_orders_manager.models.Producto;` | `import api.astro.whats_orders_manager.modules.producto.model.Producto;` |
| `import api.astro.whats_orders_manager.services.ProductoService;` | `import api.astro.whats_orders_manager.modules.producto.service.ProductoService;` |
| `import api.astro.whats_orders_manager.repositories.ProductoRepository;` | `import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;` |
| `import api.astro.whats_orders_manager.models.dto.ProductoDTO;` | `import api.astro.whats_orders_manager.modules.producto.dto.ProductoDTO;` |

### Paso 2.8: Compilar y Verificar

```bash
# Limpiar y compilar
mvn clean compile
```

**✅ CRITERIO DE ÉXITO:**
- Compilación exitosa sin errores
- Todos los imports resueltos

### Paso 2.9: Ejecutar Tests

```bash
# Tests específicos de Producto (si existen)
mvn test -Dtest=ProductoTest

# Todos los tests
mvn test
```

### Paso 2.10: Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

**Verificar:**
- ✅ Aplicación arranca sin errores
- ✅ Endpoint de productos funciona: `http://localhost:8080/productos`
- ✅ No hay errores en logs

### Paso 2.11: Commit

```bash
git add .
git commit -m "refactor: Migrar módulo Producto a estructura modular

- Movido ProductoController a modules/producto/controller/
- Movido ProductoService a modules/producto/service/
- Movido ProductoRepository a modules/producto/repository/
- Movido Producto (modelo) a modules/producto/model/
- Movido ProductoDTO a modules/producto/dto/
- Actualizados todos los imports en el proyecto
- Tests pasan: ✅
- Compilación exitosa: ✅"
```

---

## 📦 FASE 3: MIGRAR MÓDULO CLIENTE

**Duración:** 2 horas  
**Complejidad:** ⭐ Baja

### Archivos a Migrar

```
controllers/ClienteController.java → modules/cliente/controller/
services/ClienteService.java → modules/cliente/service/
repositories/ClienteRepository.java → modules/cliente/repository/
models/Cliente.java → modules/cliente/model/
models/dto/ClienteDTO.java → modules/cliente/dto/
```

### Pasos (Seguir mismo proceso que Producto)

1. ✅ Mover ClienteController.java
2. ✅ Actualizar package a `api.astro.whats_orders_manager.modules.cliente.controller`
3. ✅ Mover ClienteService.java
4. ✅ Actualizar package a `api.astro.whats_orders_manager.modules.cliente.service`
5. ✅ Mover ClienteRepository.java
6. ✅ Mover Cliente.java (modelo)
7. ✅ Mover ClienteDTO.java
8. ✅ Actualizar todos los imports (Find & Replace All)
9. ✅ Compilar: `mvn clean compile`
10. ✅ Tests: `mvn test`
11. ✅ Run: `mvn spring-boot:run`
12. ✅ Commit: "refactor: Migrar módulo Cliente a estructura modular"

---

## 📦 FASE 4: MIGRAR MÓDULO REPORTES

**Duración:** 2 horas  
**Complejidad:** ⭐⭐ Media

### Archivos a Migrar

```
controllers/ReporteController.java → modules/reportes/controller/
controllers/DashboardController.java → modules/reportes/controller/
services/ReporteService.java → modules/reportes/service/
services/ExportService.java → modules/reportes/service/
models/dto/ReporteDTO.java → modules/reportes/dto/ (si existe)
```

### Pasos

1. ✅ Mover ambos controllers
2. ✅ Mover ambos services
3. ✅ Mover DTOs relacionados
4. ✅ Actualizar packages
5. ✅ Actualizar imports
6. ✅ Compilar y testear
7. ✅ Commit

---

## 📦 FASE 5: MIGRAR MÓDULO CONFIGURACIÓN

**Duración:** 3 horas  
**Complejidad:** ⭐⭐ Media

### Archivos a Migrar

```
Controllers (5):
├── ConfiguracionController.java
├── ConfiguracionEmailRestController.java
├── ConfiguracionEmpresaRestController.java
├── ConfiguracionFacturacionRestController.java
└── ParametroSistemaRestController.java

Services (5):
├── ConfiguracionEmailService.java
├── ConfiguracionEmpresaService.java
├── ConfiguracionFacturacionService.java
├── EmpresaService.java
└── ParametroSistemaService.java

Repositories (5):
├── ConfiguracionEmailRepository.java
├── ConfiguracionEmpresaRepository.java
├── ConfiguracionFacturacionRepository.java
├── EmpresaRepository.java
└── ParametroSistemaRepository.java

Models (5):
├── ConfiguracionEmail.java
├── ConfiguracionEmpresa.java
├── ConfiguracionFacturacion.java
├── Empresa.java
└── ParametroSistema.java

DTOs:
└── (Si existen, moverlos a modules/configuracion/dto/)
```

### Script de Ayuda (PowerShell)

```powershell
# Cambiar al directorio del proyecto
cd "D:\programacion\java\spring-boot\whats_orders_manager\src\main\java\api\astro\whats_orders_manager"

# Mover Controllers
Move-Item "controllers/ConfiguracionController.java" "modules/configuracion/controller/"
Move-Item "controllers/ConfiguracionEmailRestController.java" "modules/configuracion/controller/"
Move-Item "controllers/ConfiguracionEmpresaRestController.java" "modules/configuracion/controller/"
Move-Item "controllers/ConfiguracionFacturacionRestController.java" "modules/configuracion/controller/"
Move-Item "controllers/ParametroSistemaRestController.java" "modules/configuracion/controller/"

# Mover Services
Move-Item "services/ConfiguracionEmailService.java" "modules/configuracion/service/"
Move-Item "services/ConfiguracionEmpresaService.java" "modules/configuracion/service/"
Move-Item "services/ConfiguracionFacturacionService.java" "modules/configuracion/service/"
Move-Item "services/EmpresaService.java" "modules/configuracion/service/"
Move-Item "services/ParametroSistemaService.java" "modules/configuracion/service/"

# Continuar con repositories y models...
```

### Pasos

1. ✅ Mover todos los controllers (5 archivos)
2. ✅ Mover todos los services (5 archivos)
3. ✅ Mover todos los repositories (5 archivos)
4. ✅ Mover todos los models (5 archivos)
5. ✅ Mover DTOs relacionados
6. ✅ Actualizar packages en todos los archivos
7. ✅ Actualizar imports con Find & Replace
8. ✅ Compilar: `mvn clean compile`
9. ✅ Tests: `mvn test`
10. ✅ Commit

---

## 📦 FASE 6: MIGRAR MÓDULO FACTURACIÓN

**Duración:** 4 horas  
**Complejidad:** ⭐⭐⭐ Alta

### Archivos a Migrar

```
Controllers (2):
├── FacturaController.java
└── LineaFacturaController.java

Services (2):
├── FacturaService.java
└── LineaFacturaService.java

Repositories (2):
├── FacturaRepository.java
└── LineaFacturaRepository.java

Models (2):
├── Factura.java
└── LineaFactura.java

DTOs:
├── FacturaDTO.java
└── LineaFacturaDTO.java

Enums:
└── EstadoFactura.java (si existe en enums/)
```

### Consideraciones Especiales

⚠️ **IMPORTANTE:** Facturación es módulo CORE, muchos otros módulos lo usan:
- WhatsApp (enviar facturas)
- Reportes (estadísticas de facturas)
- Cliente (historial de facturas)
- Notificaciones (notificar sobre facturas)

### Estrategia de Migración

1. ✅ **Primero:** Mover models (Factura, LineaFactura)
2. ✅ **Segundo:** Actualizar TODOS los imports de models
3. ✅ **Tercero:** Mover repositories
4. ✅ **Cuarto:** Mover services
5. ✅ **Quinto:** Mover controllers
6. ✅ **Sexto:** Compilar después de cada paso
7. ✅ **Séptimo:** Test completo al final

### Pasos Detallados

```bash
# 1. Mover Models
mv models/Factura.java modules/facturacion/model/
mv models/LineaFactura.java modules/facturacion/model/

# 2. Actualizar imports de Factura en TODO el proyecto
# IntelliJ: Ctrl + Shift + R
# Buscar: import api.astro.whats_orders_manager.models.Factura;
# Reemplazar: import api.astro.whats_orders_manager.modules.facturacion.model.Factura;

# 3. Compilar para verificar
mvn clean compile

# 4. Continuar con repositories, services, controllers...
```

### Testing Extra

```bash
# Tests de facturación
mvn test -Dtest=FacturaTest

# Tests de integración que usan facturación
mvn test -Dtest=*FacturaIntegrationTest

# Test completo
mvn test
```

---

## 📦 FASE 7: MIGRAR MÓDULO WHATSAPP

**Duración:** 4 horas  
**Complejidad:** ⭐⭐⭐ Alta

### Archivos a Migrar

```
Controllers (5):
├── WhatsAppViewController.java
├── WhatsAppMensajeController.java
├── WhatsAppPlantillaController.java
├── WhatsAppFacturaController.java
└── WhatsAppWebhookController.java

Services (5):
├── WhatsAppService.java
├── MensajeWhatsAppService.java
├── PlantillaWhatsAppService.java
├── WhatsAppFacturaService.java
└── WebhookWhatsAppService.java

Repositories (3):
├── MensajeWhatsAppRepository.java
├── PlantillaWhatsAppRepository.java
└── WebhookLogRepository.java

Models (3):
├── MensajeWhatsApp.java
├── PlantillaWhatsApp.java
└── WebhookLog.java

DTOs:
├── MensajeWhatsAppDTO.java
└── PlantillaWhatsAppDTO.java

Enums:
├── DireccionMensaje.java (si existe)
└── EstadoMensaje.java (si existe)
```

### Consideraciones Especiales

⚠️ **DEPENDENCIAS:**
- Usa `Factura` del módulo facturación
- Usa `Usuario` del módulo seguridad
- Usa `NotificacionService` del módulo notificación

### Script PowerShell de Ayuda

```powershell
cd "D:\programacion\java\spring-boot\whats_orders_manager\src\main\java\api\astro\whats_orders_manager"

# Controllers
$controllers = @(
    "WhatsAppViewController",
    "WhatsAppMensajeController",
    "WhatsAppPlantillaController",
    "WhatsAppFacturaController",
    "WhatsAppWebhookController"
)

foreach ($controller in $controllers) {
    Move-Item "controllers/$controller.java" "modules/whatsapp/controller/"
}

# Services
$services = @(
    "WhatsAppService",
    "MensajeWhatsAppService",
    "PlantillaWhatsAppService",
    "WhatsAppFacturaService",
    "WebhookWhatsAppService"
)

foreach ($service in $services) {
    Move-Item "services/$service.java" "modules/whatsapp/service/"
}

# Continuar con repositories, models, etc.
```

### Orden de Migración

1. ✅ Models (3 archivos)
2. ✅ Enums (si existen)
3. ✅ DTOs (2 archivos)
4. ✅ Repositories (3 archivos)
5. ✅ Services (5 archivos)
6. ✅ Controllers (5 archivos)
7. ✅ Actualizar packages
8. ✅ Actualizar imports
9. ✅ Compilar después de cada grupo
10. ✅ Test final

---

## 📦 FASE 8: MIGRAR MÓDULO NOTIFICACIÓN

**Duración:** 4 horas  
**Complejidad:** ⭐⭐⭐ Alta

### Archivos a Migrar

```
Controllers (3):
├── NotificacionRestController.java
├── NotificacionViewController.java
└── NotificacionWebSocketController.java

Services (4):
├── NotificacionService.java
├── PlantillaNotificacionService.java
├── PreferenciaNotificacionService.java
└── ConfiguracionNotificacionesService.java

Repositories (4):
├── NotificacionRepository.java
├── PlantillaNotificacionRepository.java
├── PreferenciaNotificacionRepository.java
└── ConfiguracionNotificacionesRepository.java

Models (4):
├── Notificacion.java
├── PlantillaNotificacion.java
├── PreferenciaNotificacion.java
└── ConfiguracionNotificaciones.java

DTOs:
├── NotificacionDTO.java
└── (otros DTOs relacionados)

Enums:
├── CanalNotificacion.java
├── TipoNotificacion.java
└── EstadoNotificacion.java

Events (si existen):
├── NotificacionEvent.java
└── NotificacionEventListener.java
```

### Consideraciones Especiales

⚠️ **EVENTOS:**
- Si usas eventos de Spring, moverlos a `modules/notificacion/events/`
- Listener puede quedarse en `core/listeners/` si es global

### Pasos

1. ✅ Mover events primero (si existen)
2. ✅ Mover enums
3. ✅ Mover models
4. ✅ Mover DTOs
5. ✅ Mover repositories
6. ✅ Mover services
7. ✅ Mover controllers
8. ✅ Actualizar packages y imports
9. ✅ Compilar y testear
10. ✅ Commit

---

## 📦 FASE 9: MIGRAR MÓDULO SEGURIDAD

**Duración:** 5 horas  
**Complejidad:** ⭐⭐⭐⭐ Muy Alta

### Archivos a Migrar

```
Controllers (8):
├── AuthController.java
├── UsuarioController.java
├── UsuarioAdminController.java
├── PermisosController.java
├── PermisoAdminController.java
├── RolAdminController.java
├── PerfilController.java
└── (otros relacionados)

Services (5):
├── UsuarioService.java
├── PermisoService.java
├── RolService.java
├── UsuarioPermisoService.java
└── UsuarioActividadService.java

Repositories (5):
├── UsuarioRepository.java
├── PermisoRepository.java
├── RolRepository.java
├── UsuarioPermisoRepository.java
└── UsuarioActividadRepository.java

Models (6):
├── Usuario.java
├── Permiso.java
├── Rol.java
├── UsuarioPermiso.java
├── UsuarioActividad.java
└── UsuarioSesion.java

DTOs:
├── UsuarioDTO.java
├── PermisoDTO.java
└── (otros)

Enums:
└── TipoPermiso.java (si existe)
```

### ⚠️ IMPORTANTE: Este módulo es CRÍTICO

**Seguridad es usado por TODO el sistema:**
- Autenticación y autorización
- Control de acceso
- Sesiones de usuario
- Auditoría

### Estrategia Conservadora

1. ✅ **Mover en pequeños grupos** (no todo de golpe)
2. ✅ **Compilar después de cada grupo**
3. ✅ **No mover SecurityConfig** (queda en `shared/config/`)
4. ✅ **Hacer backup antes** (`git stash` o commit temporal)

### Orden Seguro

```
Grupo 1: Models básicos
├── Usuario.java
├── Rol.java
└── Permiso.java
→ Compilar y verificar

Grupo 2: Models de relación
├── UsuarioPermiso.java
├── UsuarioActividad.java
└── UsuarioSesion.java
→ Compilar y verificar

Grupo 3: Repositories
├── Todos los repositories (5 archivos)
→ Compilar y verificar

Grupo 4: Services
├── Todos los services (5 archivos)
→ Compilar y verificar

Grupo 5: Controllers
├── Todos los controllers (8 archivos)
→ Compilar y verificar
```

### Testing Crítico

```bash
# Tests de autenticación
mvn test -Dtest=AuthTest

# Tests de permisos
mvn test -Dtest=PermisoTest

# Tests de usuario
mvn test -Dtest=UsuarioTest

# Test completo
mvn test

# Arrancar aplicación y probar login
mvn spring-boot:run
# Ir a http://localhost:8080/login
# Verificar que login funciona
```

---

## 📦 FASE 10: MIGRAR CÓDIGO COMPARTIDO (shared/)

**Duración:** 2 horas  
**Complejidad:** ⭐⭐ Media

### Archivos a Migrar

```
Config (mantener en shared/config/):
├── SecurityConfig.java
├── WebConfig.java
├── ThymeleafConfig.java
└── DatabaseConfig.java

Exception:
├── GlobalExceptionHandler.java → shared/exception/
├── ResourceNotFoundException.java → shared/exception/
└── BusinessException.java → shared/exception/

Util:
├── DateUtil.java → shared/util/
├── FileUtil.java → shared/util/
└── ValidationUtil.java → shared/util/

DTOs compartidos:
└── ApiResponse.java → shared/dto/
```

### Consideraciones

- ⚠️ **Config ya está en `config/`**, solo mover a `shared/config/`
- ✅ Utils son fáciles de mover
- ✅ Exceptions son independientes

### Pasos

1. ✅ Mover utils
2. ✅ Mover exceptions
3. ✅ Mover DTOs compartidos
4. ✅ Mover configs a `shared/config/`
5. ✅ Actualizar imports
6. ✅ Compilar y testear

---

## 📦 FASE 11: MIGRAR CORE (listeners/schedulers)

**Duración:** 1 hora  
**Complejidad:** ⭐ Baja

### Archivos a Migrar

```
Listeners:
└── ApplicationStartupListener.java → core/listeners/

Schedulers:
└── CleanupScheduler.java → core/schedulers/

Events (base):
└── BaseEvent.java → core/events/ (si existe)
```

### Pasos

1. ✅ Mover listeners a `core/listeners/`
2. ✅ Mover schedulers a `core/schedulers/`
3. ✅ Actualizar packages
4. ✅ Actualizar imports
5. ✅ Compilar y testear

---

## 📦 FASE 12: LIMPIEZA FINAL

**Duración:** 1 hora  
**Complejidad:** ⭐ Baja

### Paso 12.1: Eliminar Carpetas Vacías

```bash
# Verificar que las carpetas antiguas están vacías
ls controllers/
ls services/
ls repositories/
ls models/

# Si están vacías, eliminarlas
rmdir controllers/
rmdir services/
rmdir repositories/
rmdir models/
```

### Paso 12.2: Limpiar Imports No Usados

**IntelliJ IDEA:**
1. `Ctrl + Alt + O` (Optimize Imports) en cada archivo
2. O usar: `Code` → `Optimize Imports` → `Whole Project`

### Paso 12.3: Formatear Código

**IntelliJ IDEA:**
1. `Ctrl + Alt + L` (Reformat Code)
2. O usar: `Code` → `Reformat Code` → `Whole Project`

### Paso 12.4: Compilación Final

```bash
# Limpiar todo
mvn clean

# Compilar completo
mvn compile

# Ejecutar todos los tests
mvn test

# Generar el JAR
mvn package
```

### Paso 12.5: Verificar Aplicación

```bash
# Arrancar aplicación
mvn spring-boot:run

# Verificar endpoints principales:
# - http://localhost:8080/
# - http://localhost:8080/login
# - http://localhost:8080/productos
# - http://localhost:8080/facturas
# - http://localhost:8080/whatsapp/mensajes
```

### Paso 12.6: Commit Final

```bash
git add .
git commit -m "refactor: Completar refactorización modular del proyecto

Cambios:
- ✅ 9 módulos migrados exitosamente
- ✅ Código compartido en shared/
- ✅ Infraestructura en core/
- ✅ Carpetas antiguas eliminadas
- ✅ Imports optimizados
- ✅ Código formateado
- ✅ Tests pasan: 100%
- ✅ Compilación exitosa
- ✅ Aplicación funciona correctamente

Módulos:
- modules/producto/
- modules/cliente/
- modules/facturacion/
- modules/whatsapp/
- modules/notificacion/
- modules/seguridad/
- modules/configuracion/
- modules/reportes/
- modules/presentacion/

Estructura:
- shared/ (config, exception, util, dto)
- core/ (listeners, schedulers, events)"
```

---

## ✅ CHECKLIST GENERAL DE MIGRACIÓN

### Por Cada Módulo

```
□ 1. Identificar todos los archivos del módulo
□ 2. Crear estructura de carpetas (controller, service, repository, model, dto, enums)
□ 3. Mover models primero
□ 4. Actualizar package en models
□ 5. Mover repositories
□ 6. Actualizar package en repositories
□ 7. Mover services
□ 8. Actualizar package en services
□ 9. Mover controllers
□ 10. Actualizar package en controllers
□ 11. Mover DTOs
□ 12. Mover Enums
□ 13. Actualizar todos los imports (Find & Replace)
□ 14. Compilar: mvn clean compile
□ 15. Verificar que compila sin errores
□ 16. Ejecutar tests: mvn test
□ 17. Verificar que tests pasan
□ 18. Arrancar aplicación: mvn spring-boot:run
□ 19. Verificar que endpoints funcionan
□ 20. Commit con mensaje descriptivo
```

---

## 🔍 CHECKLIST POR MÓDULO

### Módulo Producto ✅
```
□ ProductoController.java → modules/producto/controller/
□ ProductoService.java → modules/producto/service/
□ ProductoRepository.java → modules/producto/repository/
□ Producto.java → modules/producto/model/
□ ProductoDTO.java → modules/producto/dto/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Cliente ✅
```
□ ClienteController.java → modules/cliente/controller/
□ ClienteService.java → modules/cliente/service/
□ ClienteRepository.java → modules/cliente/repository/
□ Cliente.java → modules/cliente/model/
□ ClienteDTO.java → modules/cliente/dto/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Reportes ✅
```
□ ReporteController.java → modules/reportes/controller/
□ DashboardController.java → modules/reportes/controller/
□ ReporteService.java → modules/reportes/service/
□ ExportService.java → modules/reportes/service/
□ ReporteDTO.java → modules/reportes/dto/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Configuración ✅
```
□ ConfiguracionController.java
□ ConfiguracionEmailRestController.java
□ ConfiguracionEmpresaRestController.java
□ ConfiguracionFacturacionRestController.java
□ ParametroSistemaRestController.java
□ 5 Services relacionados
□ 5 Repositories relacionados
□ 5 Models relacionados
□ DTOs relacionados
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Facturación ✅
```
□ FacturaController.java
□ LineaFacturaController.java
□ FacturaService.java
□ LineaFacturaService.java
□ FacturaRepository.java
□ LineaFacturaRepository.java
□ Factura.java (MODEL - MOVER PRIMERO)
□ LineaFactura.java (MODEL - MOVER PRIMERO)
□ FacturaDTO.java
□ LineaFacturaDTO.java
□ EstadoFactura.java (enum)
□ Actualizar packages
□ Actualizar imports en TODO el proyecto (muchos archivos usan Factura)
□ Compilar
□ Tests
□ Commit
```

### Módulo WhatsApp ✅
```
□ 5 Controllers (WhatsApp*)
□ 5 Services (WhatsApp*, MensajeWhatsApp*, PlantillaWhatsApp*, Webhook*)
□ 3 Repositories
□ 3 Models (MensajeWhatsApp, PlantillaWhatsApp, WebhookLog)
□ 2 DTOs
□ 2 Enums (si existen)
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Notificación ✅
```
□ 3 Controllers (Notificacion*)
□ 4 Services
□ 4 Repositories
□ 4 Models
□ DTOs relacionados
□ 3 Enums (CanalNotificacion, TipoNotificacion, EstadoNotificacion)
□ Events (si existen)
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Módulo Seguridad ✅ (CRÍTICO)
```
□ 8 Controllers (Auth, Usuario*, Permiso*, Rol*, Perfil)
□ 5 Services
□ 5 Repositories
□ 6 Models (Usuario, Permiso, Rol, UsuarioPermiso, UsuarioActividad, UsuarioSesion)
□ DTOs relacionados
□ Enums (si existen)
□ Actualizar packages
□ Actualizar imports en TODO el proyecto
□ Compilar después de cada grupo
□ Tests críticos (Auth, Login, Permisos)
□ Verificar login en navegador
□ Commit
```

### Código Compartido (shared/) ✅
```
□ Config → shared/config/
□ Exception handlers → shared/exception/
□ Utils → shared/util/
□ DTOs compartidos → shared/dto/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Core (listeners/schedulers) ✅
```
□ Listeners → core/listeners/
□ Schedulers → core/schedulers/
□ Events base → core/events/
□ Actualizar packages
□ Actualizar imports
□ Compilar
□ Tests
□ Commit
```

### Limpieza Final ✅
```
□ Eliminar carpetas vacías antiguas
□ Optimize imports (Ctrl + Alt + O)
□ Reformat code (Ctrl + Alt + L)
□ mvn clean compile
□ mvn test
□ mvn package
□ mvn spring-boot:run
□ Verificar endpoints
□ Commit final
```

---

## 🛠️ RESOLUCIÓN DE PROBLEMAS

### Problema 1: Errores de Compilación

**Síntoma:**
```
[ERROR] cannot find symbol
symbol:   class Factura
location: class FacturaService
```

**Solución:**
```java
// Verificar que el import está correcto
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;

// No debe ser:
import api.astro.whats_orders_manager.models.Factura; // ❌ Antigua ubicación
```

**Usar IntelliJ:**
1. Click en el error
2. `Alt + Enter`
3. Seleccionar "Import class"

---

### Problema 2: Circular Dependencies

**Síntoma:**
```
The dependencies of some of the beans in the application context form a cycle
```

**Causas comunes:**
- Módulo A usa Módulo B
- Módulo B usa Módulo A

**Solución:**
```java
// Usar @Lazy para romper el ciclo
@Service
public class FacturaService {
    
    @Autowired
    @Lazy
    private NotificacionService notificacionService;
}
```

---

### Problema 3: Component Scan No Encuentra Beans

**Síntoma:**
```
Field xxxService in xxxController required a bean of type 'xxxService' that could not be found.
```

**Solución 1: Verificar @ComponentScan**
```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "api.astro.whats_orders_manager",
    "api.astro.whats_orders_manager.modules",
    "api.astro.whats_orders_manager.shared",
    "api.astro.whats_orders_manager.core"
})
public class WhatsOrdersManagerApplication {
    // ...
}
```

**Solución 2: Verificar anotaciones**
```java
// El service debe tener @Service
@Service
public class ProductoService {
    // ...
}

// El repository debe tener @Repository
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // ...
}
```

---

### Problema 4: Tests No Encuentran Clases

**Síntoma:**
```
java.lang.ClassNotFoundException: api.astro.whats_orders_manager.models.Producto
```

**Solución:**
Actualizar imports en los tests:

```java
// En ProductoTest.java
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.service.ProductoService;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
```

---

### Problema 5: Thymeleaf No Encuentra Templates

**Síntoma:**
```
Error resolving template [whatsapp/mensajes], template might not exist
```

**Solución:**
Los templates HTML no cambian de ubicación, siguen en:
```
src/main/resources/templates/
```

Solo cambia el Java, no los templates.

---

### Problema 6: Git Muestra Muchos Cambios

**Síntoma:**
`git status` muestra 100+ archivos modificados

**Solución:**
Es normal, estás moviendo muchos archivos. Hacer commits frecuentes:

```bash
# Commit por módulo
git add modules/producto/
git commit -m "refactor: Migrar módulo Producto"

git add modules/cliente/
git commit -m "refactor: Migrar módulo Cliente"

# etc.
```

---

### Problema 7: IntelliJ No Detecta Cambios

**Síntoma:**
IntelliJ sigue mostrando imports antiguos como válidos

**Solución:**
```
1. File → Invalidate Caches → Invalidate and Restart
2. Esperar a que IntelliJ re-indexe el proyecto
```

---

## ✅ VALIDACIÓN FINAL

### Checklist de Validación Completa

#### 1. Compilación
```bash
□ mvn clean compile
□ Sin errores de compilación
□ Sin warnings críticos
```

#### 2. Tests
```bash
□ mvn test
□ Todos los tests pasan
□ Coverage > 70% (si aplica)
```

#### 3. Empaquetado
```bash
□ mvn package
□ JAR generado exitosamente
□ Tamaño del JAR es razonable
```

#### 4. Ejecución
```bash
□ mvn spring-boot:run
□ Aplicación arranca sin errores
□ Puerto 8080 disponible
□ No hay stacktraces en consola
```

#### 5. Endpoints Principales
```
□ http://localhost:8080/ → Página principal
□ http://localhost:8080/login → Login funciona
□ http://localhost:8080/productos → Lista productos
□ http://localhost:8080/clientes → Lista clientes
□ http://localhost:8080/facturas → Lista facturas
□ http://localhost:8080/whatsapp/mensajes → Vista WhatsApp
□ http://localhost:8080/reportes/dashboard → Dashboard
```

#### 6. Funcionalidades Críticas
```
□ Login funciona correctamente
□ Crear producto funciona
□ Crear cliente funciona
□ Crear factura funciona
□ Enviar mensaje WhatsApp funciona
□ Ver reportes funciona
□ Permisos funcionan correctamente
```

#### 7. Base de Datos
```
□ Conexión a BD exitosa
□ Queries funcionan
□ No hay errores de JPA/Hibernate
```

#### 8. Logs
```
□ No hay errores en logs
□ No hay warnings críticos
□ Logging nivel apropiado
```

#### 9. Git
```
□ Todos los cambios commiteados
□ Branch feature/modular-refactoring actualizado
□ No hay archivos sin trackear importantes
```

#### 10. Documentación
```
□ README actualizado con nueva estructura
□ Javadocs actualizados (si aplica)
□ Esta guía marcada como completada
```

---

## 📊 MÉTRICAS ESPERADAS

### Antes de la Refactorización
```
controllers/          30 archivos
services/             28 archivos
repositories/         ~25 archivos
models/               25+ archivos
config/               ~5 archivos
util/                 ~3 archivos
Total carpetas raíz:  6-8
```

### Después de la Refactorización
```
modules/
├── producto/         ~5 archivos
├── cliente/          ~5 archivos
├── facturacion/      ~8 archivos
├── whatsapp/         ~18 archivos
├── notificacion/     ~18 archivos
├── seguridad/        ~24 archivos
├── configuracion/    ~20 archivos
├── reportes/         ~6 archivos
└── presentacion/     ~2 archivos

shared/               ~10 archivos
core/                 ~3 archivos

Total módulos:        9
Total archivos:       ~106 (igual que antes, solo reorganizados)
```

---

## 🎯 CRITERIOS DE ÉXITO

### ✅ La refactorización es exitosa si:

1. **Compilación limpia**
   - `mvn clean compile` → Sin errores
   - Todos los imports resueltos

2. **Tests pasan**
   - `mvn test` → 100% de tests pasan
   - No hay tests rotos

3. **Aplicación funciona**
   - `mvn spring-boot:run` → Arranca sin errores
   - Todos los endpoints funcionan
   - Login funciona
   - CRUD básico funciona

4. **Estructura correcta**
   - 9 módulos creados
   - shared/ con código compartido
   - core/ con infraestructura
   - Carpetas antiguas eliminadas

5. **Git limpio**
   - Todos los cambios commiteados
   - Branch actualizado
   - No hay conflictos

6. **Documentación actualizada**
   - README refleja nueva estructura
   - Esta guía completada
   - Checklist marcado

---

## 📝 NOTAS FINALES

### Tiempo Estimado Total
- **Óptimo:** 20 horas (2.5 días a tiempo completo)
- **Realista:** 25 horas (3 días)
- **Conservador:** 30 horas (4-5 días con interrupciones)

### Recomendaciones
1. ✅ **No apresurarse:** Mejor lento y seguro que rápido y roto
2. ✅ **Commits frecuentes:** Un commit por módulo migrado
3. ✅ **Tests después de cada paso:** No acumular cambios sin validar
4. ✅ **Backup antes de empezar:** Git branch + copia de seguridad
5. ✅ **Documentar problemas:** Si encuentras errores, documentarlos

### Siguiente Paso Después de Completar
```
□ Merge a master (después de QA)
□ Actualizar documentación del proyecto
□ Notificar al equipo
□ Celebrar 🎉
```

---

## 🎉 CONCLUSIÓN

Esta refactorización mejorará significativamente:
- ✅ **Mantenibilidad:** Código más fácil de mantener
- ✅ **Escalabilidad:** Fácil agregar nuevos módulos
- ✅ **Navegación:** Encontrar código es intuitivo
- ✅ **Testing:** Tests organizados por módulo
- ✅ **Trabajo en equipo:** Menos conflictos en Git

**¡Éxito con la refactorización!** 🚀

---

**Última actualización:** 27 de diciembre de 2025  
**Versión:** 1.0  
**Autor:** Equipo de Desarrollo
