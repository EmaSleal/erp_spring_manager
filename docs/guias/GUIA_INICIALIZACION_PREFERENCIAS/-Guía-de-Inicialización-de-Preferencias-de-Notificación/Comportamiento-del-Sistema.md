## Comportamiento del Sistema

### Lógica de Preferencias

1. **Preferencia Global (NULL, NULL):**
   - Si `activa = true`: Usuario RECIBE todas las notificaciones
   - Si `activa = false`: Usuario NO recibe ninguna notificación
   - Tiene prioridad sobre preferencias específicas

2. **Preferencias Específicas:**
   - Se evalúan solo si la preferencia global lo permite
   - Permiten control granular por tipo y canal
   - Pueden tener diferentes frecuencias (INMEDIATA, DIARIA, SEMANAL)

3. **Campo `notificacionesDesactivadasGlobal`:**
   - Si es `true`: Usuario NO recibe NINGUNA notificación
   - Tiene máxima prioridad sobre todas las demás configuraciones

---

