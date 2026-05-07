## 📋 Problema Reportado

### **Error 1: Chart.js bloqueado por integrity hash**

```
Failed to find a valid digest in the 'integrity' attribute for resource 
'https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js' 
with computed SHA-384 integrity 'FcQlsUOd0TJjROrBxhJdUhXTUgNJQxTMcxZe6nHbaEfFL1zjQ+bq/uRoBQxb0KMo'. 
The resource has been blocked.
```

**Síntomas:**
- Chart.js no se carga
- `Uncaught ReferenceError: Chart is not defined`
- Todos los gráficos fallan al renderizar

### **Problema 2: Sobrecarga del servidor web**

El usuario reportó preocupación por:
- Procesamiento excesivo de datos en el servidor de aplicaciones
- Stream API de Java procesando miles de registros
- Cálculos complejos en memoria (agrupaciones, ordenamientos)
- Conversión de tipos y formateo de fechas en Java

---

