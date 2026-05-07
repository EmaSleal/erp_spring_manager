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

