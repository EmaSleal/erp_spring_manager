##  Tareas Programadas (Schedulers)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/schedulers/`

| Scheduler | Descripción | Frecuencia |
|-----------|-------------|------------|
| `RecordatorioPagoScheduler.java` | Envío automático de recordatorios de pago por email | Configurable (cron) |

**Configuración:** Habilitado con `@EnableScheduling` en la clase principal.

---

