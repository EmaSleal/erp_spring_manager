## 💡 NOTAS TÉCNICAS

### Detección de Tema del Sistema

**JavaScript:**
```javascript
// Detectar preferencia del sistema
const prefersDarkScheme = window.matchMedia('(prefers-color-scheme: dark)');

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('theme', theme);
}

// Auto: usar preferencia del sistema
if (prefersDarkScheme.matches) {
  applyTheme('dark');
} else {
  applyTheme('light');
}

// Escuchar cambios en preferencia del sistema
prefersDarkScheme.addEventListener('change', (e) => {
  applyTheme(e.matches ? 'dark' : 'light');
});
```

---

### Modo Offline - Queue de Operaciones

**IndexedDB para operaciones pendientes:**
```javascript
// Guardar operación offline
async function saveOfflineOperation(operation) {
  const db = await openDB('offline-queue');
  await db.add('operations', {
    url: operation.url,
    method: operation.method,
    body: operation.body,
    timestamp: Date.now()
  });
}

// Sincronizar cuando haya conexión
window.addEventListener('online', async () => {
  const db = await openDB('offline-queue');
  const operations = await db.getAll('operations');
  
  for (const op of operations) {
    try {
      await fetch(op.url, {
        method: op.method,
        body: JSON.stringify(op.body)
      });
      await db.delete('operations', op.id);
    } catch (error) {
      console.error('Error sincronizando:', error);
    }
  }
});
```

---

### Optimización de Imágenes

**Conversión a WebP:**
```bash
# Convertir todas las imágenes a WebP
for file in *.jpg; do
  cwebp -q 80 "$file" -o "${file%.jpg}.webp"
done
```

**HTML con fallback:**
```html
<picture>
  <source srcset="imagen.webp" type="image/webp">
  <source srcset="imagen.jpg" type="image/jpeg">
  <img src="imagen.jpg" alt="Descripción" loading="lazy">
</picture>
```

---

### Medición de Performance

**Web Vitals:**
```javascript
import { getCLS, getFID, getFCP, getLCP, getTTFB } from 'web-vitals';

function sendToAnalytics(metric) {
  console.log(metric.name, metric.value);
  // Enviar a Google Analytics o similar
}

getCLS(sendToAnalytics);
getFID(sendToAnalytics);
getFCP(sendToAnalytics);
getLCP(sendToAnalytics);
getTTFB(sendToAnalytics);
```

---

