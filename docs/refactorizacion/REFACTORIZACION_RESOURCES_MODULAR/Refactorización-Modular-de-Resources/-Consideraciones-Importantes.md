## ⚠️ Consideraciones Importantes

### 1. **Rutas en Spring Boot**

Spring Boot busca recursos estáticos en estas ubicaciones por defecto:
- `/static/`
- `/public/`
- `/resources/`
- `/META-INF/resources/`

**Problema:** Si movemos archivos a `modules/`, Spring Boot NO los encontrará automáticamente.

**Soluciones:**

#### Opción A: Configurar ResourceHandlers (RECOMENDADA)
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Recursos compartidos
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/shared/static/");
        
        // Recursos por módulo
        registry.addResourceHandler("/modules/cliente/static/**")
                .addResourceLocations("classpath:/modules/cliente/static/");
        
        registry.addResourceHandler("/modules/producto/static/**")
                .addResourceLocations("classpath:/modules/producto/static/");
        
        // ... etc para cada módulo
    }
}
```

#### Opción B: Mantener estructura plana pero organizada
```
static/
├── shared/
│   ├── css/
│   ├── js/
│   └── images/
└── modules/
    ├── cliente/
    ├── producto/
    └── ...
```

Esta opción mantiene todo bajo `/static/` pero organizado internamente.

### 2. **Thymeleaf Template Resolution**

Thymeleaf busca templates en `classpath:/templates/` por defecto.

**Problema:** Similar a los archivos estáticos, si movemos templates a `modules/`, necesitamos configuración adicional.

**Soluciones:**

#### Opción A: Configurar múltiples template resolvers
```java
@Configuration
public class ThymeleafConfig {
    
    @Bean
    public SpringResourceTemplateResolver sharedTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/shared/templates/");
        resolver.setSuffix(".html");
        resolver.setOrder(1);
        resolver.setCheckExistence(true);
        return resolver;
    }
    
    @Bean
    public SpringResourceTemplateResolver modulesTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/modules/");
        resolver.setSuffix(".html");
        resolver.setOrder(2);
        resolver.setCheckExistence(true);
        return resolver;
    }
}
```

#### Opción B: Usar prefijos en los nombres de vistas
Mantener templates en `classpath:/templates/` pero organizados:
```
templates/
├── shared/
└── modules/
    ├── cliente/
    └── ...
```

Y en los controladores usar:
```java
return "modules/cliente/templates/clientes";
```

### 3. **Compatibilidad con Referencias Actuales**

Todos los templates y archivos HTML tienen referencias a CSS/JS que deberán actualizarse:

```html
<!-- ANTES -->
<link th:href="@{/css/clientes.css}" rel="stylesheet">
<script th:src="@{/js/clientes.js}"></script>

<!-- DESPUÉS (Opción A - con resource handlers) -->
<link th:href="@{/modules/cliente/static/css/clientes.css}" rel="stylesheet">
<script th:src="@{/modules/cliente/static/js/clientes.js}"></script>

<!-- DESPUÉS (Opción B - estructura plana organizada) -->
<link th:href="@{/static/modules/cliente/css/clientes.css}" rel="stylesheet">
<script th:src="@{/static/modules/cliente/js/clientes.js}"></script>
```

---

