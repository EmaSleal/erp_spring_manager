## 📝 Decisiones de Diseño

### ¿Por qué DTOs en lugar de clases internas?

| Clases Internas | DTOs en paquete dto/ |
|----------------|----------------------|
| ❌ No reutilizables | ✅ Reutilizables en todo el proyecto |
| ❌ Acopladas al controller | ✅ Desacopladas e independientes |
| ❌ Difíciles de testear | ✅ Fáciles de testear |
| ❌ No versionables | ✅ Versionables y documentables |

### ¿Por qué Utils estáticas?

- ✅ **Stateless**: No mantienen estado
- ✅ **Thread-safe**: Seguros en entornos concurrentes
- ✅ **Simplicidad**: No requieren inyección de dependencias
- ✅ **Performance**: No hay overhead de instanciación

### ¿Por qué Records para EstadisticasUsuariosDTO?

- ✅ **Inmutabilidad**: No se modifican después de creación
- ✅ **Concisión**: Menos código boilerplate
- ✅ **Seguridad**: No hay setters accidentales
- ✅ **Java 14+**: Aprovecha características modernas

---

