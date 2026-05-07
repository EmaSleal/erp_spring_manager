## Cómo Probar en el Sistema

### 1. Desde Postman/Insomnia

**Request:**
```
POST http://localhost:8080/api/notificaciones/admin/inicializar-preferencias
```

**Headers:**
```
Content-Type: application/json
```

**Body:** (vacío)

---

### 2. Desde JavaScript en el Navegador

```javascript
fetch('/api/notificaciones/admin/inicializar-preferencias', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    }
})
.then(response => response.json())
.then(data => {
    console.log('Resultado:', data);
    alert(`✅ Preferencias creadas: ${data.preferenciasCreadas} para ${data.usuariosSinPreferencias} usuarios`);
})
.catch(error => console.error('Error:', error));
```

---

### 3. Desde cURL (Terminal)

```bash
# Inicializar para todos los usuarios (requiere ser admin)
curl -X POST http://localhost:8080/api/notificaciones/admin/inicializar-preferencias \
  -H "Content-Type: application/json"

# Inicializar solo para usuario actual
curl -X POST http://localhost:8080/api/notificaciones/preferencias/inicializar \
  -H "Content-Type: application/json"
```

---

