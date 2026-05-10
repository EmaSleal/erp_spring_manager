## 🏗️ Estructura General

```
whats_orders_manager/
├── docs/                              # Documentación del proyecto
│   ├── base de datos/                 # Scripts SQL y migraciones
│   ├── diseno/                        # Mockups y diseños
│   ├── planificacion/                 # Planes y decisiones técnicas
│   ├── referencias/                   # Referencias y roadmaps
│   └── sprints/                       # Documentación de sprints
│       ├── SPRINT_1/
│       ├── SPRINT_2/
│       ├── SPRINT_3/
│       │   └── FASE_1_WHATSAPP_CONVERSACIONES.md
│       └── fixes/
│
├── src/main/
│   ├── java/api/astro/whats_orders_manager/
│   │   ├── config/                    # Configuraciones de Spring
│   │   ├── controllers/               # Controladores REST y MVC
│   │   ├── dto/                       # ⚠️ DEPRECATED - usar models/dto
│   │   ├── models/                    # 📦 MODELOS ORGANIZADOS
│   │   │   ├── dto/                   # Data Transfer Objects
│   │   │   ├── enums/                 # Enumeraciones
│   │   │   ├── class/                 # Clases auxiliares
│   │   │   ├── records/               # Java Records
│   │   │   └── *.java                 # Entidades JPA
│   │   ├── repositories/              # Repositorios Spring Data JPA
│   │   ├── services/                  # Lógica de negocio
│   │   └── WhatsOrdersManagerApplication.java
│   │
│   └── resources/
│       ├── static/                    # Recursos estáticos
│       │   ├── css/                   # Hojas de estilo
│       │   ├── js/                    # Scripts JavaScript
│       │   └── img/                   # Imágenes
│       ├── templates/                 # Plantillas Thymeleaf
│       │   ├── components/            # Componentes reutilizables
│       │   ├── whatsapp/              # Vistas WhatsApp
│       │   └── *.html                 # Vistas generales
│       └── application.yml            # Configuración principal
│
├── target/                            # Archivos compilados (generado)
├── pom.xml                            # Dependencias Maven
├── start.ps1                          # Script de inicio
└── .env.local                         # Variables de entorno
```

---

