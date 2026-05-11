## 🐛 TROUBLESHOOTING

### Problema 1: No se muestran las iniciales
**Causa:** `userInitials` es null o vacío  
**Solución:** Verificar que `GlobalControllerAdvice` está siendo invocado
```bash
# Verificar en logs:
DEBUG - Agregando datos globales del usuario: 555666777
DEBUG - Datos globales agregados - Usuario: Juan Pérez, Rol: ADMIN, Iniciales: JP
```

### Problema 2: Avatar no se muestra (muestra iniciales)
**Causa:** `userAvatar` es null o la ruta es incorrecta  
**Solución:** 
1. Verificar que el campo `avatar` existe en BD
2. Verificar que la ruta es accesible: `http://localhost:9090/uploads/avatars/juan.jpg`
3. Verificar configuración de recursos estáticos en Spring

### Problema 3: Avatar se ve distorsionado
**Causa:** CSS no está aplicando `object-fit: cover`  
**Solución:** Verificar que la clase `avatar-img` tiene:
```css
.avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}
```

### Problema 4: @ControllerAdvice no funciona
**Causa:** No está siendo escaneado por Spring  
**Solución:** Verificar que está en el paquete correcto:
```
api.astro.whats_orders_manager.config
```
O agregar `@ComponentScan` si es necesario.

---

