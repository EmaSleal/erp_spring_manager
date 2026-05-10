##  Estructura General

```
src/
├── main/
│   ├── java/
│   │   └── api/
│   │       └── astro/
│   │           └── whats_orders_manager/
│   │               ├── WhatsOrdersManagerApplication.java  # Clase principal
│   │               ├── config/                             # Configuraciones
│   │               ├── controller/                         # Controladores auxiliares
│   │               ├── controllers/                        # Controladores principales
│   │               ├── dto/                               #  Data Transfer Objects
│   │               ├── enums/                             # Enumeraciones
│   │               ├── models/                            # Modelos de dominio
│   │               ├── repositories/                      # Acceso a datos
│   │               ├── schedulers/                        # Tareas programadas
│   │               ├── services/                          # Lógica de negocio
│   │               └── util/                              #  Utilidades reutilizables
│   └── resources/
│       ├── application.yml                                # Configuración de Spring
│       ├── static/                                        # Recursos estáticos
│       │   ├── css/                                      # Hojas de estilo
│       │   ├── js/                                       # JavaScript
│       │   └── images/                                   # Imágenes
│       └── templates/                                     # Plantillas Thymeleaf
│           ├── auth/                                     # Autenticación
│           ├── clientes/                                 # Gestión de clientes
│           ├── configuracion/                            # Configuración
│           ├── dashboard/                                # Panel principal
│           ├── email/                                    # Plantillas de email
│           ├── error/                                    # Páginas de error
│           ├── facturas/                                 # Gestión de facturas
│           ├── perfil/                                   # Perfil de usuario
│           ├── productos/                                # Gestión de productos
│           ├── reportes/                                 # Reportes y gráficos
│           ├── usuarios/                                 # Gestión de usuarios
│           ├── components/                               # Componentes reutilizables
│           ├── index.html                                # Página de inicio
│           └── layout.html                               # Layout base
└── test/
    └── java/
        └── api/                                          # Tests unitarios
```

---

