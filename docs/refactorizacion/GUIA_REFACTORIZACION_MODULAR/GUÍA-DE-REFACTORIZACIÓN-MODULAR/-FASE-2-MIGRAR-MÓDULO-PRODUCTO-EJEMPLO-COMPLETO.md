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
- ✅ Endpoint de productos funciona: `http://localhost:9090/productos`
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

