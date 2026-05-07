## 🎯 LECCIÓN APRENDIDA

### Problema Común: POST-REDIRECT-GET Pattern

Cuando usas **fragments de Thymeleaf** que dependen de **múltiples objetos en el modelo**, el redirect después de un POST debe ir a un endpoint que:

1. ✅ **Cargue TODOS los objetos** necesarios para la vista completa
2. ✅ **Use parámetros GET** para indicar qué fragment activar
3. ❌ **NO redirija a un endpoint específico** que solo carga un subset de datos

### Regla General:

```java
// ❌ EVITAR: Redirect a endpoint específico
return "redirect:/configuracion/notificaciones";

// ✅ PREFERIR: Redirect a endpoint principal con parámetro
return "redirect:/configuracion?tab=notificaciones";
```

---

