# Servicio de Ubicaciones Costa Rica

## Resumen

Sistema unificado para gestionar ubicaciones de Costa Rica (provincias, cantones y distritos) con un único servicio y endpoints REST.

## Arquitectura

### Entidades JPA

**1. ProvinciaCostaRica** (`cat_provincia_cr`)
- PK: `codigo` (CHAR(1)) - Códigos del 1 al 7
- Campos: `nombre`, `created_at`, `updated_at`
- Ejemplo: `'1'` = "San José"

**2. CantonCostaRica** (`cat_canton_cr`)
- PK Compuesta: `(provincia_codigo, codigo)`
- Campos: `nombre`, `created_at`, `updated_at`
- Relación: ManyToOne → ProvinciaCostaRica
- Ejemplo: `('1', '01')` = "San José"

**3. DistritoCostaRica** (`cat_distrito_cr`)
- PK Compuesta: `(provincia_codigo, canton_codigo, codigo)`
- Campos: `nombre`, `created_at`, `updated_at`
- Relación: ManyToOne → CantonCostaRica
- Ejemplo: `('1', '01', '01')` = "Carmen"

### Repositories

- **ProvinciaCostaRicaRepository**: Buscar por código, nombre; listar ordenadas
- **CantonCostaRicaRepository**: Buscar por provincia; búsqueda por nombre; contar
- **DistritoCostaRicaRepository**: Buscar por provincia/cantón; búsqueda por nombre; contar

### Servicio Unificado

**UbicacionService** - Único servicio que gestiona las 3 entidades:

**Métodos de Provincias:**
- `obtenerProvincias()` - Lista todas las provincias
- `obtenerProvinciaPorCodigo(codigo)` - Obtiene una provincia específica
- `buscarProvinciaPorNombre(nombre)` - Busca por nombre exacto

**Métodos de Cantones:**
- `obtenerCantonesPorProvincia(provinciaCodigo)` - Cantones de una provincia
- `obtenerCanton(provinciaCodigo, cantonCodigo)` - Cantón específico
- `buscarCantonesPorNombre(nombre)` - Búsqueda parcial por nombre
- `contarCantonesPorProvincia(provinciaCodigo)` - Cuenta cantones

**Métodos de Distritos:**
- `obtenerDistritosPorProvincia(provinciaCodigo)` - Todos los distritos de provincia
- `obtenerDistritosPorCanton(provinciaCodigo, cantonCodigo)` - Distritos de cantón
- `obtenerDistrito(provinciaCodigo, cantonCodigo, distritoCodigo)` - Distrito específico
- `buscarDistritosPorNombre(nombre)` - Búsqueda parcial por nombre
- `contarDistritosPorCanton(provinciaCodigo, cantonCodigo)` - Cuenta distritos

**Métodos Combinados:**
- `obtenerUbicacionCompleta(p, c, d)` - Devuelve objeto con todos los datos
- `validarUbicacion(p, c, d)` - Verifica que existe la combinación

### DTOs

- **ProvinciaDTO**: `codigo`, `nombre`
- **CantonDTO**: `provinciaCodigo`, `codigo`, `nombre`, `codigoCompleto` ("1-01")
- **DistritoDTO**: `provinciaCodigo`, `cantonCodigo`, `codigo`, `nombre`, `codigoCompleto` ("1-01-01")
- **UbicacionCompletaDTO**: Todos los campos + `ubicacionCompleta` ("San José, San José, Carmen")

## API REST Endpoints

### Provincias

```http
# Listar todas las provincias
GET /api/ubicaciones/provincias

# Obtener provincia específica
GET /api/ubicaciones/provincias/{codigo}
```

### Cantones

```http
# Listar cantones de una provincia
GET /api/ubicaciones/cantones?provincia=1

# Obtener cantón específico
GET /api/ubicaciones/cantones/1/01

# Buscar cantones por nombre
GET /api/ubicaciones/cantones/buscar?nombre=san
```

### Distritos

```http
# Listar distritos de un cantón
GET /api/ubicaciones/distritos?provincia=1&canton=01

# Obtener distrito específico
GET /api/ubicaciones/distritos/1/01/01

# Buscar distritos por nombre
GET /api/ubicaciones/distritos/buscar?nombre=carmen
```

### Combinados

