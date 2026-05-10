## 📦 MÓDULO FACTURACIÓN

### Controllers
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.controllers.FacturaController;` | `import api.astro.whats_orders_manager.modules.facturacion.controller.FacturaController;` |
| `import api.astro.whats_orders_manager.controllers.LineaFacturaController;` | `import api.astro.whats_orders_manager.modules.facturacion.controller.LineaFacturaController;` |

### Services
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.services.FacturaService;` | `import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;` |
| `import api.astro.whats_orders_manager.services.LineaFacturaService;` | `import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;` |

### Repositories
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.repositories.FacturaRepository;` | `import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;` |
| `import api.astro.whats_orders_manager.repositories.LineaFacturaRepository;` | `import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;` |

### Models (⚠️ MUCHOS ARCHIVOS USAN ESTOS)
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.Factura;` | `import api.astro.whats_orders_manager.modules.facturacion.model.Factura;` |
| `import api.astro.whats_orders_manager.models.LineaFactura;` | `import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;` |

### DTOs
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.FacturaDTO;` | `import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDTO;` |
| `import api.astro.whats_orders_manager.models.dto.LineaFacturaDTO;` | `import api.astro.whats_orders_manager.modules.facturacion.dto.LineaFacturaDTO;` |

### Enums
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.enums.EstadoFactura;` | `import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoFactura;` |

---

