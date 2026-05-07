## 📦 CÓDIGO COMPARTIDO (shared/)

### Config
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.config.SecurityConfig;` | `import api.astro.whats_orders_manager.shared.config.SecurityConfig;` |
| `import api.astro.whats_orders_manager.config.WebConfig;` | `import api.astro.whats_orders_manager.shared.config.WebConfig;` |
| `import api.astro.whats_orders_manager.config.ThymeleafConfig;` | `import api.astro.whats_orders_manager.shared.config.ThymeleafConfig;` |
| `import api.astro.whats_orders_manager.config.DatabaseConfig;` | `import api.astro.whats_orders_manager.shared.config.DatabaseConfig;` |

### Exception
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.exception.GlobalExceptionHandler;` | `import api.astro.whats_orders_manager.shared.exception.GlobalExceptionHandler;` |
| `import api.astro.whats_orders_manager.exception.ResourceNotFoundException;` | `import api.astro.whats_orders_manager.shared.exception.ResourceNotFoundException;` |
| `import api.astro.whats_orders_manager.exception.BusinessException;` | `import api.astro.whats_orders_manager.shared.exception.BusinessException;` |

### Util
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.util.DateUtil;` | `import api.astro.whats_orders_manager.shared.util.DateUtil;` |
| `import api.astro.whats_orders_manager.util.FileUtil;` | `import api.astro.whats_orders_manager.shared.util.FileUtil;` |
| `import api.astro.whats_orders_manager.util.ValidationUtil;` | `import api.astro.whats_orders_manager.shared.util.ValidationUtil;` |

### DTOs Compartidos
| ANTES | DESPUÉS |
|-------|---------|
| `import api.astro.whats_orders_manager.models.dto.ApiResponse;` | `import api.astro.whats_orders_manager.shared.dto.ApiResponse;` |

---