```http
# Obtener ubicación completa
GET /api/ubicaciones/completa?provincia=1&canton=01&distrito=01

# Validar ubicación
GET /api/ubicaciones/validar?provincia=1&canton=01&distrito=01

# Estadísticas generales
GET /api/ubicaciones/estadisticas
```

## Ejemplos de Uso

### JavaScript - Cargar Dropdown Dinámico

```javascript
// Cargar provincias al iniciar
async function cargarProvincias() {
    const response = await fetch('/api/ubicaciones/provincias');
    const provincias = await response.json();
    
    const select = document.getElementById('provincia');
    select.innerHTML = '<option value="">Seleccione...</option>';
    
    provincias.forEach(p => {
        select.innerHTML += `<option value="${p.codigo}">${p.nombre}</option>`;
    });
}

// Cargar cantones cuando se selecciona provincia
document.getElementById('provincia').addEventListener('change', async function() {
    const provinciaCodigo = this.value;
    if (!provinciaCodigo) return;
    
    const response = await fetch(`/api/ubicaciones/cantones?provincia=${provinciaCodigo}`);
    const cantones = await response.json();
    
    const select = document.getElementById('canton');
    select.innerHTML = '<option value="">Seleccione...</option>';
    
    cantones.forEach(c => {
        select.innerHTML += `<option value="${c.codigo}">${c.nombre}</option>`;
    });
});

// Cargar distritos cuando se selecciona cantón
document.getElementById('canton').addEventListener('change', async function() {
    const provinciaCodigo = document.getElementById('provincia').value;
    const cantonCodigo = this.value;
    if (!cantonCodigo) return;
    
    const response = await fetch(
        `/api/ubicaciones/distritos?provincia=${provinciaCodigo}&canton=${cantonCodigo}`
    );
    const distritos = await response.json();
    
    const select = document.getElementById('distrito');
    select.innerHTML = '<option value="">Seleccione...</option>';
    
    distritos.forEach(d => {
        select.innerHTML += `<option value="${d.codigo}">${d.nombre}</option>`;
    });
});
```

### Java - Uso desde otro Service

```java
@Service
@RequiredArgsConstructor
public class EmpresaService {
    
    private final UbicacionService ubicacionService;
    
    public void validarDatosEmpresa(Empresa empresa) {
        // Validar ubicación
        boolean ubicacionValida = ubicacionService.validarUbicacion(
            empresa.getProvincia(),
            empresa.getCanton(),
            empresa.getDistrito()
        );
        
        if (!ubicacionValida) {
            throw new IllegalArgumentException("Ubicación inválida");
        }
        
        // Obtener datos completos para XML
        UbicacionCompletaDTO ubicacion = ubicacionService.obtenerUbicacionCompleta(
            empresa.getProvincia(),
            empresa.getCanton(),
            empresa.getDistrito()
        );
        
        log.info("Ubicación: {}", ubicacion.getUbicacionCompleta());
    }
}
```

## Datos Cargados

Después de ejecutar los scripts SQL:
- ✅ 7 provincias
- ✅ 81 cantones
- ✅ 475 distritos
- ✅ 6,576 barrios (opcional)

## Próximos Pasos

1. **Ejecutar scripts SQL**: 
   ```sql
   SOURCE docs/base de datos/EJECUTAR_UBICACIONES_CR.sql;
   ```

2. **Iniciar Spring Boot**: Las entidades JPA se auto-registrarán

3. **Probar endpoints**:
   ```bash
   curl http://localhost:8080/api/ubicaciones/provincias
   curl http://localhost:8080/api/ubicaciones/cantones?provincia=1
   curl http://localhost:8080/api/ubicaciones/estadisticas
   ```

4. **Integrar en formularios**: Usar JavaScript para cargar dropdowns dinámicos en:
   - Formulario de Empresa (add-form.html)
   - Formulario de Cliente (add-form.html)

## Ventajas del Diseño

✅ **Un solo servicio**: UbicacionService gestiona las 3 entidades  
✅ **Repositorios separados**: Cada entidad mantiene su repository propio (buena práctica JPA)  
✅ **API RESTful**: Endpoints consistentes y fáciles de consumir  
✅ **DTOs específicos**: Respuestas optimizadas para cada caso de uso  
✅ **Validación integrada**: Método `validarUbicacion()` para verificar combinaciones  
✅ **Búsquedas flexibles**: Por código, por nombre, búsqueda parcial  
✅ **Transaccionalidad**: `@Transactional(readOnly = true)` para optimizar consultas
