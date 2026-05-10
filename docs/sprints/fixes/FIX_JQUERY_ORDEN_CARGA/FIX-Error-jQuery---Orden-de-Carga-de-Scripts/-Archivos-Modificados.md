## 📊 Archivos Modificados

### 1. `templates/usuarios/usuarios.html`

**Cambios:**
- Movido `<script th:src="@{/js/usuarios.js}"></script>` 
- De: Dentro de `</section>` (antes de scripts comunes)
- A: Después de `<th:block th:replace="~{layout :: scripts}"></th:block>`

**Líneas afectadas:** 425-434

**Antes:**
```html
    <!-- Scripts específicos de usuarios -->
    <script th:src="@{/js/usuarios.js}"></script>
</section>
        </div>
    </main>

    <!-- Scripts comunes -->
    <th:block th:replace="~{layout :: scripts}"></th:block>
```

**Después:**
```html
</section>
        </div>
    </main>

    <!-- Scripts comunes (jQuery, Bootstrap, SweetAlert2) -->
    <th:block th:replace="~{layout :: scripts}"></th:block>
    
    <!-- Scripts específicos de usuarios (después de jQuery) -->
    <script th:src="@{/js/usuarios.js}"></script>
```

