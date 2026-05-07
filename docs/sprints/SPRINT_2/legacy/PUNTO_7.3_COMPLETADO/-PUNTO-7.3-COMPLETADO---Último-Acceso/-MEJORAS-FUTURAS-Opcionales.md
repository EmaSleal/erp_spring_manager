## 🎯 MEJORAS FUTURAS (Opcionales)

### 1. Mostrar tiempo relativo ⏸️
```html
<!-- Ejemplo: "Hace 2 horas" en lugar de "20/10/2025 11:37" -->
<span th:text="${@timeAgoService.getTimeAgo(usuario.ultimoAcceso)}">Hace 2 horas</span>
```

### 2. Indicador visual de actividad reciente ⏸️
```html
<!-- Verde si login en últimas 24h, amarillo si < 7 días, rojo si > 30 días -->
<span class="status-indicator" 
      th:classappend="${@activityService.getActivityStatus(usuario.ultimoAcceso)}">
</span>
```

### 3. Historial de accesos ⏸️
- Crear tabla `accesos_historial`
- Registrar cada login con IP, navegador, ubicación
- Vista de historial en perfil de usuario

### 4. Alertas de seguridad ⏸️
- Notificar si login desde nueva ubicación
- Alertar si login fuera de horario habitual
- Detectar logins simultáneos sospechosos

### 5. Dashboard de actividad ⏸️
- Gráfico de logins por hora/día/mes
- Usuarios más activos
- Usuarios inactivos (sin login en X días)

---

