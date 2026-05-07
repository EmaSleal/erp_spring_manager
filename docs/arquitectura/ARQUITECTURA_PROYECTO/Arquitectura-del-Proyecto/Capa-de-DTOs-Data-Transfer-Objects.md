##  Capa de DTOs (Data Transfer Objects)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/dto/`

### DTOs Genéricos

| Clase | Tipo | Descripción | Uso |
|-------|------|-------------|-----|
| `PaginacionDTO.java` | Class | DTO genérico para resultados paginados | 3 controllers |
| `ResponseDTO.java` | Class | Respuestas API REST estandarizadas | 1 controller |
| `EstadisticasUsuariosDTO.java` | Record | Estadísticas de usuarios inmutables | UsuarioController |

### PaginacionDTO<T>

**Propiedades:**
- `List<T> contenido` - Elementos de la página actual
- `int paginaActual` - Número de página (0-indexed)
- `int tamanoPagina` - Elementos por página
- `long totalElementos` - Total de elementos
- `int totalPaginas` - Total de páginas

**Métodos útiles:**
- `tieneSiguiente()` - Verifica si hay página siguiente
- `tieneAnterior()` - Verifica si hay página anterior
- `esPrimera()` - Verifica si es la primera página
- `esUltima()` - Verifica si es la última página

### ResponseDTO

**Propiedades:**
- `boolean success` - Indica éxito/fallo
- `String message` - Mensaje descriptivo
- `Object data` - Datos opcionales

**Factory Methods:**
- `ResponseDTO.success(mensaje)` - Crea respuesta exitosa
- `ResponseDTO.success(mensaje, data)` - Crea respuesta exitosa con datos
- `ResponseDTO.error(mensaje)` - Crea respuesta de error
- `ResponseDTO.error(mensaje, data)` - Crea respuesta de error con datos
- `toMap()` - Convierte a Map para compatibilidad

### EstadisticasUsuariosDTO

**Record con campos:**
- `long total` - Total de usuarios
- `long activos` - Usuarios activos
- `long inactivos` - Usuarios inactivos
- `long administradores` - Total de administradores
- `long vendedores` - Total de vendedores

**Métodos calculados:**
- `porcentajeActivos()` - % de usuarios activos
- `porcentajeAdministradores()` - % de administradores

---

