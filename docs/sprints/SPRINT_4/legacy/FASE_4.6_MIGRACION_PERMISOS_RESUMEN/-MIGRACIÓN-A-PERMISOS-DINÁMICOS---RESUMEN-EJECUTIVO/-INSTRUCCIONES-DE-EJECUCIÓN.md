## 🚀 INSTRUCCIONES DE EJECUCIÓN

### PASO 1: Arrancar Spring Boot

```bash
mvn spring-boot:run
```

**¿Qué sucede?**
- ✅ Spring Boot arranca la aplicación
- ✅ JPA/Hibernate crea automáticamente las 4 tablas nuevas
- ✅ Se agrega la columna `id_rol` a la tabla `usuario`
- ⚠️ Las tablas estarán VACÍAS (sin datos)

### PASO 2: Ejecutar Script SQL

**Abrir tu cliente MySQL favorito** (MySQL Workbench, HeidiSQL, DBeaver, etc.) y ejecutar:

```bash
docs/base de datos/MIGRATION_PERMISOS_DINAMICOS.sql
```

**¿Qué sucede?**
- ✅ Se insertan **48 permisos** en la tabla `permiso`
- ✅ Se insertan **3 roles** en la tabla `rol`
- ✅ Se asignan **15 permisos** al rol VENDEDOR
- ✅ Se asignan **30 permisos** al rol GERENTE
- ✅ Se asignan **48 permisos** al rol ADMIN
- ✅ (Opcional) Se migran usuarios existentes al nuevo sistema

### PASO 3: Verificar Migración

Ejecutar estas consultas para verificar:

```sql
-- Verificar permisos por rol
SELECT r.nombre, COUNT(rp.id_permiso) as total_permisos
FROM rol r
LEFT JOIN rol_permiso rp ON r.id_rol = rp.id_rol
GROUP BY r.nombre;

-- Resultado esperado:
-- VENDEDOR: 15 permisos
-- GERENTE: 30 permisos
-- ADMIN: 48 permisos
```

---

