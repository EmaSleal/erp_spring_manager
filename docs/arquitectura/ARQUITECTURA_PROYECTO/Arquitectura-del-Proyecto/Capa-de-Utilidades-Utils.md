##  Capa de Utilidades (Utils)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/util/`

### Utilidades Estáticas

| Clase | Descripción | Métodos Principales | Uso |
|-------|-------------|---------------------|-----|
| `ResponseUtil.java` | Respuestas HTTP estandarizadas | `error()`, `success()`, `successData()` | 1 controller |
| `PasswordUtil.java` | Generación de contraseñas seguras | `generarPasswordAleatoria()`, `esPasswordValida()` | 1 controller |
| `PaginacionUtil.java` | Operaciones de paginación | `fromPage()`, `crear()`, `agregarAtributos()` | 3 controllers |

### ResponseUtil

**Métodos estáticos:**
```java
ResponseEntity<Map<String, Object>> error(String mensaje)
ResponseEntity<Map<String, Object>> error(String mensaje, Object data)
ResponseEntity<Map<String, Object>> success(String mensaje)
ResponseEntity<Map<String, Object>> success(String mensaje, Object data)
ResponseEntity<Map<String, Object>> successData(Object data)
```

### PasswordUtil

**Características:**
- Usa `SecureRandom` para generación segura
- Caracteres: A-Z, a-z, 0-9, !@#$%
- Longitud por defecto: 10 caracteres
- Longitud mínima: 6 caracteres

**Métodos estáticos:**
```java
String generarPasswordAleatoria()
String generarPasswordAleatoria(int longitud)
boolean esPasswordValida(String password)
int getLongitudMinima()
```

### PaginacionUtil

**Métodos estáticos:**
```java
PaginacionDTO<T> fromPage(Page<T> page)  // Spring Page → PaginacionDTO
PaginacionDTO<T> crear(List<T> contenido, int paginaActual, int tamanoPagina, long totalElementos)
void agregarAtributos(Model model, PaginacionDTO<T> paginacion, String nombreAtributo)
void agregarAtributosConOrdenamiento(Model model, PaginacionDTO<T> paginacion, String nombreAtributo, String sortBy, String sortDir)
```

---

