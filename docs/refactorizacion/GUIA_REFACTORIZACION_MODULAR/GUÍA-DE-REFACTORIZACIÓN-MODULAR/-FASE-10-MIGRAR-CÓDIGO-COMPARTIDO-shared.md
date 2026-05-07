## 📦 FASE 10: MIGRAR CÓDIGO COMPARTIDO (shared/)

**Duración:** 2 horas  
**Complejidad:** ⭐⭐ Media

### Archivos a Migrar

```
Config (mantener en shared/config/):
├── SecurityConfig.java
├── WebConfig.java
├── ThymeleafConfig.java
└── DatabaseConfig.java

Exception:
├── GlobalExceptionHandler.java → shared/exception/
├── ResourceNotFoundException.java → shared/exception/
└── BusinessException.java → shared/exception/

Util:
├── DateUtil.java → shared/util/
├── FileUtil.java → shared/util/
└── ValidationUtil.java → shared/util/

DTOs compartidos:
└── ApiResponse.java → shared/dto/
```

### Consideraciones

- ⚠️ **Config ya está en `config/`**, solo mover a `shared/config/`
- ✅ Utils son fáciles de mover
- ✅ Exceptions son independientes

### Pasos

1. ✅ Mover utils
2. ✅ Mover exceptions
3. ✅ Mover DTOs compartidos
4. ✅ Mover configs a `shared/config/`
5. ✅ Actualizar imports
6. ✅ Compilar y testear

---

