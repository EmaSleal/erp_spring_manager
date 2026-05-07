# etc.
```

### Ejecución

```sql
-- Ejecutar en orden:
source docs/base\ de\ datos/MIGRATION_2FA_SPRINT_7.sql;
source docs/base\ de\ datos/MIGRATION_REMEMBER_ME_TOKENS_SPRINT_7.sql;
-- ...
```

---

## ✅ Checklist Post-Migración

- [ ] Backup restaurado y funcional (en caso de emergencia)
- [ ] Migraciones SQL ejecutadas sin errores
- [ ] Tests unitarios pasando
- [ ] Tests de integración pasando
- [ ] Login con email funciona
- [ ] Login con username funciona
- [ ] Remember Me funciona
- [ ] 2FA se puede habilitar
- [ ] Auditoría registra eventos
- [ ] No hay errores en logs de aplicación

---

**Responsable:** DevOps / DBA  
**Duración estimada:** 2-3 horas  
**Downtime requerido:** Opcional (migrations soportan dual-write)
```

---

