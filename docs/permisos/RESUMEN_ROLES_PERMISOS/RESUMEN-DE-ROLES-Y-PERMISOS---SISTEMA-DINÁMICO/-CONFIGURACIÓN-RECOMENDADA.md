## ⚙️ CONFIGURACIÓN RECOMENDADA

### Permisos Personalizados

El sistema permite **permisos personalizados por usuario** mediante la tabla `usuario_permiso`:

```sql
-- Conceder permiso adicional a un usuario
INSERT INTO usuario_permiso (id_usuario, id_permiso, tipo, concedido_por)
VALUES (5, 12, 'CONCEDIDO', 1);

-- Denegar permiso a un usuario (sobrescribe rol)
INSERT INTO usuario_permiso (id_usuario, id_permiso, tipo, concedido_por)
VALUES (7, 23, 'DENEGADO', 1);
```

**Casos de Uso:**
- Vendedor de confianza → Conceder `FACTURA_ELIMINAR`
- Gerente en período de prueba → Denegar `PRODUCTO_ELIMINAR`
- Usuario temporal → Denegar todos excepto `FACTURA_VER`

---

