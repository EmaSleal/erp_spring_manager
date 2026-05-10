## Verificación

### Consulta SQL para verificar las preferencias creadas:

```sql
-- Ver todas las preferencias
SELECT 
    u.nombre as usuario,
    pn.tipo_notificacion,
    pn.canal,
    pn.activa,
    pn.frecuencia
FROM usuario u
INNER JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
ORDER BY u.nombre, pn.tipo_notificacion, pn.canal;

-- Contar preferencias por usuario
SELECT 
    u.nombre,
    COUNT(pn.id_preferencia) as total_preferencias
FROM usuario u
LEFT JOIN preferencia_notificacion pn ON u.id_usuario = pn.id_usuario
GROUP BY u.id_usuario, u.nombre;
```

---

