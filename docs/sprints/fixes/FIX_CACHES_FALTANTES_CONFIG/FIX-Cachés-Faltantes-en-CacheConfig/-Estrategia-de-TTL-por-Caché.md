## 📊 Estrategia de TTL por Caché

| Caché | TTL | Justificación |
|-------|-----|---------------|
| `reportes` | 5 min | Datos cambian frecuentemente con nuevas ventas |
| `estadisticas` | 10 min | Estadísticas agregadas menos volátiles |
| `graficas` | 5 min | Sincronizadas con reportes |
| `exportaciones` | 2 min | Archivos grandes, liberar memoria rápido |
| `empresa` | 30 min | Configuración estable, cambia raramente |
| `plantillas` | 15 min | Contenido semi-estático |
| `configuracionFacturacion` | 30 min | Configuración estable |
| `configuracionNotificaciones` | 30 min | Configuración estable |

---

