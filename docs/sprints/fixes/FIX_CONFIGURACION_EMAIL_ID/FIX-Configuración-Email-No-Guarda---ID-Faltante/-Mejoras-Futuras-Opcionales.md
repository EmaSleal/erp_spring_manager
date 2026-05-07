## 🔧 Mejoras Futuras Opcionales

### 1. Usar Campo Hidden en HTML

```html
<form id="form-email">
    <input type="hidden" id="idConfiguracionEmail" value="">
    <!-- ... resto de campos -->
</form>
```

```javascript
// Al cargar
document.getElementById('idConfiguracionEmail').value = datos.idConfiguracion || '';

// Al guardar
datos.idConfiguracion = parseInt(document.getElementById('idConfiguracionEmail').value) || null;
```

### 2. Endpoint RESTful con ID en URL

```javascript
// PUT con ID en path
PUT /api/configuracion/email/1
{ smtpHost: "smtp.gmail.com", ... }
```

```java
@PutMapping("/{id}")
public ResponseEntity<?> actualizar(
    @PathVariable Integer id,
    @RequestBody ConfiguracionEmailDTO dto) {
    // ID viene del path, no del body
}
```

### 3. Validación Optimista con ETag

```javascript
// Incluir versión para detectar conflictos
const response = await fetch(url, {
    headers: { 'If-Match': lastETag }
});

if (response.status === 412) {
    alert('Configuración modificada por otro usuario. Refresca la página.');
}
```

---

