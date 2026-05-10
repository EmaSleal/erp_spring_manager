## 🔍 Diferencias Técnicas: Clientes vs Productos

| Aspecto | Clientes | Productos |
|---------|----------|-----------|
| **Fuente de Datos** | AJAX (fetch) | Array global |
| **Endpoint** | `/clientes/detalle/{id}` | No aplica |
| **Eficiencia** | Bajo (HTTP request) | Alto (búsqueda local) |
| **Uso de Memoria** | Bajo | Alto (array en memoria) |
| **Tiempo de Respuesta** | ~100-300ms | ~1-5ms |

---

