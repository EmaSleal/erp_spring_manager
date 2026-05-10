## 📦 FASE 11: MIGRAR CORE (listeners/schedulers)

**Duración:** 1 hora  
**Complejidad:** ⭐ Baja

### Archivos a Migrar

```
Listeners:
└── ApplicationStartupListener.java → core/listeners/

Schedulers:
└── CleanupScheduler.java → core/schedulers/

Events (base):
└── BaseEvent.java → core/events/ (si existe)
```

### Pasos

1. ✅ Mover listeners a `core/listeners/`
2. ✅ Mover schedulers a `core/schedulers/`
3. ✅ Actualizar packages
4. ✅ Actualizar imports
5. ✅ Compilar y testear

---

