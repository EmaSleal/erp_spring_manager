## 📚 Principios Aplicados

### 1. Single Responsibility Principle (SRP)
- Controller: Solo maneja HTTP requests/responses
- Service: Lógica de negocio y decisiones

### 2. Don't Repeat Yourself (DRY)
- Lógica común en `saveOrUpdate()`
- No duplicación de validaciones

### 3. Separation of Concerns
- Controller: Capa de presentación
- Service: Capa de negocio
- Repository: Capa de datos

### 4. RESTful Best Practices
- POST → Crear recurso nuevo
- PUT → Actualizar recurso existente
- Status codes HTTP semánticos

---

